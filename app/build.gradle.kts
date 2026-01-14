plugins {
    id("java-library")
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

    implementation(libs.guava)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set(rootProject.name)
    archiveVersion.set(project.version.toString())
}

tasks.named<Delete>("clean") {
    delete(rootProject.projectDir.resolve("run"))
}
