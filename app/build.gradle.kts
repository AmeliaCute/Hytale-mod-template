import java.nio.file.Files
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.3.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

val hytaleHome: String by lazy {
    if (project.hasProperty("hytale_home")) {
        project.findProperty("hytale_home") as String
    } else {
        val os = org.gradle.internal.os.OperatingSystem.current()
        val userHome = System.getProperty("user.home")
        when {
            os.isWindows -> "$userHome/AppData/Roaming/Hytale"
            os.isMacOsX -> "$userHome/Library/Application Support/Hytale"
            os.isLinux -> {
                val flatpakPath = "$userHome/.var/app/com.hypixel.HytaleLauncher/data/Hytale"
                if (file(flatpakPath).exists()) flatpakPath
                else "$userHome/.local/share/Hytale"
            }
            else -> throw GradleException("Unsupported operating system. Please define hytale_home property.")
        }
    }
}

val gamePatchline: String by project

if (!file(hytaleHome).exists()) {
    throw GradleException("Failed to find Hytale at $hytaleHome. Please install the game or set the hytale_home property.")
}

val pluginName: String by project
val pluginVersion: String by project
val pluginGroup: String by project
val pluginDescription: String by project
val pluginMain: String by project
val hytaleServerJar = "$hytaleHome/install/$gamePatchline/package/game/latest/Server/HytaleServer.jar"
val hytaleAssetsZip = "$hytaleHome/install/$gamePatchline/package/game/latest/Assets.zip"


version = pluginVersion
group = pluginGroup

repositories {
    mavenCentral()
}

if (!file(hytaleServerJar).exists()) {
    throw GradleException("HytaleServer.jar not found at $hytaleServerJar")
}

if (!file(hytaleAssetsZip).exists()) {
    throw GradleException("Assets.zip not found at $hytaleAssetsZip")
}

dependencies {
    compileOnly(files(hytaleServerJar))
}

abstract class ProcessManifestTask : DefaultTask() {
    @get:InputFile
    abstract val generatedManifest: RegularFileProperty

    @get:OutputFile
    abstract val resourcesManifest: RegularFileProperty

    @get:Input
    abstract val variables: MapProperty<String, String>

    @TaskAction
    fun processManifest() {
        val inputFile = generatedManifest.asFile.get()
        val outputFile = resourcesManifest.asFile.get()

        if (!inputFile.exists()) {
            throw GradleException("Source manifest not found at ${inputFile.absolutePath}")
        }

        val manifestText = inputFile.readText()
        val processedText = processJsonVariables(manifestText, variables.get())

        outputFile.parentFile.mkdirs()
        outputFile.writeText(processedText)

        logger.lifecycle("Processed manifest.json: generated/ -> src/main/resources/")
    }

    private fun processJsonVariables(jsonText: String, vars: Map<String, String>): String {
        var result = jsonText
        vars.forEach { (key, value) ->
            result = result.replace("\$$key", value)
            result = result.replace("\${$key}", value)
        }
        return result
    }
}

abstract class InstallDevAssetsTask : DefaultTask() {
    @get:InputDirectory
    @get:Optional
    abstract val resourcesDir: DirectoryProperty

    @get:OutputDirectory
    abstract val assetsOutputDir: DirectoryProperty

    @get:Input
    abstract val assetPluginName: Property<String>

    @get:Input
    abstract val assetPluginVersion: Property<String>

    @get:Input
    abstract val assetPluginGroup: Property<String>

    @get:Input
    abstract val assetPluginDescription: Property<String>

    @TaskAction
    fun installAssets() {
        val assetsDir = assetsOutputDir.asFile.get()
        val resourcesSrc = resourcesDir.asFile.orNull

        assetsDir.mkdirs()

        if (resourcesSrc == null || !resourcesSrc.exists()) {
            logger.lifecycle("No resources folder found, skipping assets installation")
            return
        }

        val manifestData = """
        {
            "Group": "${assetPluginGroup.get()}",
            "Name": "assets",
            "Version": "${assetPluginVersion.get()}",
            "Description": "${assetPluginDescription.get()}",
            "Authors": [],
            "Website": "",
            "Dependencies": {},
            "OptionalDependencies": {},
            "LoadBefore": {},
            "DisabledByDefault": false,
            "IncludesAssetPack": false,
            "SubPlugins": []
        }
        """.trimIndent()

        val assetsManifestFile = assetsDir.resolve("manifest.json")
        assetsManifestFile.writeText(manifestData)
        logger.lifecycle("Created assets manifest.json")

        val commonSourceFolder = resourcesSrc.resolve("Common")
        if (commonSourceFolder.exists() && commonSourceFolder.isDirectory) {
            val commonTargetFolder = assetsDir.resolve("Common")
            commonSourceFolder.copyRecursively(commonTargetFolder, overwrite = true)
            logger.lifecycle("Copied folder: Common")
        }

        val serverSourceFolder = resourcesSrc.resolve("Server")
        if (serverSourceFolder.exists() && serverSourceFolder.isDirectory) {
            val serverTargetFolder = assetsDir.resolve("Server")
            try {
                Files.createSymbolicLink(serverTargetFolder.toPath(), serverSourceFolder.toPath())
                logger.lifecycle("Symlinked folder: Server")
            } catch (e: Exception) {
                logger.warn("Could not symlink Server folder \n${e.message}")
            }
        }

        logger.lifecycle("Assets installed to: ${assetsDir.absolutePath}")
    }
}

