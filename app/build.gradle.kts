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
val serverPath = rootProject.file("libs/HytaleServer.jar")
val assetsPath = rootProject.file("libs/Assets.zip")
val runFolder = rootProject.file("run")
val modsFolder = runFolder.resolve("mods")

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("../libs/HytaleServer.jar"))

    implementation("com.google.guava:guava:33.4.6-jre")
}
tasks {
    clean {
        delete(rootProject.projectDir.resolve("run"))
    }

    jar {
        enabled = false
    }

    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }

    val checkRequiredFiles by registering(DefaultTask::class) {
        doLast {
            val missingFiles = mutableListOf<String>()

            if (!serverPath.exists()) {
                missingFiles.add("HytaleServer.jar")
            }
            if (!assetsPath.exists()) {
                missingFiles.add("Assets.zip")
            }

            if (missingFiles.isNotEmpty()) {
                throw GradleException(
                    "ERROR: The following required files are missing in ./libs/:\n" +
                            missingFiles.joinToString("\n") { "  - $it" } +
                            "\n\nPlease read ./libs/README.md for instructions on how to obtain these files."
                )
            }
        }
    }

    val setupRunFolder by registering(Copy::class) {
        dependsOn(checkRequiredFiles)

        from(serverPath) {
            into(".")
        }
        from(assetsPath) {
            into(".")
        }

        into(runFolder)

        onlyIf {
            !runFolder.resolve("HytaleServer.jar").exists() || !runFolder.resolve("Assets.zip").exists()
        }
    }

    val copyMods by registering(Copy::class) {
        dependsOn(shadowJar, setupRunFolder)
        delete(modsFolder)

        from(shadowJar.get().archiveFile)
        into(modsFolder)
    }

    val runServer by registering(JavaExec::class) {
        dependsOn(copyMods)

        group = "hytale"
        description = "Runs the Hytale server with the plugin"
        workingDir = runFolder
        classpath = files(runFolder.resolve("HytaleServer.jar"))
        args = listOf("--assets", "Assets.zip")
        standardInput = System.`in`
    }
}