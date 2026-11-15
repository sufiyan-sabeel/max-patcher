<div align="center">

# MAX Patcher

**An independent Android patcher, forked from Morphe Manager.**

Created by Umaiz Sufiyan

</div>

MAX Patcher applies patch bundles to Android apps: it finds the installed app or an APK, lets you
pick patches, runs them in an embedded JVM, signs the result and installs it. It is a rebranded,
independently maintained fork, not an official Morphe release and not affiliated with or endorsed
by the Morphe project.

## What it is

- A fork of [Morphe Manager](https://github.com/MorpheApp/morphe-manager) with the same
  architecture, screens, workflows and features.
- Paired with [MAX Patches](https://github.com/sufiyan-sabeel/max-patches), an independently
  maintained patch collection.
- GPLv3, like the project it came from. See [NOTICE](NOTICE) for the upstream attribution and the
  list of changes.

## Status

Development build. **Version 0.1.0 is not a stable release** and its patch compatibility is
inherited, not verified: see [the compatibility matrix](https://github.com/sufiyan-sabeel/max-patcher-docs/blob/main/COMPATIBILITY.md) before trusting any patch on any
app version.

## Features

Everything the upstream manager does, including:

- App discovery, APK selection and split APK handling
- Patch source management, updates, selection and configuration
- Simple and expert patching modes, per-patch options, dependency resolution
- Detailed logs, progress and resource usage while patching
- APK signing with a managed keystore, export, install (session, root, Shizuku) and saved APKs
- Queue and batch patching, notifications, backup and restore
- Settings, appearance, About and open-source licence screens

## Building

See [docs/BUILDING.md](docs/BUILDING.md). The build needs **no GitHub token**: the patcher is
built from `engine/` in this repository and its libraries from `vendor/` submodules pinned to exact
commits. [docs/DEPENDENCIES.md](docs/DEPENDENCIES.md) explains what that replaced and where a token
is still needed (only in `max-patches`).

## Repositories

| Repository | What it holds |
|---|---|
| [max-patcher](https://github.com/sufiyan-sabeel/max-patcher) | This Android application, and MAX Engine under `engine/` |
| [max-patches](https://github.com/sufiyan-sabeel/max-patches) | Patch collection and `.mpp` bundles |
| [max-patcher-docs](https://github.com/sufiyan-sabeel/max-patcher-docs) | Guides and compatibility matrix |

## Versions

Three version numbers move independently: MAX Patcher (the app), MAX Engine (the patcher library),
MAX Patches (the bundle). All start at 0.1.0.

## Licence and attribution

GPLv3. The upstream NOTICE is preserved in full in [NOTICE](NOTICE), which also lists what MAX
Patcher changed and why. Upstream branding assets are not covered by the upstream open-source
licence and none of them are redistributed here; the MAX icon, wordmark and notification icons are
original artwork.

"Morphe" is used in this project only descriptively, to say what MAX Patcher is compatible with.
