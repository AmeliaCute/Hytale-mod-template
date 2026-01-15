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

version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("../libs/HytaleServer.jar"))
    implementation("com.google.guava:guava:33.4.6-jre")
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

    shadowJar {
        archiveBaseName.set(project.property("pluginName") as String)
        archiveVersion.set(project.property("pluginVersion") as String)
        archiveClassifier.set("")
        mergeServiceFiles()

        from(sourceSets.main.get().output.resourcesDir)
    }

    val shadowJarTask = named<ShadowJar>("shadowJar")
    val buildRelease by registering {
        dependsOn(shadowJarTask)
        group = "hytale"
        description = "Builds the final .jar file for distribution (combined assets + code)"

        val releaseFileProvider = shadowJarTask.flatMap { it.archiveFile }

        doLast {
            logger.lifecycle("===========================================")
            logger.lifecycle(" Build Success!")
            logger.lifecycle(" Release File: ${releaseFileProvider.get().asFile.absolutePath}")
            logger.lifecycle(" (Combined: Java classes + Resources)")
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
        description = "Creates a JAR with only Java classes (no resources)"

        archiveBaseName.set("${project.property("pluginName") as String}-dev")
        archiveClassifier.set("code-only")

        from(sourceSets.main.get().output.classesDirs)

        sourceSets.main.get().output.resourcesDir?.let {
            from(it) {
                include("manifest.json")
                include("Common")
            }
        }

        destinationDirectory.set(rootProject.file("run/mods"))
    }

    val installDevAssets by registering {
        dependsOn(setupRunFolder, "processResources")
        group = "hytale"
        description = "Installs assets as a symlinked folder"

        val pluginName = project.property("pluginName") as String
        val runDirPath = rootProject.file("run")
        val resourcesSrcPath = file("src/main/resources")

        doLast {
            val runDir = runDirPath
            val modsDir = runDir.resolve("mods")
            val assetsDir = modsDir.resolve("${pluginName}-assets")

            val resourcesSrc = resourcesSrcPath

            if (assetsDir.exists()) assetsDir.deleteRecursively()
            assetsDir.mkdirs()

            if (resourcesSrc.exists())
            {
                val manifestData = mapOf(
                    "Group" to pluginName,
                    "Name" to "$pluginName-asset",
                    "Version" to "1.0.0",
                    "Description" to "",
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
                assetsManifest["IncludesAssetPack"] = true

                val assetsManifestFile = assetsDir.resolve("manifest.json")
                val jsonBuilder = JsonBuilder(assetsManifest)
                assetsManifestFile.writeText(jsonBuilder.toPrettyString())
                logger.lifecycle("Created assets manifest.json with IncludesAssetPack = true")

                listOf("Server").forEach { folderName ->
                    val sourceFolder = resourcesSrc.resolve(folderName)
                    if (sourceFolder.exists() && sourceFolder.isDirectory) {
                        val targetFolder = assetsDir.resolve(folderName)
                        try {
                            Files.createSymbolicLink(targetFolder.toPath(), sourceFolder.toPath())
                            logger.lifecycle("Symlinked folder: $folderName")
                        } catch (e: Exception) {
                            logger.warn("Could not symlink $folderName folder, copying instead.\n${e.message}")
                            sourceFolder.copyRecursively(targetFolder, overwrite = true)
                        }
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

        val pluginName = project.property("pluginName") as String

        doLast {
            logger.lifecycle("===========================================")
            logger.lifecycle(" Dev Installation Complete!")
            logger.lifecycle(" Code JAR: run/mods/${pluginName}-dev-code-only.jar")
            logger.lifecycle("   - Contains: manifest.json + compiled classes")
            logger.lifecycle(" Assets: run/mods/${pluginName}-assets/ (symlinked)")
            logger.lifecycle("   - Contains: manifest.json (IncludesAssetPack=true) + all resources")
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