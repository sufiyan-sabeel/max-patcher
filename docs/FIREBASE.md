# Firebase: migration plan and secrets

MAX Patcher keeps the upstream notification features — update pushes delivered by Firebase Cloud
Messaging — but it cannot reuse Morphe's Firebase project, because an FCM client is bound to the
`applicationId` and to the project's API key.

## What the app needs

One file, `app/google-services.json`, registered for the package **`app.maxpatcher.manager`** (and
optionally `app.maxpatcher.manager.debug` for debug builds).

It is gitignored on purpose. The Gradle plugin is applied only when the file exists, so a source
build without it compiles and runs; FCM topic sync is skipped and logged, and update checks fall
back to WorkManager.

## Steps to create it

1. Open the [Firebase console](https://console.firebase.google.com) and create a project, e.g.
   `max-patcher`.
2. Add an **Android** app with package name `app.maxpatcher.manager`. Skip the SHA-1 for now; it is
   only needed for Google Sign-In and Dynamic Links, neither of which this app uses.
3. Optionally add a second Android app for `app.maxpatcher.manager.debug`.
4. Download `google-services.json` and place it at `app/google-services.json` locally.
5. For CI, base64 it and store it as a repository secret, then decode it in the workflow before the
   Gradle step:

   ```bash
   base64 -w0 app/google-services.json     # store this as FIREBASE_GOOGLE_SERVICES_B64
   ```

   ```yaml
   - name: Firebase config
     run: echo "${{ secrets.FIREBASE_GOOGLE_SERVICES_B64 }}" | base64 -d > app/google-services.json
   ```

   That workflow step is **not** added yet — it belongs with the release pipeline work in Phase 7,
   and adding it before the secret exists would break the build.

## Topics the app subscribes to

Topic names are MAX's own, defined in `app/src/main/java/app/morphe/manager/util/FcmTopicManager.kt`:

| Topic | Subscribed when |
|---|---|
| `max_patcher_updates` | notifications on |
| `max_patcher_updates_dev` | notifications on and manager prereleases on |
| `max_patches_updates` | notifications on |
| `max_patches_updates_dev` | notifications on and patch prereleases on |

Whatever sends the pushes must use exactly these names.

## Message contract

Every message needs a `type` key in its `data` map: `manager_update` or `bundle_update`, each with
an optional `version`. Unknown types are ignored, so the sender can add types later.

## Server side

Sending pushes needs a service account JSON (`FCM_PROJECT_ID`, `FCM_SERVICE_ACCOUNT_JSON`). The
upstream workflow script `.github/scripts/send_fcm.py` does this and works unchanged once those two
secrets exist. Keep the service account key out of the repository and out of logs.

## What breaks without it

Nothing at runtime. Without the file: no push notifications, and `syncFcmTopics` returns early with
a debug log. Background update checks continue through WorkManager on non-GMS devices; on GMS
devices with notifications enabled, the periodic check is skipped upstream on the assumption that
FCM delivers. Until Firebase is configured, set the update-check interval so WorkManager runs
instead, or accept that update notices arrive only when the app is opened.
