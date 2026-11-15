# Patch bundle signing

## What a signature covers

A signed bundle is a normal `.mpp` zip whose archive **comment** carries:

```
MorpheSig1 <base64 public key> <base64 signature>
```

- The key is a **raw Ed25519** key, 32 bytes, Base64. Not PEM, not an X.509 certificate.
- The signature is Ed25519 over `MorpheSig1` followed by the SHA-256 digest of the archive as it was
  before signing — that is, every byte of the bundle with an empty comment.
- Zip readers ignore the comment, so a signed bundle loads exactly like an unsigned one.

`MorpheSig1` is a wire-format constant, not branding. It stays as it is because the signing side (the
`app.morphe.patches` Gradle plugin) and the reading side must agree; renaming it would mean forking the
signer for no security gain. It is recorded here so nobody "fixes" it later.

Verification lives in `PatchBundleSignature.verify` (engine) and is called from
`PatchBundleSource` (app). A bundle whose signature does not verify against the key the source
declares is **refused**, not warned about.

## Two keys, two purposes

| Key | Signs | Where the private half lives | Public half |
|---|---|---|---|
| **Development** | `dev`-channel bundles, built from the `dev` branch | GitHub Actions secret `MAX_PATCHES_SIGNING_KEY_DEV` | published in each dev release's `patches-bundle.json` |
| **Production** | `main`-channel bundles, the ones users get by default | GitHub Actions secret `MAX_PATCHES_SIGNING_KEY_PROD` | `patches-bundle.json` + pinned in `max-patcher`'s `SOURCE_SIGNING_KEY` |

Why two: a leaked development key must not let anyone forge a production bundle. A dev build with a
dev key is expected to be replaced, so rotating it costs nothing.

The app verifies against the key a source declares, falling back to the built-in default only when the
source declares none — so a third-party source with its own key works, and MAX's own bundles are
pinned.

## Generating the keys — exact process

Nothing here creates a key for you. Run it yourself, and keep the private half out of the repository,
out of chat, and out of shell history (`set +o history` first, or use a scratch machine).

```bash
# 1. Generate a raw Ed25519 private key (32-byte seed).
openssl genpkey -algorithm ed25519 -out max-patches-prod.pem

# 2. Extract it in the form the signer wants: Base64 of the raw 32-byte seed.
openssl pkey -in max-patches-prod.pem -outform DER | tail -c 32 | base64 -w0
#    -> that single line is MAX_PATCHES_SIGNING_KEY_PROD

# 3. Derive the public key in the same raw form.
openssl pkey -in max-patches-prod.pem -pubout -outform DER | tail -c 32 | base64 -w0
#    -> that single line is the public key, the one that gets published
```

Repeat with `-out max-patches-dev.pem` for the development key.

Verify the pair before trusting it: sign a throwaway bundle and compare the public key the signer
reports with the one you derived in step 3. If they differ, the extraction was wrong; do not proceed.

```bash
# from max-patches, after the build plugin is wired:
./gradlew signBundle -Pbundle=patches/build/libs/patches-0.1.0.mpp -Pkey="$MAX_PATCHES_SIGNING_KEY_PROD"
```

## Secrets to create

In `max-patches` → Settings → Secrets and variables → Actions:

| Secret | Contents | Needed for |
|---|---|---|
| `MAX_PATCHES_SIGNING_KEY_DEV` | Base64 raw Ed25519 private key (dev) | dev channel releases |
| `MAX_PATCHES_SIGNING_KEY_PROD` | Base64 raw Ed25519 private key (prod) | stable releases |
| `GHP_PACKAGES_TOKEN` | PAT with `read:packages` | building the patches plugin |
| `FCM_SERVICE_ACCOUNT_JSON` | Firebase service account JSON | release notifications |
| `FCM_PROJECT_ID` | Firebase project id | release notifications |

In `max-patcher`:

| Secret | Contents | Needed for |
|---|---|---|
| `KEYSTORE_B64` | Base64 of `app/keystore.jks` | signing the release APK |
| `KEYSTORE_PASSWORD`, `KEYSTORE_ENTRY_ALIAS`, `KEYSTORE_ENTRY_PASSWORD` | keystore credentials | signing the release APK |
| `FIREBASE_GOOGLE_SERVICES_B64` | Base64 of `google-services.json` | Firebase in CI builds |
| `FCM_SERVICE_ACCOUNT_JSON`, `FCM_PROJECT_ID` | Firebase service account | FCM pushes |

Never commit any of these. `.gitignore` covers `*.jks`, `google-services.json` and `*.pem`; if one
slips in, say so immediately rather than quietly deleting it — the key is already public.

## Pinning the production key in the app

Once the production key exists, put its **public** half in
`app/src/main/java/app/morphe/manager/util/Constants.kt`:

```kotlin
const val SOURCE_SIGNING_KEY = "<base64 public key>"
```

It is empty today, which means "trust only what a source declares". Filling it in makes MAX Patches
verifiable even if a source's metadata is tampered with.

## Rotation

Rotate when a key is exposed, when a maintainer leaves, or on a schedule you set yourself.

1. Generate the new key pair (process above).
2. Sign the next bundle with the new key and publish the new public key in `patches-bundle.json`.
3. Release one bundle **signed by the old key** that declares the new key, so existing installs learn
   the new key from a signature they already trust. This is the step people skip, and it is the one
   that matters: without it, every existing install refuses the next bundle.
4. After a full cycle, put the new public key into `max-patcher`'s `SOURCE_SIGNING_KEY` and ship an app
   update.
5. Retire the old key: stop signing with it, and note the retirement date in `COMPATIBILITY.md`.

Emergency rotation (key leaked) cannot use step 3. In that case users must update the app before the
next bundle, and the release notes say so plainly.

## What this does not protect against

- A compromised build pipeline: whoever can run the signing step can sign anything. Secrets scoping
  and branch protection are the real defence there.
- A malicious patch source using its own key: the app trusts a source's declared key, so it cannot
  audit what a third-party patch does. That is stated in `SECURITY.md` too.
- The APK's own signature. That is a separate key (the release keystore) with a separate lifecycle.
