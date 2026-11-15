# GitHub Pages for the add-source page

The add-source page is a static file in this repository: `add-source.html`. It needs to be served over
HTTPS for links to work, and GitHub Pages does that with no build step and no cost.

The page URL the app generates links to is:

```
https://sufiyan-sabeel.github.io/max-patcher-docs/add-source.html
```

## Enable it (once, in the browser)

1. Open https://github.com/sufiyan-sabeel/max-patcher-docs/settings/pages
2. **Source**: *Deploy from a branch*.
3. **Branch**: `main`, folder **/ (root)**. Save.
4. Wait for the green banner — usually under a minute for a static file.
5. **Enforce HTTPS** becomes available once the certificate is issued; tick it.

There is no workflow to add: Pages serves the branch contents directly. No Actions minutes are spent.

## Verify the deployment

```bash
# 1. The page is served, with the right content type and no redirect to a 404
curl -sSI https://sufiyan-sabeel.github.io/max-patcher-docs/add-source.html | head -5
#    expect: HTTP/2 200, content-type: text/html

# 2. It contains the handoff code, not a placeholder
curl -sS https://sufiyan-sabeel.github.io/max-patcher-docs/add-source.html | grep -c "intent://"

# 3. A well-formed link renders a repository card
open "https://sufiyan-sabeel.github.io/max-patcher-docs/add-source.html"

# 4. A malformed link is refused, and says why
open "https://sufiyan-sabeel.github.io/max-patcher-docs/add-source.html"

# 5. A missing repository is refused, distinctly from an unreachable API
open "https://sufiyan-sabeel.github.io/max-patcher-docs/add-source.html"
```

## What a working page does

| Input | Expected |
|---|---|
| `?github=owner/repo` | card shows full name, description, licence, last push; button enabled |
| `?gitlab=owner/repo` | same, from the GitLab API |
| `?github=owner` | refused: "not a valid repository. It must look like owner/repo" |
| `?github=owner/does-not-exist` | refused: "does not exist, or it is private" |
| no provider parameter | refused: "does not name a GitHub or GitLab repository" |
| API unreachable | refused: "could not be reached right now", button disabled |

In every refused case the button is disabled, so a broken link cannot half-work.

## The handoff, and why the app is the one that asks

The page's button is an `intent://` URL targeting `app.maxpatcher.manager`. If the app is installed,
Android opens it and the app shows its own confirmation dialog naming the repository; nothing is added
until the user confirms. If it is not installed, the intent goes nowhere — which is correct, because
the page cannot install anything.

The app also refuses a source it already has, so a link cannot add the same repository twice.

## If the URL 404s

- Pages not enabled yet, or the branch/folder is wrong — recheck steps 1–3.
- The file is on a different branch than the one Pages serves.
- A custom domain was configured on the repository; then the page is served from that domain and
  `ADD_SOURCE_BASE_URL` in `app/src/main/java/app/morphe/manager/util/Constants.kt` must change with it.

## Changing the URL

`ADD_SOURCE_BASE_URL` and `ADD_SOURCE_PATH` in
`app/src/main/java/app/morphe/manager/util/Constants.kt` are the only places the app knows about this
page. The deep-link handler additionally accepts the upstream host, so links shared before the move keep
working.
