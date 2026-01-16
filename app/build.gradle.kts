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

val pluginName: String by project
val pluginVersion: String by project
val pluginGroup: String by project
val pluginDescription: String by project
val pluginMain: String by project

version = pluginVersion
group = pluginGroup

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("../libs/HytaleServer.jar"))
    implementation("com.google.guava:guava:33.4.6-jre")
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
        val libsDir = rootProject.layout.projectDirectory.dir("libs")
        val runDir = rootProject.layout.projectDirectory.dir("run")

        from(libsDir) { include("HytaleServer.jar", "Assets.zip") }
        into(runDir)

        onlyIf { !runDir.file("HytaleServer.jar").asFile.exists() }
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

    val installDevAssets by registering {
        dependsOn(setupRunFolder, "processResources")
        group = "hytale"
        description = "Installs assets as a symlinked folder"

        val runDirPath = rootProject.file("run")
        val resourcesSrcPath = file("src/main/resources")

        doLast {
            val runDir = runDirPath
            val modsDir = runDir.resolve("mods")
            val assetsDir = modsDir.resolve("${pluginName}.assets")

            val resourcesSrc = resourcesSrcPath

            assetsDir.mkdirs()
            if (resourcesSrc.exists())
            {
                val manifestData = """
                {
                    "Group": "$pluginGroup",
                    "Name": "assets",
                    "Version": "$pluginVersion",
                    "Description": "$pluginDescription",
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
            } else
            {
                logger.lifecycle("No resources folder found, skipping assets installation")
            }
        }
    }

    val installDevMod by registering {
        dependsOn(installDevCode, installDevAssets)
        group = "hytale"
        description = "Installs dev mod (code JAR + symlinked assets folder)"

        doLast {
            logger.lifecycle("===========================================")
            logger.lifecycle(" Dev Installation Complete!")
            logger.lifecycle(" Code JAR: run/mods/${pluginName}-dev-code-only.jar")
            logger.lifecycle("   - Contains: compiled classes")
            logger.lifecycle(" Assets: run/mods/${pluginName}.assets/")
            logger.lifecycle("   - Contains: all resources")
            logger.lifecycle("===========================================")
        }
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