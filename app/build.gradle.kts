import java.nio.file.Files
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
    clean {
        delete(rootProject.file("run"))
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

    val installDevMod by registering {
        dependsOn(setupRunFolder, "classes")
        group = "hytale"
        description = "Installs the mod as a folder with Symlinked resources"

        doLast {
            val runDir = rootProject.file("run")
            val modsDir = runDir.resolve("mods")
            val modDir = modsDir.resolve(rootProject.name)

            val resourcesSrc = file("src/main/resources")
            val classesSrc = sourceSets.main.get().output.classesDirs.singleFile

            if (modDir.exists()) modDir.deleteRecursively()
            modDir.mkdirs()

            copy {
                from(classesSrc)
                into(modDir)
            }

            if (resourcesSrc.exists()) {
                resourcesSrc.listFiles()?.forEach { file ->
                    val target = modDir.resolve(file.name).toPath()
                    try {
                        Files.createSymbolicLink(target, file.toPath())
                        logger.lifecycle("Symlinked: ${file.name}")
                    } catch (e: Exception) {
                        logger.warn("Could not symlink ${file.name}, copying instead.")
                        copy { from(file); into(modDir) }
                    }
                }
            }
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
            "--auth-mode", "offline",
            //"--validate-assets", NEVER FUCKING ACTIVATE THIS SHIT
            "--event-debug",
            "--allow-op"
        )
        standardInput = System.`in`
        systemProperty("org.gradle.console", "plain")
    }
}