abstract class InstallDevModTask : DefaultTask() {
    @get:Input
    abstract val modPluginName: Property<String>

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun printSummary() {
        logger.lifecycle("===========================================")
        logger.lifecycle(" Dev Installation Complete!")
        logger.lifecycle(" Code JAR: run/mods/${modPluginName.get()}-dev-code-only.jar")
        logger.lifecycle("   - Contains: compiled classes")
        logger.lifecycle(" Assets: run/mods/${modPluginName.get()}.assets/")
        logger.lifecycle("   - Contains: all resources")
        logger.lifecycle("===========================================")
    }
}

tasks {
    val cleanMods by registering(Delete::class) {
        group = "hytale"
        description = "Cleans only the mods folder in run directory"

        val runDir = rootProject.file("run")
        val modsDir = runDir.resolve("mods")

        delete(modsDir)
    }

    clean {
        dependsOn(cleanMods)
        delete(rootProject.file("build"))
    }

    jar {
        enabled = false
    }

    val processManifest by registering(ProcessManifestTask::class) {
        group = "hytale"
        description = "Processes manifest.json from generated/ and copies to resources/"

        generatedManifest.set(file("src/main/generated/manifest.json"))
        resourcesManifest.set(file("src/main/resources/manifest.json"))
        variables.set(mapOf(
            "pluginName" to pluginName,
            "pluginVersion" to pluginVersion,
            "pluginGroup" to pluginGroup,
            "pluginDescription" to pluginDescription,
            "pluginMain" to pluginMain
        ))
    }

    processResources {
        dependsOn(processManifest)
    }

    shadowJar {
        archiveBaseName.set(pluginName)
        archiveVersion.set(pluginVersion)
        archiveClassifier.set("")
        mergeServiceFiles()

        from(sourceSets.main.get().output.resourcesDir)
    }

    val shadowJarTask = named<ShadowJar>("shadowJar")
    val buildRelease by registering {
        dependsOn(shadowJarTask)
        group = "hytale"
        description = "Builds the final .jar file for distribution"

        val releaseFileProvider = shadowJarTask.flatMap { it.archiveFile }

        doLast {
            logger.lifecycle("===========================================")
            logger.lifecycle(" Build Success!")
            logger.lifecycle(" Release File: ${releaseFileProvider.get().asFile.absolutePath}")
            logger.lifecycle(" (Merged: Java classes + Resources)")
            logger.lifecycle("===========================================")
        }
    }

    build {
        dependsOn(buildRelease)
    }

    val setupRunFolder by registering(Copy::class) {
        group = "hytale"
        description = "Copies HytaleServer.jar and Assets.zip to run directory"

        val runDir = rootProject.file("run")

        from(hytaleServerJar)
        from(hytaleAssetsZip)
        into(runDir)

        doFirst {
            runDir.mkdirs()
        }
    }

    val installDevCode by registering(Jar::class) {
        dependsOn("classes")
        group = "hytale"
        description = "Creates a JAR with only Java classes (no assets)"

        archiveBaseName.set("${pluginName}-dev")
        archiveClassifier.set("code-only")

        from(sourceSets.main.get().output.classesDirs)

        sourceSets.main.get().output.resourcesDir?.let {
            from(it) {
                include("manifest.json")
            }
        }

        destinationDirectory.set(rootProject.file("run/mods"))
    }

    val installDevAssets by registering(InstallDevAssetsTask::class) {
        dependsOn(setupRunFolder, "processResources")
        group = "hytale"
        description = "Installs assets as a symlinked folder"

        resourcesDir.set(file("src/main/resources"))
        assetsOutputDir.set(rootProject.file("run/mods/${pluginName}.assets"))

        assetPluginName.set(pluginName)
        assetPluginVersion.set(pluginVersion)
        assetPluginGroup.set(pluginGroup)
        assetPluginDescription.set(pluginDescription)
    }

    val installDevMod by registering(InstallDevModTask::class) {
        dependsOn(installDevCode, installDevAssets)
        group = "hytale"
        description = "Installs dev mod (code JAR + symlinked assets folder)"

        modPluginName.set(pluginName)
    }

    val runServer by registering(JavaExec::class) {
        dependsOn(installDevMod)
        group = "hytale"
        description = "Runs the Hytale server"

        val runDir = rootProject.file("run")
        workingDir = runDir

        classpath = files(runDir.resolve("HytaleServer.jar"))
        args = listOf(
            "--assets", "Assets.zip",
            //"--validate-assets", NEVER FUCKING ACTIVATE THIS SHIT
            "--event-debug",
        )
        standardInput = System.`in`
        systemProperty("org.gradle.console", "plain")
    }
}
