# Firebase: console setup, step by step

Do this once, in your own Google account. **Do not reuse the Morphe Firebase project** — a MAX build
must not depend on another project's API key, quota or data.

## 1. Create the project

1. Open <https://console.firebase.google.com> and choose **Add project**.
2. Name it `max-patcher`. Accept or change the generated project id (it must be globally unique).
3. Google Analytics: not needed. Turn it off; MAX collects nothing.
4. Wait for provisioning, then **Continue**.

## 2. Register the Android app

1. On the project overview, choose the **Android** icon (**Add app** → Android).
2. **Android package name:** `app.maxpatcher.manager` — exactly, it must match `applicationId`.
3. **App nickname:** `MAX Patcher`.
4. **Debug signing certificate SHA-1:** leave empty. It is only needed for Google Sign-In, Dynamic
   Links and Phone Auth, none of which MAX uses.
5. **Register app**.
6. **Download `google-services.json`**. This is the only file the build needs.

## 3. Optional second app for debug builds

Only if you build with `applicationIdSuffix` and want push notifications in debug builds too. Register
a second Android app with package `app.maxpatcher.manager.debug`, download its config separately, and
keep the two files apart. If you skip this, debug builds simply have no Firebase, which is fine —
`syncFcmTopics` logs and returns.

## 4. Put the file where Gradle expects it

Local build:

```bash
cp ~/Downloads/google-services.json app/google-services.json
```

That path is gitignored. Never commit it.

CI: base64 the file and store it as a secret, then decode it in the workflow before the Gradle step.

```bash
base64 -w0 app/google-services.json    # store the output as FIREBASE_GOOGLE_SERVICES_B64
```

```yaml
- name: Firebase config
  run: echo "${{ secrets.FIREBASE_GOOGLE_SERVICES_B64 }}" | base64 -d > app/google-services.json
```

That step is deliberately **not** in `dev-build.yml` yet: adding it before the secret exists would
break every build. Add it in the same commit that adds the secret.

## 5. Topics

The app subscribes itself; nothing to create in the console. Names are fixed in
`app/src/main/java/app/morphe/manager/util/FcmTopicManager.kt`:

| Topic | Subscribed when |
|---|---|
| `max_patcher_updates` | notifications on |
| `max_patcher_updates_dev` | notifications on and manager prereleases on |
| `max_patches_updates` | notifications on |
| `max_patches_updates_dev` | notifications on and patch prereleases on |

Whatever sends the pushes must use exactly these names. A message needs a `type` key in its `data`
map: `manager_update` or `bundle_update`, each with an optional `version`.

## 6. Sending pushes (server side)

1. Project settings → **Service accounts** → **Generate new private key**. A JSON file downloads.
2. Store it as the secret `FCM_SERVICE_ACCOUNT_JSON` (the whole file, as text) and the project id as
   `FCM_PROJECT_ID`.
3. The upstream script `.github/scripts/send_fcm.py` sends the pushes and works unchanged once those
   two secrets exist. Keep the key out of the repository and out of logs.

## 7. Verify

1. Build with the config present and install on a device.
2. Settings → notifications on.
3. In the console: **Messaging** → **New campaign** → **Firebase Notification messages**, send to
   topic `max_patcher_updates_dev`, with `data` = `{"type":"manager_update","version":"0.1.0"}`.
4. The device should show a MAX Patcher update notification.

## What breaks without it

Nothing at runtime. No push notifications; update checks fall back to WorkManager on devices without
Google Play services, and on devices with it the periodic check stays cancelled while notifications
are enabled — so update notices arrive when the app is opened. That is upstream behaviour, preserved
as-is and documented rather than changed silently.
