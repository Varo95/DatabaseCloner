import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.1.10"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.jetbrains.compose") version "1.8.0-alpha04"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10"
}

javafx {
    version = "23.0.2"
    modules = mutableListOf("javafx.controls", "javafx.fxml", "javafx.base", "javafx.graphics")
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)
    implementation(libs.com.sun.xml.bind.jaxb.core)
    implementation(libs.com.sun.xml.bind.jaxb.impl)
    implementation(libs.org.apache.logging.log4j.log4j.api)
    implementation(libs.org.apache.logging.log4j.log4j.core)
    implementation(libs.org.apache.logging.log4j.log4j.slf4j.impl)
    implementation(libs.org.mariadb.database.jdbc.mariadb)
    implementation(libs.org.postgresql.database.jdbc)
    implementation(libs.com.oracle.database.jdbc.ojdbc11)
    implementation(libs.com.microsoft.sqlserver.jdbc)
    val fxVersion = "23.0.2"
    val fxModules: List<String> = listOf("javafx-base", "javafx-controls", "javafx-fxml", "javafx-graphics")

    fxModules.forEach { module: String ->
        implementation("org.openjfx:$module:$fxVersion")
    }
    implementation(compose.desktop.currentOs)
    testImplementation(libs.org.jetbrains.kotlin.kotlin.test)
    testImplementation(libs.io.mockk.mockk)
}

group = "com.alvaro.devutils"
version = "1.0.0"
description = "DatabaseCloner"
java.sourceCompatibility = JavaVersion.VERSION_21

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

compose.desktop {

    application {
        buildTypes.release.proguard {
            version.set("7.5.0")
        }
        mainClass = "com.alvaro.devutils.MainKt"
        nativeDistributions {
            includeAllModules = true
            targetFormats(TargetFormat.Exe,TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            macOS {
                iconFile.set(project.file("/src/main/resources/com/alvaro/devutils/icon.icns"))
                vendor = "Alvaro's Software"
                appStore = false
            }
            windows {
                iconFile.set(project.file("/src/main/resources/com/alvaro/devutils/icon.ico"))
                vendor = "Alvaro's Software"
                perUserInstall = true
            }
            linux {
                iconFile.set(project.file("/src/main/resources/com/alvaro/devutils/icon.png"))
                vendor = "Alvaro's Software"
                debMaintainer = "Alvaro"
            }
            packageName = description
            packageVersion = version.toString()
        }
    }

}