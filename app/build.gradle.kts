import java.nio.file.Files
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

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

tasks.jar{
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from("src/main/ressources")
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

    val processManifest by registering {
        group = "hytale"
        description = "Processes manifest.json from generated/ and copies to resources/"

        val generatedManifestFile = file("src/main/generated/manifest.json")
        val resourcesManifestFile = file("src/main/resources/manifest.json")

        inputs.file(generatedManifestFile)
        outputs.file(resourcesManifestFile)

        doLast {
            if (!generatedManifestFile.exists()) {
                throw GradleException("Source manifest not found at ${generatedManifestFile.absolutePath}")
            }

            val slurper = JsonSlurper()
            val manifestJson = slurper.parse(generatedManifestFile) as Map<String, Any>

            val processedManifest = processJsonVariables(manifestJson, mapOf(
                "pluginName" to pluginName,
                "pluginVersion" to pluginVersion,
                "pluginGroup" to pluginGroup,
                "pluginDescription" to pluginDescription,
                "pluginMain" to pluginMain
            ))

            resourcesManifestFile.parentFile.mkdirs()

            val jsonBuilder = JsonBuilder(processedManifest)
            resourcesManifestFile.writeText(jsonBuilder.toPrettyString())

            logger.lifecycle("Processed manifest.json: generated/ -> src/main/resources/")
        }
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
                val manifestData = mapOf(
                    "Group" to pluginGroup,
                    "Name" to "assets",
                    "Version" to pluginVersion,
                    "Description" to pluginDescription,
                    "Authors" to emptyList<String>(),
                    "Website" to "",
                    "Dependencies" to emptyMap<String, String>(),
                    "OptionalDependencies" to emptyMap<String, String>(),
                    "LoadBefore" to emptyMap<String, String>(),
                    "DisabledByDefault" to false,
                    "IncludesAssetPack" to false,
                    "SubPlugins" to emptyList<String>()
                )

                val assetsManifest = manifestData.toMutableMap()
                val assetsManifestFile = assetsDir.resolve("manifest.json")
                val jsonBuilder = JsonBuilder(assetsManifest)
                assetsManifestFile.writeText(jsonBuilder.toPrettyString())
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

fun processJsonVariables(obj: Any?, variables: Map<String, String>): Any? {
    return when (obj) {
        is Map<*, *> -> {
            obj.entries.associate { (key, value) ->
                key to processJsonVariables(value, variables)
            }
        }
        is List<*> -> {
            obj.map { processJsonVariables(it, variables) }
        }
        is String -> {
            var result: String = obj
            variables.forEach { (varKey, varValue) ->
                result = result.replace("\$$varKey", varValue)
                result = result.replace("\${$varKey}", varValue)
            }
            result
        }
        else -> obj
    }
}