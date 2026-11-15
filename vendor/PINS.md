# Vendored forks: pinned commits and licences

Both submodules are third-party forks that MAX Patcher builds from source rather than downloading as
binaries. They are pinned to exact commits so a build is reproducible and cannot drift under us.

| Path | Repository | Pinned commit | Licence |
|---|---|---|---|
| `vendor/ARSCLib` | https://github.com/MorpheApp/ARSCLib | `675bed2de397097400ba6f0c808e28005ae5aa53` | Apache-2.0 |
| `vendor/smali` | https://github.com/MorpheApp/smali | `bbabf5f9980028daba64c19366483bbddc05cf01`, **vendored as source, not as a submodule** — see below | GPL-3.0 (Morphe contributors' changes); the upstream smali it derives from is BSD-3-Clause, recorded in `third_party/NOTICE` |

## What each one is

**ARSCLib** reads and writes Android resource tables — binary `resources.arsc` and `public.xml`.
MAX Engine uses it for resource lookups, which is the layer where a renamed drawable becomes a
"Could not find resource type" failure. Apache-2.0 is compatible with GPLv3, so vendoring it into a
GPLv3 project is fine, and its licence and NOTICE files travel with the submodule.

**smali** is the assembler/disassembler over dexlib2 that MAX Engine uses for bytecode. The Morphe
fork exists because upstream smali releases stopped. Its `LICENSE` is a symlink to
`third_party/NOTICE`, which carries both the original BSD-3-Clause terms and the GPLv3 terms the
Morphe contributors applied to their changes. Both are preserved unmodified by vendoring.

## Why ARSCLib is pinned to 675bed2d and not to the branch head

The engine's own version catalog names ARSCLib `675bed2de3`. That is not the current head of
MorpheApp/ARSCLib: commits after it removed `com.reandroid.arsc.value.ValueType`, which the engine
imports in `AndroidManifestHelper.kt`. Pinning the branch head (`50418a0`) does not fail at
resolution — it fails later, at `:compileKotlin`, with `Unresolved reference 'ValueType'`, which
looks like an engine bug and is not one.

So the pin follows the engine's declared version, not the newest commit. When the engine is updated to
a newer ARSCLib API, this pin moves with it, and the change is proved by `:compileKotlin` succeeding.

## Why smali is vendored source and not a submodule

`vendor/smali` is a copy of MorpheApp/smali at `bbabf5f9`, committed into this repository, with one
local change: the SPDX SBOM plugin (`org.spdx.sbom` 0.8.0) is no longer applied in
`vendor/smali/dexlib2/build.gradle`.

Reason: that plugin is published only to the Gradle Plugin Portal, whose artifact CDN refuses some
networks outright (HTTP 403 here). Because Gradle resolves a build's plugins before it can skip any
task, one portal-only plugin in a vendored sub-build made the entire MAX build unresolvable — the app,
the engine, everything. It is not on Maven Central, so it could not be redirected.

What the plugin does, and why dropping it from this build is not dropping an SBOM requirement: it
generates an SPDX SBOM for dexlib2's *own published artifacts*. MAX consumes dexlib2 as a composite
build and never publishes it, so that SBOM was never part of a MAX release. MAX's own release SBOM is
generated separately, from the APK, in the release workflow (see `docs/REPRODUCIBILITY.md`), and the
requirement is unchanged.

The alternative — a submodule plus a scripted patch step — was rejected because a clone would not
build without running that step, and a build that needs an undocumented post-step is not reproducible.

Cost of this choice: updating smali is a copy-and-reapply rather than a `git submodule update`. The
change is two blocks in one file, marked in place with an explanation, so re-applying it is a minute's
work and the intent is visible to anyone reading the file.

## Updating a pin

1. Check out the new commit in the submodule and update the table above and the `.gitmodules` entry.
2. Build and run `./engine/gradlew -p engine test apiCheck`.
3. Patch a real APK with MAX Patches and install the result.
4. Record the change, with the reason, in the commit message. A pin bump without a reason is not
   reviewable.

## Why not JitPack

Upstream resolves these two from JitPack. JitPack is a third-party build service: a build that needs
it is neither reproducible nor available when JitPack is slow, rate-limited or down. Vendoring the
sources removes that dependency and, with the engine also built from source, leaves MAX Patcher with
no package-registry dependency at all.
