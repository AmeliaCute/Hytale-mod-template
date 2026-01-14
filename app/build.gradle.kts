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
}