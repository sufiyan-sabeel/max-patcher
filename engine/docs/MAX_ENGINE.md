# MAX Engine: what this fork changes

MAX Engine is Morphe Patcher 2.3.0 with a build and distribution fork, not a code fork. That is a
deliberate position: the brief says not to rewrite the engine without a demonstrated technical
reason, and at this stage there is none.

## Changes at 0.1.0

| Change | Reason | Effect on patches |
|---|---|---|
| Gradle project renamed `max-engine` | independent identity | none |
| Version `0.1.0`, independent of the app | three separately versioned artifacts, as specified | none — bundles declare a minimum patcher version, and 0.1.0 sorts below 2.3.0, so a bundle requiring 2.3.0 is rejected. See "Version numbering" below |
| ARSCLib and smali resolved from vendored submodules | removes JitPack and the authenticated registry from the build | none |
| Consumed as an included build by MAX Patcher | the app stops downloading the patcher from GitHub Packages | none |
| `api/max-engine.api` carried over unchanged (byte-identical to upstream's `morphe-patcher.api`) | the ABI contract is explicit and diffable | none |

### Why the ABI dump is called `max-engine.api`

The binary-compatibility validator names the dump after the Gradle project, so renaming the project to
`max-engine` made `apiCheck` expect `api/max-engine.api`. The file was renamed; its **contents** are
byte-identical to upstream's `morphe-patcher.api` at 2.3.0, which is what "the ABI is unchanged"
means and how it can be checked with `diff`.

## What is deliberately unchanged

- Every `app.morphe.patcher.*` package, class and method signature.
- The `patch { }` DSL, `dependsOn`, `compatibleWith`, `Fingerprint`, `resourceLiteral` and the rest
  of the filter API.
- Behaviour of the resource table lookup, including the exception text patches surface:
  `Could not find resource type: DRAWABLE name: …`.
- The `ResourceIds` contract that makes a missing resource fatal by default.

## Version numbering

MAX Engine is **2.3.1**. That continues the upstream line rather than restarting at 0.1.0, and it is
not a number picked to slip past a check:

- Every patch bundle declares `Patcher-Version` in its manifest, and the manager refuses a bundle that
  needs a newer patcher than the one it carries (`isPatcherOutdated`).
- MAX Engine's public ABI is byte-for-byte upstream 2.3.0's: `api/max-engine.api` is byte-identical to upstream's dump,
  and `apiCheck` fails if it ever moves.
- A bundle built for 2.3.0 therefore *does* run here, and semver says an API-compatible change is a
  patch bump. `0.1.0` would have rejected every inherited bundle on a technicality.
- MAX's increments continue from there: 2.3.2, 2.3.3, and 2.4.0 only when the ABI moves.

The app no longer reads the patcher version from its own version catalog — that string could drift
from the engine it actually carries. `app/build.gradle.kts` reads `engine/gradle.properties` and
fails the build if the version is missing, so what the app reports is what the engine is.

The three artifacts stay independently versioned: MAX Patcher 0.1.0, MAX Engine 2.3.1, MAX Patches
0.1.0.

## Testing

- The upstream suite (10 test classes under `src/test/kotlin`) runs unchanged.
- `ResourceIdsTest` was added to pin the resource lookup contract that the YouTube 21.45.82 failure
  depends on: a missing resource is a fatal error by default, `has`/`getOrNull` are the non-fatal
  paths, and the exception text is exactly what the manager shows the user.
- `apiCheck` (binary compatibility validator) is the regression test for the ABI.

**These tests have not been executed.** No JDK, Gradle or network-to-registry exists in the
environment where this fork was prepared. They are written to run, not known to pass.

## Rules for changing the engine

Every future engine change carries: the root cause, the code change, a regression test, a
compatibility assessment against `api/max-engine.api`, and a line in this document.
