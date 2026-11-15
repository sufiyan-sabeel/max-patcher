/*
 * Copyright 2026 MAX Patcher contributors.
 * Licensed under the GNU General Public License v3.0, see LICENSE.
 */

package app.morphe.manager.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The manager's half of the engine-version contract.
 *
 * `isPatcherOutdated` decides whether a patch source is usable. MAX Engine carries version 2.3.1
 * (see engine/gradle.properties and engine/docs/MAX_ENGINE.md), so the inherited bundles that
 * require 2.3.0 must be accepted, and anything newer must still be refused.
 */
class PatcherVersionCompatibilityTest {
    private val engine = "2.3.1"

    @Test
    fun `inherited bundles requiring 2_3_0 are accepted`() {
        assertFalse(isPatcherOutdated(required = "2.3.0", current = engine))
    }

    @Test
    fun `a bundle requiring the same version is accepted`() {
        assertFalse(isPatcherOutdated(required = "2.3.1", current = engine))
    }

    @Test
    fun `a bundle requiring a newer engine is refused`() {
        assertTrue(isPatcherOutdated(required = "2.4.0", current = engine))
        assertTrue(isPatcherOutdated(required = "2.3.2", current = engine))
    }

    @Test
    fun `an older requirement is accepted`() {
        assertFalse(isPatcherOutdated(required = "2.2.0", current = engine))
        assertFalse(isPatcherOutdated(required = "1.0.0", current = engine))
    }

    @Test
    fun `a dev requirement below the release is accepted`() {
        assertFalse(isPatcherOutdated(required = "2.3.0-dev.5", current = engine))
    }

    @Test
    fun `version strings from tags and metadata compare the same`() {
        assertFalse(isPatcherOutdated(required = "v2.3.0", current = engine))
    }
}
