# Building MAX Patcher

## What you need

| Requirement | Version | Why |
|---|---|---|
| JDK | 17, 21 and 25 | 17 and 25 build the JREs the app embeds, 21 runs Gradle |
| Android SDK | platform 37, build-tools current | `compileSdk = 37` |
| NDK + CMake | any version AGP 9.4.1 accepts | the app has native code |
| Node.js | LTS | only for the release pipeline |

**No GitHub token is needed.** The patcher is built from `engine/` and its two libraries from
`vendor/`, so nothing is downloaded from a package registry. This is the main difference from the
upstream build, which resolves the patcher from `maven.pkg.github.com`.

## Steps

```bash
git clone --recurse-submodules https://github.com/sufiyan-sabeel/max-patcher
cd max-patcher
git submodule update --init --recursive   # vendor/ARSCLib, vendor/smali
```

Then:

```bash
./gradlew :app:assembleDebug     # unsigned debug build
./gradlew :app:assembleRelease   # needs app/keystore.jks, see below
./gradlew test                   # app unit tests
./engine/gradlew -p engine test apiCheck   # engine tests and ABI check
```

`JAVA_HOME_17_X64` and `JAVA_HOME_25_X64` are the variable names CI uses for the toolchains Gradle
does not look for on its own.

## Layout worth knowing

| Path | What it is |
|---|---|
| `app/` | the Android application |
| `patcher-core/` | the JVM-side patching host the app runs in an embedded JVM |
| `engine/` | MAX Engine, an included Gradle build, forked from Morphe Patcher |
| `vendor/ARSCLib`, `vendor/smali` | pinned forks the engine builds against |
| `docs/` | guides |

## Release signing

`assembleRelease` reads `app/keystore.jks` and three environment variables:

```bash
KEYSTORE_PASSWORD=...
KEYSTORE_ENTRY_ALIAS=...
KEYSTORE_ENTRY_PASSWORD=...
```

None of these belong in the repository. CI supplies them as secrets, and no release is published
until a build has been produced and installed successfully.

## Firebase

Optional. Without `app/google-services.json` the app builds and runs, and update push notifications
are inactive. See [FIREBASE.md](FIREBASE.md).

## The embedded JVM

The build downloads three OpenJDK builds for Android (arm64, armeabi-v7a, x86_64), each pinned by
SHA-256 in `app/build.gradle.kts`, and packs the patcher host beside them. Those downloads are
several hundred megabytes and need `github.com` reachable. A build that cannot reach it fails at the
`prepareEmbeddedJvm*` tasks, not at compile time.

## Reproducibility

See [REPRODUCIBILITY.md](REPRODUCIBILITY.md).
