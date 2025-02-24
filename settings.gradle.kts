plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "calendario-poliba"

sourceControl {
    gitRepository(java.net.URI("https://github.com/codeborne/klite.git")) {
        producesModule("com.github.codeborne.klite:server")
        producesModule("com.github.codeborne.klite:jackson")
        producesModule("com.github.codeborne.klite:openapi")
    }
}