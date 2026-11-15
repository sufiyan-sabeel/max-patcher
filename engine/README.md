# MAX Engine

The patching engine behind [MAX Patcher](https://github.com/sufiyan-sabeel/max-patcher), forked
from [Morphe Patcher](https://github.com/MorpheApp/morphe-patcher) and maintained independently.

- **Version 0.1.0.** Versioned separately from MAX Patcher and MAX Patches; see `gradle.properties`.
- **Same API.** The public surface is pinned by `api/max-engine.api` and checked by the binary
  compatibility validator. Nothing in a patch bundle compiled against Morphe Patcher 2.3.0 needs to
  change.
- **Built from source.** ARSCLib and smali are vendored submodules pinned to exact commits
  (`vendor/PINS.md` at the repository root), so the build needs no package registry.
- **Not a rewrite.** At 0.1.0 the engine patches exactly as 2.3.0 does. Changes are recorded in
  [docs/MAX_ENGINE.md](docs/MAX_ENGINE.md).

## Building

```bash
git submodule update --init --recursive   # from the MAX Patcher repository root
./gradlew -p engine build
./gradlew -p engine test
./gradlew -p engine apiCheck        # fails if the public ABI moved
```

Inside MAX Patcher the engine is an included build, so `./gradlew :app:assembleDebug` at the root
builds it as part of the app.

## Licence

GPLv3. See [NOTICE](NOTICE) for the upstream attribution and the list of changes.
