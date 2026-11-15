# MAX Engine, and how the app uses it

The patching engine is not a dependency downloaded from a registry. It is `engine/`, a fork of
Morphe Patcher that lives in this repository, versioned on its own and built from source.

## How it is wired

`settings.gradle.kts` at the repository root declares it as an included build and substitutes the
module coordinate upstream used:

```kotlin
includeBuild("engine") {
    dependencySubstitution {
        substitute(module("app.morphe:morphe-patcher")).using(project(":"))
    }
}
```

`app/build.gradle.kts` and `patcher-core/build.gradle.kts` still declare
`implementation(libs.morphe.patcher)`. Gradle resolves that coordinate to the local project, so the
source code is unchanged and the download disappears.

## Two ways the app runs the engine

1. **Embedded JVM.** `patcher-core` is packed into a `hostJar` and shipped beside an OpenJDK build
   for Android. The app starts that JVM in a separate process and talks to it over AIDL
   (`IPatcherService`, `JvmLaunch`, `HostStarted`). This is the default and gives the engine a
   full desktop JVM.
2. **In-app runtime.** A 32-bit x86 device patches inside the app process instead, using Android's
   own runtime.

Both paths compile against the same engine source, which is why the engine must not use Android
APIs — and why it can be tested without an emulator.

## Changing it

See `engine/docs/MAX_ENGINE.md`. Every change needs a root cause, the diff, a regression test, an
ABI assessment (`apiCheck`) and a documentation update.

## Version skew

MAX Engine carries **2.3.1**, continuing upstream's line rather than restarting at 0.1.0, because
patch bundles declare a minimum patcher version and the manager enforces it. The ABI is identical to
upstream 2.3.0, so inherited bundles run unchanged. Reasoning and the test that pins it:
`engine/docs/MAX_ENGINE.md` and `engine/src/test/kotlin/.../EngineVersionCompatibilityTest.kt`.

The app reports the engine's version by reading `engine/gradle.properties` at build time, so the two
cannot drift apart silently.
