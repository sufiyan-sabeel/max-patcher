@file:Suppress("UnstableApiUsage")

import java.util.Properties

val localProps = Properties().apply {
    val file = rootDir.resolve("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun githubUser(): String? =
    localProps.getProperty("gpr.user")
        ?: providers.gradleProperty("gpr.user").orNull
        ?: System.getenv("GITHUB_ACTOR")

fun githubToken(): String? =
    localProps.getProperty("gpr.key")
        ?: providers.gradleProperty("gpr.key").orNull
        ?: System.getenv("GITHUB_TOKEN")

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven("https://jitpack.io") {
            metadataSources {
                mavenPom()
                artifact()
            }
        }
        maven {
            // A repository must be specified for some reason. "registry" is a dummy.
            url = uri("https://maven.pkg.github.com/MorpheApp/registry")
            credentials {
                val gprUser: String? = githubUser()
                val gprKey: String? = githubToken()

                username = gprUser.orEmpty().ifBlank { "anonymous" }
                password = gprKey.orEmpty()
            }
        }
    }
}

rootProject.name = "max-patcher"
include(":app")
include(":patcher-core")

// MAX Engine is this repository's own fork of the patcher, at engine/, and is built from source as
// an included build. Substituting the module coordinate means the app never downloads the patcher
// from the Morphe GitHub Packages registry, so a build of this repository needs no package token.
//
// The engine's own libraries (the ARSCLib fork and the smali fork) are included by the engine's
// settings file, which finds them under vendor/ as pinned git submodules. See vendor/PINS.md.
includeBuild("engine") {
    dependencySubstitution {
        substitute(module("app.morphe:morphe-patcher")).using(project(":"))
    }
}
