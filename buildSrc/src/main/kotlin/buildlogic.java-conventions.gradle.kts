import de.skuzzle.restrictimports.gradle.RestrictImports

plugins {
    `java-library`
    id("com.diffplug.spotless")
    id("de.skuzzle.restrictimports")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
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
    api("org.jspecify:jspecify:1.0.1")

    compileOnly("net.kyori:adventure-api:5.2.0")
    compileOnly("app.ashcon:sportpaper:1.8.8-R0.1-SNAPSHOT")
    compileOnly("tc.oc.pgm:core:0.16-SNAPSHOT")
    compileOnly("dev.pgm:events:1.0.0-SNAPSHOT")
    compileOnly("org.incendo:cloud-annotations:2.0.0")
    compileOnly("org.jetbrains:annotations:26.1.0")
    compileOnly("com.google.guava:guava:17.0")
    compileOnly("dev.pgm.community:core:0.2-SNAPSHOT")
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
        trimTrailingWhitespace()
        formatAnnotations()
        palantirJavaFormat("2.102.0").style("GOOGLE").formatJavadoc(true)
    }
}

tasks {
    // Bypass inability to have two groups with same base packages inside the import restriction plugin
    val restrictJavaxAnnotations = register<RestrictImports>("restrictJavaxAnnotations") {
        group = "verification"
        reason = "Use org.jspecify.annotations to add annotations, or org.jetbrains.annotations if needed"
        bannedImports = listOf("javax.annotation.**")
    }
    val restrictAsserts = register<RestrictImports>("restrictAsserts") {
        group = "verification"
        reason = "Use tc.oc.pgm.util.Assert to add assertions"
        bannedImports = listOf("com.google.common.base.Preconditions.**", "java.util.Objects.requireNonNull")
    }
    // Enforce the import restrictions
    check {
        dependsOn(restrictJavaxAnnotations, restrictAsserts)
    }
}