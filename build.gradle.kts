import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("buildlogic.java-conventions")
    `maven-publish`
    id("com.gradleup.shadow")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName = "Ingame.jar"
    archiveClassifier.set("")
    destinationDirectory = rootProject.projectDir.resolve("build/libs")

    minimize()

    dependencies {
        exclude(dependency("org.jetbrains:annotations"))
    }

    relocate("co.aikar.taskchain", "rip.bolt.ingame.taskchain")

    exclude("META-INF/**")
}

publishing {
    publications.create<MavenPublication>("ingame") {
        groupId = project.group as String
        artifactId = project.name
        version = project.version as String

        artifact(tasks["shadowJar"])
    }
    repositories {
        maven {
            name = "ghPackages"
            url = uri("https://maven.pkg.github.com/bolt-rip/ingame")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks {
    processResources {
        filesMatching(listOf("plugin.yml")) {
            expand(
                "name" to project.name,
                "description" to project.description,
                "mainClass" to "rip.bolt.ingame.Ingame",
                "version" to project.version,
                "commitHash" to project.latestCommitHash(),
                "url" to "https://github.com/bolt-rip/"
            )
        }
    }

    named("jar") {
        enabled = false
    }

    named("build") {
        dependsOn(shadowJar)
    }
}