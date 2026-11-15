/*
 * Copyright 2026 MAX Patcher contributors.
 * Licensed under the GNU General Public License v3.0, see LICENSE.
 */

package app.morphe.patcher

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins the version semantics MAX Engine relies on to stay compatible with patch bundles built for
 * upstream Morphe Patcher 2.3.0.
 *
 * A bundle declares `Patcher-Version: 2.3.0` and the manager refuses a bundle that needs a newer
 * patcher than the one it carries. MAX Engine continues the upstream line at 2.3.1 rather than
 * restarting at 0.1.0, so that comparison must hold — and it must keep holding as MAX increments.
 */
class EngineVersionCompatibilityTest {
    private val engineVersion = PatcherVersion.parse(System.getProperty("maxEngineVersion"))

    /** The newest patcher version any inherited bundle declares. */
    private val newestInheritedRequirement = PatcherVersion.parse("2.3.0")

    @Test
    fun `the build passes the engine version to the tests`() {
        assertNotNull(engineVersion, "maxEngineVersion system property missing or unparsable")
    }

    @Test
    fun `this engine satisfies a bundle that requires 2_3_0`() {
        val current = assertNotNull(engineVersion)
        val required = assertNotNull(newestInheritedRequirement)
        assertTrue(
            current >= required,
            "engine $current must not be older than the 2.3.0 that inherited bundles require",
        )
    }

    @Test
    fun `a release sorts above its own pre-releases`() {
        assertTrue(PatcherVersion.parse("2.3.0-dev.5")!! < PatcherVersion.parse("2.3.0")!!)
        assertTrue(PatcherVersion.parse("2.3.1")!! > PatcherVersion.parse("2.3.0")!!)
    }

    @Test
    fun `a newer requirement is still refused`() {
        // The guard must keep working: a bundle built for a future engine is not silently accepted.
        assertTrue(PatcherVersion.parse("2.4.0")!! > assertNotNull(engineVersion))
    }

    @Test
    fun `build metadata does not affect ordering`() {
        assertEquals(
            PatcherVersion.parse("2.3.1"),
            PatcherVersion.parse("2.3.1+build.7"),
        )
    }
}
