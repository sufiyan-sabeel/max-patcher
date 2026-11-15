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
| `api/morphe-patcher.api` carried over unchanged | the ABI contract is explicit and diffable | none |

## What is deliberately unchanged

- Every `app.morphe.patcher.*` package, class and method signature.
- The `patch { }` DSL, `dependsOn`, `compatibleWith`, `Fingerprint`, `resourceLiteral` and the rest
  of the filter API.
- Behaviour of the resource table lookup, including the exception text patches surface:
  `Could not find resource type: DRAWABLE name: …`.
- The `ResourceIds` contract that makes a missing resource fatal by default.

## Version numbering

`0.1.0` is below `2.3.0` in semver ordering. Patch bundles declare `patcher: "2.3.0"` or similar in
their manifest, and the manager refuses a bundle that requires a newer patcher than the one it
carries. With the engine at 0.1.0, MAX Patches bundles must declare `0.1.0`, or the manager will
report the source as incompatible.

Two options, both acceptable, decided in Phase 4 when the bundle is built:

1. Start MAX Patches at a bundle that requires patcher `0.1.0`. Clean, but the first bundle cannot
   also be used with an upstream Morphe manager.
2. Version the engine `2.3.0-max.1` instead. Semver sorts a prerelease *below* 2.3.0, which has the
   same problem, so the workable variant is `2.3.1-max.1`.

Until that is decided, this document records the constraint rather than hiding it.

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
compatibility assessment against `api/morphe-patcher.api`, and a line in this document.
