/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-manager
 */

package app.morphe.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import app.morphe.manager.ui.screen.shared.autoMirrored
import app.morphe.manager.util.COMMUNITY_PATCHES_URL
import app.morphe.manager.util.PROJECT_WEBSITE_URL
import com.composables.icons.fontawesome.FontAwesome
import com.composables.icons.fontawesome.brands.*
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*

data class SocialLink(
    val name: String,
    val url: String,
    val preferred: Boolean = false,
)

class AboutViewModel : ViewModel() {
    companion object {
        val socials: List<SocialLink> = listOf(
            SocialLink(
                name = "Website",
                url = PROJECT_WEBSITE_URL,
                preferred = true
            ),
            SocialLink(
                name = "Releases",
                url = "$PROJECT_WEBSITE_URL/releases"
            ),
            SocialLink(
                name = "GitHub",
                url = "https://github.com/sufiyan-sabeel"
            ),
            // Documentation, not a subreddit or a Crowdin project: MAX has neither yet, and pointing
            // at Morphe's would send MAX's users to another project's community.
            SocialLink(
                name = "Documentation",
                url = COMMUNITY_PATCHES_URL
            )
        )

        private val socialIcons = mapOf(
            "Website" to MaterialSymbols.Rounded.Public,
            "GitHub" to FontAwesome.Brands.Github,
            "Releases" to MaterialSymbols.Rounded.Article.autoMirrored,
            "Documentation" to MaterialSymbols.Rounded.Language,
        )

        fun getSocialIcon(name: String) = socialIcons[name] ?: MaterialSymbols.Rounded.Language
    }
}
