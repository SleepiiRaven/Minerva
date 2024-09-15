plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.minervamc"
version = "0.1.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveClassifier.set("")
    val additionalPaths = listOf("run/plugins") // my server dir in my ide
    val default = "${layout.buildDirectory.get().asFile}/libs"

    var pluginDir = file("null")
    try {
        pluginDir = file("D:/Servers/Minerva/plugins") // Minerva server dir
    }catch (_: InvalidUserDataException) {}

    if (pluginDir.exists()) destinationDirectory.set(pluginDir)
    else destinationDirectory.set(file(default))

    doLast {
        additionalPaths.forEach { additionalPath ->
            if (file(additionalPath).exists()) {
                copy {
                    from(archiveFile)
                    into(additionalPath)
                }
            }
        }
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
