plugins {
    id ("java")
    id ("io.freefair.lombok") version "8.6"
    id ("io.papermc.paperweight.userdev") version "1.7.2"
}

group = "net.minervamc"
version = "1.0"

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
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
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
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
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

tasks.jar {
    destinationDirectory.set(file("D:/Servers/Minerva/plugins"))
    //destinationDirectory.set(file("run/plugins")) //Faceless
}