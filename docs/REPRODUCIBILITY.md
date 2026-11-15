# Reproducible builds

A build is reproducible here in the sense that matters: the same commit, on a clean machine, resolves
the same inputs. It is **not** yet bit-for-bit reproducible, and this document says where that
stands rather than implying more.

## What is pinned

| Input | Pinned by |
|---|---|
| Gradle distribution | `gradle/wrapper/gradle-wrapper.properties`, `distributionSha256Sum` |
| Android Gradle Plugin, Kotlin, all libraries | `gradle/libs.versions.toml` |
| ARSCLib | submodule commit `50418a07…`, recorded in `vendor/PINS.md` |
| smali | submodule commit `bbabf5f9…`, recorded in `vendor/PINS.md` |
| MAX Engine | the commit in this repository (`engine/`) |
| Embedded JREs (arm64, armeabi-v7a, x86_64) | `sha256` per ABI in `app/build.gradle.kts`, checked after download |
| The patcher host archive | built in-repo from `patcher-core` |

CI fails if a submodule is not at its pinned commit (`dev-build.yml`, "Verify submodule pins").

## What is not pinned yet

1. **Gradle dependency verification.** No `gradle/verification-metadata.xml` exists, so a resolved
   artifact's checksum is trusted rather than checked. Generating one requires running the build with
   `--write-verification-metadata sha256`, which has not happened: there is no JDK, no Android SDK and
   no network to the registries where this was prepared. This is the single highest-value remaining
   item, and it is a one-command change once a build machine exists.
2. **Dependency locking.** `dependencyLocking` is not enabled. Same reason, same remedy.
3. **Bit-for-bit identical APKs.** The version code is derived from `System.currentTimeMillis()` in
   `app/build.gradle.kts`, so two builds of the same commit produce different `versionCode` values
   and therefore different APK bytes. If byte-identical output is wanted later, that timestamp has to
   become a build input (for example `SOURCE_DATE_EPOCH`).
4. **`--no-daemon` and a fixed locale/timezone in CI** are not enforced.

## Reproducing a release, step by step

```bash
git clone --recurse-submodules https://github.com/sufiyan-sabeel/max-patcher
cd max-patcher
git checkout <release-commit>
git submodule update --init --recursive
test "$(git -C vendor/ARSCLib rev-parse HEAD)" = 50418a07a806f7fc6d8a567be81d951d6e360079
test "$(git -C vendor/smali rev-parse HEAD)" = bbabf5f9980028daba64c19366483bbddc05cf01
./engine/gradlew -p engine test apiCheck
./gradlew :app:assembleRelease
sha256sum app/build/outputs/apk/release/*.apk
```

The `versionCode` will differ from a published APK because of the timestamp; everything else should
match. That caveat is deliberate and stated, not hidden.
