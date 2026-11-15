pluginManagement {
    repositories {
        // Maven Central first: the plugin portal redirects artifacts to a CDN that some networks
        // block outright, and every plugin this build needs is published to Central as well.
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

rootProject.name = "max-engine"

// The engine's two outside libraries are built from source, pinned by the commits recorded in
// vendor/PINS.md, so neither JitPack nor the GitHub Packages registry is consulted.
//
// When the engine is built from inside MAX Patcher it sits at <repo>/engine and the submodules are
// at <repo>/vendor; standalone it is expected to sit beside them. Both layouts are accepted.
mapOf(
    "ARSCLib" to "com.github.MorpheApp:ARSCLib",
).forEach { (libraryPath, libraryName) ->
    listOf(file("../vendor/$libraryPath"), file("../$libraryPath"))
        .firstOrNull { it.exists() }
        ?.let { libDir ->
            includeBuild(libDir) {
                dependencySubstitution {
                    substitute(module(libraryName)).using(project(":"))
                }
            }
        }
}

listOf(file("../vendor/smali"), file("../smali")).firstOrNull { it.exists() }?.let { smaliDir ->
    includeBuild(smaliDir) {
        dependencySubstitution {
            substitute(module("com.github.MorpheApp.smali:smali")).using(project(":smali"))
            substitute(module("com.github.MorpheApp.smali:smali-dexlib2")).using(project(":dexlib2"))
            substitute(module("com.github.MorpheApp.smali:smali-util")).using(project(":util"))
        }
    }
}
