import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("buildlogic.java-conventions")
    `maven-publish`
    id("com.gradleup.shadow")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName = "Ingame.jar"
    archiveClassifier.set("")

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
        val name = project.name
        val description = project.description
        val version = project.version.toString()
        val commitHash = project.latestCommitHash()

        inputs.property("commitHash", commitHash)

        filesMatching(listOf("plugin.yml")) {
            expand(
                mapOf(
                    "name" to name,
                    "description" to description,
                    "apiVersion" to "26.2",
                    "mainClass" to "rip.bolt.ingame.Ingame",
                    "version" to version,
                    "commitHash" to commitHash.get(),
                    "url" to "https://github.com/bolt-rip/"
                )
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