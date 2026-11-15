/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-manager
 *
 * Modified for MAX Patcher, see NOTICE.
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.manager.util

import androidx.compose.ui.graphics.Color
import app.morphe.manager.BuildConfig
import app.morphe.manager.ui.theme.MaxBrandCyan
import app.morphe.manager.ui.theme.MaxBrandTeal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

const val tag = "MAX Patcher"

const val SOURCE_NAME = "MAX Patches"
const val MANAGER_REPO_URL = "https://github.com/sufiyan-sabeel/max-patcher"
const val SOURCE_REPO_URL = "https://github.com/sufiyan-sabeel/max-patches"

/**
 * Key the releases of [SOURCE_REPO_URL] are signed with, see PatchBundleSignature.
 *
 * Empty until MAX Patches has its own signing key: a bundle that declares its key in
 * `patches-bundle.json` is verified against that key, so nothing depends on this default.
 * Fill this in once the MAX key pair exists, and never commit the private half.
 */
const val SOURCE_SIGNING_KEY = ""

/**
 * MAX's own API, once there is one. Until then this is the upstream endpoint, kept so no
 * feature is lost in this build: it serves the APK download helper's web search and the
 * patch source blocklist. Point it at MAX infrastructure before a public release.
 */
const val API_BASE_URL = "https://api.morphe.software"

/** Sent on every request to [API_BASE_URL]. */
const val APP_USER_AGENT = "MAX-Patcher/${BuildConfig.VERSION_CODE}"

const val PROJECT_WEBSITE_URL = "https://github.com/sufiyan-sabeel/max-patcher"
const val COMMUNITY_PATCHES_URL = "https://github.com/sufiyan-sabeel/max-patcher-docs"
const val BLOCKED_SOURCES_URL = "$API_BASE_URL/v2/blocked-sources"

/**
 * Base of the page behind add-source links, which MAX hosts as a static page in the docs
 * repository (GitHub Pages). Upstream served this from its own website.
 */
const val ADD_SOURCE_BASE_URL = "https://sufiyan-sabeel.github.io/max-patcher-docs"

/** The add-source page itself: ?github|gitlab=owner/repo(&name=...) */
const val ADD_SOURCE_PATH = "/add-source.html"

/** Host of [ADD_SOURCE_BASE_URL], which the deep-link check compares against. */
const val ADD_SOURCE_HOST = "sufiyan-sabeel.github.io"

/** What a source link looks like, shown in a field waiting for one. */
const val REMOTE_URL_PLACEHOLDER = "github.com/owner/repo"

/** Raw GitHub URL for the stable MAX Patcher release JSON (main branch) */
const val MANAGER_RELEASE_JSON_URL = "https://raw.githubusercontent.com/sufiyan-sabeel/max-patcher/refs/heads/main/app-release.json"

/** Raw GitHub URL for the pre-release MAX Patcher release JSON (dev branch) */
const val MANAGER_PRERELEASE_JSON_URL = "https://raw.githubusercontent.com/sufiyan-sabeel/max-patcher/refs/heads/dev/app-release.json"

/** Controls whether manager updates are fetched directly from JSON files in the repository instead of using the GitHub API */
const val USE_MANAGER_DIRECT_JSON = true

/** Controls whether patches are fetched directly from JSON files in the repository instead of using the API */
const val USE_PATCHES_DIRECT_JSON = true

/**
 * Registry of known patchable apps.
 */
object KnownApps {
    const val YOUTUBE       = "com.google.android.youtube"
    const val YOUTUBE_MUSIC = "com.google.android.apps.youtube.music"
    const val REDDIT        = "com.reddit.frontpage"
    // const val X_TWITTER     = "com.twitter.android"

    // Shared MAX brand gradient tail
    val GRADIENT_MID = MaxBrandCyan
    val GRADIENT_END = MaxBrandTeal

    val DEFAULT_DOWNLOAD_COLOR = Color(0xFF0E3F6E)

    // Default gradient for packages with no bundle-declared color
    val DEFAULT_COLORS = listOf(DEFAULT_DOWNLOAD_COLOR, GRADIENT_MID, GRADIENT_END)

    /**
     * A known patchable app entry.
     *
     * @param packageName The app's package name.
     * @param isPinnedByDefault Whether this app appears pinned on the home screen by default.
     * @param brandColor App brand color for the shimmer placeholder shown before any bundle loads.
     *   Should match the appIconColor value shipped in the bundle's Compatibility declaration.
     *   Null means fall back to [DEFAULT_COLORS].
     */
    data class Entry(
        val packageName: String,
        val isPinnedByDefault: Boolean = true,
        val brandColor: Color? = null,
    )

