plugins {
    kotlin("jvm") version "2.1.0"

    id("co.uzzu.dotenv.gradle") version "4.0.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.google.cloud.tools.jib") version "3.3.1"
    id("idea")
}

group = "it.rattly"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {
    implementation(kotlin("reflect"))

    listOf("polyglot", "js").forEach {
        implementation("org.graalvm.polyglot:$it:24.1.2")
    }

//    listOf("javet", "javet-node-linux-x86_64").forEach {
//        implementation("com.caoccao.javet:$it:4.1.1")
//    }


    implementation("com.github.kittinunf.fuel:fuel:3.0.0-alpha04")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("com.sksamuel.aedile:aedile-core:2.0.3")

    listOf("server", "jackson", "openapi").forEach {
        implementation("com.github.codeborne.klite:klite-$it:+")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.jib.get().dependsOn("runViteBuild")
tasks.register<Exec>("runViteBuild") {
    workingDir = layout.projectDirectory.dir("src/main/javascript").asFile
    commandLine = listOf("bun", "run", "--bun", "build")
}

jib {
    from {
        image = "ghcr.io/graalvm/jdk-community:23"

        platforms {
            platform { os = "linux"; architecture = "arm64" }
        }

        extraDirectories {
            paths {
                path {
                    setFrom("src/main/javascript/dist")
                    into = "/web"
                }

                path {
                    setFrom("src/main/resources/packages/node_modules/")
                    into = "/web/node_modules"
                }

                path {
                    setFrom("src/main/resources/static")
                    into = "/web/client/assets"
                }
            }
        }
    }

    to {
        image = "ghcr.io/${env.REGISTRY_USERNAME.value.lowercase()}/${project.name.lowercase()}:latest"
        auth {
            username = env.REGISTRY_USERNAME.value
            password = env.REGISTRY_PASSWORD.value
        }
    }

    container {
        mainClass = "it.rattly.MainKt"
        jvmFlags = listOf("-Xss2m", "-XX:MaxRAMPercentage=80", "-XX:+ExitOnOutOfMemoryError")
        labels = mapOf("org.opencontainers.image.source" to "https://github.com/${env.REGISTRY_USERNAME}/${project.name}")
    }
}

kotlin {
    jvmToolchain(17)
}