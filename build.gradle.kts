import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT"
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
}

fun resolveProperty(name: String, fallback: String): String {
    val prop = project.findProperty(name) ?: return fallback
    return if (prop is Provider<*>) {
        prop.orNull?.toString() ?: fallback
    } else {
        prop.toString()
    }
}

version = resolveProperty("mod_version", "0.0.0")
group = resolveProperty("maven_group", "lgbt.faith")

loom {
    mods {
        register("chiyoko") {
            sourceSet(sourceSets.main.get())
        }
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

dependencies {
    implementation("com.google.guava:guava:33.0.0-jre")

    minecraft("com.mojang:minecraft:${resolveProperty("minecraft_version", "26.2")}")

    implementation("net.fabricmc:fabric-loader:${resolveProperty("loader_version", "0.19.3")}")

    implementation("net.fabricmc.fabric-api:fabric-api:${resolveProperty("fabric_api_version", "0.154.2+26.2")}")
    implementation("net.fabricmc:fabric-language-kotlin:${resolveProperty("fabric_kotlin_version", "1.13.12+kotlin.2.4.0")}")
}

tasks.processResources {
    inputs.property("version", version)

    val minecraftVersion = resolveProperty("minecraft_version", "")
    val loaderVersion = resolveProperty("loader_version", "")
    val kotlinLoaderVersion = resolveProperty("fabric_kotlin_version", "")

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to version,
                "minecraft_version" to minecraftVersion,
                "loader_version" to loaderVersion,
                "kotlin_loader_version" to kotlinLoaderVersion
            )
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
    }
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
    inputs.property("projectName", project.name)

    from("LICENSE") {
        rename { "${it}_${project.name}" }
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}