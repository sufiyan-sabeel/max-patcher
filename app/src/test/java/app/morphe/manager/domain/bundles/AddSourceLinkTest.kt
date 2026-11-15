/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-manager
 */

package app.morphe.manager.domain.bundles

import app.morphe.manager.domain.bundles.RemotePatchBundle.Companion.addSourceLinkForEndpoint
import app.morphe.manager.util.ADD_SOURCE_BASE_URL
import app.morphe.manager.util.ADD_SOURCE_PATH
import kotlin.test.*

/**
 * The share button hands out an add-source link, which names only the repository. It must be
 * offered only for endpoints that link recreates, or the recipient adds a different source.
 */
class AddSourceLinkTest {
    /**
     * The host is configuration, not behaviour: these tests assert how the query is built, against
     * whatever [PROJECT_WEBSITE_URL] is set to. Hardcoding a host here made five tests fail the
     * moment the project stopped being Morphe — a failure that said nothing about the code.
     *
     * MAX has no `/add-source` page yet, so a link generated today leads nowhere. The link format is
     * kept because the app still accepts these links and the page is a matter of publishing one.
     */
    private val base = ADD_SOURCE_BASE_URL + ADD_SOURCE_PATH

    @Test
    fun `github root bundle on stable branch is shared`() {
        assertEquals(
            "$base?github=crimera/piko",
            addSourceLinkForEndpoint(
                "https://raw.githubusercontent.com/crimera/piko/main/patches-bundle.json",
                name = null
            )
        )
    }

    @Test
    fun `github root bundle on dev branch is shared`() {
        assertEquals(
            "$base?github=hoo-dles/morphe-patches",
            addSourceLinkForEndpoint(
                "https://raw.githubusercontent.com/hoo-dles/morphe-patches/dev/patches-bundle.json",
                name = null
            )
        )
    }

    @Test
    fun `gitlab root bundle is shared`() {
        assertEquals(
            "$base?gitlab=owner/repo",
            addSourceLinkForEndpoint(
                "https://gitlab.com/owner/repo/-/raw/main/patches-bundle.json",
                name = null
            )
        )
    }

    @Test
    fun `name is encoded as a query value`() {
        assertEquals(
            "$base?github=owner/repo&name=Piko%20%26%20Co",
            addSourceLinkForEndpoint(
                "https://raw.githubusercontent.com/owner/repo/main/patches-bundle.json",
                name = "Piko & Co"
            )
        )
    }

    @Test
    fun `blank name is left out`() {
        assertEquals(
            "$base?github=owner/repo",
            addSourceLinkForEndpoint(
                "https://raw.githubusercontent.com/owner/repo/main/patches-bundle.json",
                name = " "
            )
        )
    }

    @Test
    fun `custom bundle file is not shared`() {
        assertNull(
            addSourceLinkForEndpoint(
                "https://raw.githubusercontent.com/owner/repo/main/piko-latest-patches-bundle.json",
                name = null
            )
        )
    }

    @Test
    fun `bundle in a subfolder is not shared`() {
        assertNull(
            addSourceLinkForEndpoint(
                "https://raw.githubusercontent.com/owner/repo/main/bundles/patches-bundle.json",
                name = null
            )
        )
    }

    @Test
    fun `other branch is not shared`() {
        assertNull(
            addSourceLinkForEndpoint(
                "https://gitlab.com/owner/repo/-/raw/beta/patches-bundle.json",
                name = null
            )
        )
    }

    @Test
    fun `direct release asset is not shared`() {
        assertNull(
            addSourceLinkForEndpoint(
                "https://github.com/crimera/piko/releases/download/v3.9.0/patches-3.9.0.mpp",
                name = null
            )
        )
    }

    @Test
    fun `unknown host is not shared`() {
        assertNull(addSourceLinkForEndpoint("https://example.com/patches-bundle.json", name = null))
    }

    @Test
    fun `malformed endpoint is not shared`() {
        assertNull(addSourceLinkForEndpoint("not a url", name = null))
    }
}
