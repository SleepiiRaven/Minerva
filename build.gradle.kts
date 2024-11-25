plugins {
    id ("java")
    id ("io.freefair.lombok") version "8.6"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id ("io.papermc.paperweight.userdev") version "1.7.2"
}

group = "net.minervamc"
version = "0.1.1"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "CodeMC"
        url = uri("https://repo.codemc.io/repository/maven-public/")
    }
    maven {
        name = "phoenix"
        url = uri("https://nexus.phoenixdevt.fr/repository/maven-public/")
    }
    maven {
        name = "onarandombox"
        url = uri("https://repo.onarandombox.com/content/groups/public/")
    }

    maven {
        name = "citizens-repo"
        url = uri("https://maven.citizensnpcs.co/repo")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("de.tr7zw:item-nbt-api-plugin:2.13.2")
    compileOnly("io.lumine:MythicLib-dist:1.7.1-SNAPSHOT")
    compileOnly("net.Indyuce:MMOItems-API:6.10-SNAPSHOT")
    compileOnly("com.onarandombox.multiversecore:Multiverse-Core:4.3.1")
    compileOnly("net.citizensnpcs:citizens-main:2.0.36-SNAPSHOT")
    implementation ("fr.mrmicky:fastboard:2.1.3")
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
}

val targetJavaVersion =21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks.withType<JavaCompile>().configureEach {
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

tasks.processResources {
    val props = mutableMapOf("version" to version)
    inputs.properties(props)
    props["filteringCharset"]  = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveClassifier.set("shadow")
    //relocate ("fr.mrmicky.fastboard", "net.minervamc.minerva.fastboard")
    destinationDirectory.set(file("D:/Servers/Minerva/plugins"))
    //destinationDirectory.set(file("run/plugins")) //Faceless
}
