plugins {
    `java-library`
    id("com.diffplug.spotless")
    id("de.skuzzle.restrictimports")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.pgm.fyi/snapshots/") // PGM-specific depdencies
    maven("https://repo.aikar.co/content/groups/aikar/")
}

dependencies {
    api("tc.oc.occ:Dispense:1.0.0-SNAPSHOT")
    api("com.squareup.retrofit2:retrofit:2.9.0")
    api("com.squareup.retrofit2:converter-jackson:2.9.0")
    api("com.squareup.retrofit2:converter-gson:2.9.0")
    api("co.aikar:taskchain-bukkit:3.7.2")
    api("org.java-websocket:Java-WebSocket:1.5.1")

    compileOnly("dev.pgm.paper:paper-api:1.8_1.21.11-SNAPSHOT")
    compileOnly("tc.oc.pgm:core:0.16-SNAPSHOT")
    compileOnly("dev.pgm:events:1.0.0-SNAPSHOT")
    compileOnly("org.incendo:cloud-annotations:2.0.0")
    compileOnly("org.jetbrains:annotations:26.0.2")
    compileOnly("com.google.guava:guava:17.0")
}

group = "rip.bolt"
version = "1.0.0-SNAPSHOT"

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    withType<Javadoc> {
        options.encoding = "UTF-8"
    }
}

spotless {
    ratchetFrom = "origin/master"
    java {
        removeUnusedImports()
        palantirJavaFormat("2.83.0").style("GOOGLE").formatJavadoc(true)
    }
}


restrictImports {
    group {
        reason = "Use org.jetbrains.annotations to add annotations"
        bannedImports = listOf("javax.annotation.**")
    }
    group {
        reason = "Use tc.oc.pgm.util.Assert to add assertions"
        bannedImports = listOf("com.google.common.base.Preconditions.**", "java.util.Objects.requireNonNull")
    }
}