    /** All known app entries in display order. */
    val all: List<Entry> = listOf(
        Entry(REDDIT,        brandColor = Color(0xFFFF4500)),
        Entry(YOUTUBE,       brandColor = Color(0xFFFF0033)),
        Entry(YOUTUBE_MUSIC, brandColor = Color(0xFFFF0000)),
        // Entry(X_TWITTER, brandColor = Color(0xFF000000)),  // Uncomment when release
    )

    // Fast lookup map - built once at startup.
    private val byPackage: Map<String, Entry> = all.associateBy { it.packageName }

    /** Returns the [Entry] for [packageName], or null if not a known app. */
    fun fromPackage(packageName: String): Entry? = byPackage[packageName]

    /**
     * Ordered list of shimmer placeholder gradient colors shown during cold-start loading.
     * Uses each app's [Entry.brandColor] as the gradient start — actual bundle colors will
     * replace them once the bundle loads. Falls back to [DEFAULT_COLORS] if no brand color
     * is declared.
     */
    val DEFAULT_SHIMMER_GRADIENTS: List<List<Color>> by lazy {
        all.filter { it.isPinnedByDefault }.map { entry ->
            entry.brandColor?.let { color -> listOf(color, GRADIENT_MID, GRADIENT_END) }
                ?: DEFAULT_COLORS
        }
    }

    /**
     * Fallback display names for all packages - used only when bundle metadata and installed
     * app labels are both unavailable. Includes KnownApps entries so no separate localization
     * path is needed (bundle always provides the authoritative name anyway).
     */
    private val FALLBACK_NAMES = mapOf(
        YOUTUBE       to "YouTube",
        YOUTUBE_MUSIC to "YouTube Music",
        REDDIT        to "Reddit",
    )

    /**
     * Returns a display name for [packageName].
     * Priority: fallback table → raw package name.
     * Used as the last resort when bundle metadata and installed labels are unavailable.
     */
    fun getAppName(packageName: String): String =
        FALLBACK_NAMES[packageName] ?: packageName

    /**
     * Returns a fallback display name for [packageName], or null if not in the table.
     * Unlike [getAppName], does not fall back to the raw package name — null means unknown.
     * Used for transitional metadata fallbacks where absence should be preserved.
     */
    fun fallbackName(packageName: String): String? = FALLBACK_NAMES[packageName]
}

/**
 * Timeout applied to a single uninstall step when running as part of a batch.
 * The system uninstall UI can block indefinitely if the user leaves it open;
 * this keeps the batch making forward progress.
 */
val BATCH_UNINSTALL_TIMEOUT: Duration = 2.minutes

/**
 * Window used to collect package add, remove and replace broadcasts into a single home refresh.
 * A store updating several apps emits them in bursts, and each refresh re-inspects tracked apps.
 */
const val PACKAGE_CHANGE_DEBOUNCE_MS = 400L

const val APK_MIMETYPE  = "application/vnd.android.package-archive"

const val PLAY_STORE_INSTALLER_PACKAGE = "com.android.vending"

const val AOSP_INSTALLER_PACKAGE        = "com.google.android.packageinstaller"
const val AOSP_INSTALLER_PACKAGE_LEGACY = "com.android.packageinstaller"
const val AOSP_INSTALLER_LABEL          = "Package installer"

// Attribution left behind by anything running as the shell user, which is how Shizuku installs
const val SHELL_INSTALLER_PACKAGE = "com.android.shell"

const val JSON_MIMETYPE     = "application/json"
const val BIN_MIMETYPE      = "application/octet-stream"
const val TEXT_MIMETYPE     = "text/plain"
const val MPP_MIMETYPE      = "application/vnd.ms-project"
const val IMAGE_MIMETYPE    = "image/*"
const val WILDCARD_MIMETYPE = "*/*"

val APK_FILE_MIME_TYPES = arrayOf(
    BIN_MIMETYPE,
    APK_MIMETYPE,
    // ApkMirror split files of "app-whatever123_apkmirror.com.apk" regularly misclassify
    // the file as an application or something incorrect. Renaming the file and
    // removing "apkmirror.com" from the file name fixes the issue, but that's not something the
    // end user will know or should have to do. Instead, show all files to ensure the user can
    // always select no matter what file naming ApkMirror uses.
    "application/*",
)

val MPP_FILE_MIME_TYPES = arrayOf(
    BIN_MIMETYPE,
    MPP_MIMETYPE,
    "*/*"
)

/** File extensions recognized as APK-family archives that Morphe can patch. */
val APK_EXTENSIONS = setOf("apk", "apks", "xapk", "apkm")
