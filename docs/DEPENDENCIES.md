# Dependencies, credentials and what changed

## The problem this replaced

Upstream resolves its own artifacts from `https://maven.pkg.github.com/MorpheApp/registry`. GitHub
Packages authenticates every request, including for public packages: an anonymous request for
`app.morphe:morphe-patcher:2.3.0` returns `401`. A fork's default `GITHUB_TOKEN` only reads packages
owned by the fork's own account, so a plain fork cannot resolve them without a personal token.

Upstream also resolves `com.github.MorpheApp:ARSCLib` and the `com.github.MorpheApp.smali` modules
from JitPack, a third-party build service that can be slow, rate-limited or down — which makes a
build neither reproducible nor always available.

## What MAX Patcher does instead

| Dependency | Upstream source | MAX source |
|---|---|---|
| `app.morphe:morphe-patcher` | GitHub Packages registry (401 without a token) | `engine/`, an included build in this repository |
| `com.github.MorpheApp:ARSCLib` | JitPack | `vendor/ARSCLib`, a submodule pinned to a commit |
| `com.github.MorpheApp.smali:*` | JitPack | `vendor/smali`, a submodule pinned to a commit |

`settings.gradle.kts` substitutes each module coordinate with the local build:

```kotlin
includeBuild("engine") {
    dependencySubstitution {
        substitute(module("app.morphe:morphe-patcher")).using(project(":"))
    }
}
```

The engine's own `settings.gradle.kts` finds the two vendored libraries and substitutes their
coordinates in turn.

**Result: building MAX Patcher needs no package registry and no token at all.** Pinned commits are
in [`vendor/PINS.md`](../vendor/PINS.md).

## Where a token is still needed

`max-patches` applies the `app.morphe.patches` Gradle plugin, which is published only to the Morphe
GitHub Packages registry. Until that plugin is either vendored or replaced, building the patch
collection needs a token with `read:packages`.

### Creating it

Never paste it into a chat, a source file, a commit or a log.

1. GitHub → **Settings** → **Developer settings** → **Personal access tokens** → **Tokens
   (classic)** → **Generate new token (classic)**.
2. Name it `max-patches-read-packages`, pick an expiry you will renew.
3. Tick exactly one scope: **`read:packages`**.
4. Generate and copy the value once.
5. In `max-patches`: **Settings** → **Secrets and variables** → **Actions** → **New repository
   secret**, name it **`GHP_PACKAGES_TOKEN`**.

For a local build of `max-patches`, the same values in `~/.gradle/gradle.properties`:

```properties
gpr.user=your-github-username
gpr.key=ghp_your_token
```

A `401` on `app.morphe.patches` means the token expired or lacks `read:packages`.

## Token permissions, and what each operation needs

| Operation | Needed on the token | Where |
|---|---|---|
| Reading `MorpheApp/registry` packages | `read:packages` | `max-patches` only |
| Creating repositories | `repo` (classic) or Administration: write (fine-grained) | done already |
| Pushing commits | `repo` / Contents: write | `max-patcher`, `max-patches`, docs |
| Creating releases | `repo` / Contents: write | Phase 7, after a real build |
| Editing workflows | `workflow` (classic) | only if a workflow file is pushed by an app token |

A token with only `read:packages` **cannot** push. Repository publishing needs `repo` (or
fine-grained Contents: read and write plus Workflows: write if `.github/workflows/**` is touched).

## Firebase

`app/google-services.json` is not committed. See [FIREBASE.md](FIREBASE.md).
