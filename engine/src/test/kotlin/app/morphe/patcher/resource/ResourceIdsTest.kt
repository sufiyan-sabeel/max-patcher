/*
 * Copyright 2026 MAX Patcher contributors.
 * Licensed under the GNU General Public License v3.0, see LICENSE.
 */

package app.morphe.patcher.resource

import app.morphe.patcher.patch.PatchException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the resource lookup contract that the YouTube 21.45.82 failure turns on.
 *
 * `resourceLiteral(...)` resolves a name through [ResourceIds] and is fatal by default, which is why
 * a drawable that a target APK no longer ships aborts the patch that mentions it. These tests fix
 * both halves of that behaviour in place: the fatal path, and the non-fatal one a version-aware
 * fingerprint is expected to use.
 */
class ResourceIdsTest {
    private val ids = ResourceIds {
        mapOf(
            "drawable/known_drawable" to 0x7f080001L,
            "id/toolbar_container" to 0x7f0b0002L,
            "string/known_string" to 0x7f130003L,
        )
    }

    @Test
    fun `looks up a resource that the APK ships`() {
        assertEquals(0x7f080001L, ids[ResourceType.DRAWABLE, "known_drawable"])
        assertEquals(0x7f0b0002L, ids[ResourceType.ID, "toolbar_container"])
    }

    @Test
    fun `has is false for a resource the APK does not ship`() {
        assertFalse(ids.has(ResourceType.DRAWABLE, "yt_fill_bell_black_24"))
    }

    @Test
    fun `has is true for a resource the APK ships`() {
        assertTrue(ids.has(ResourceType.STRING, "known_string"))
    }

    @Test
    fun `getOrNull returns null instead of throwing`() {
        assertNull(ids.getOrNull(ResourceType.DRAWABLE, "yt_fill_bell_black_24"))
    }

    @Test
    fun `the name is looked up together with its type`() {
        // Same name, different type: absent, because the key is "<type>/<name>".
        assertNull(ids.getOrNull(ResourceType.STRING, "known_drawable"))
    }

    @Test
    fun `a missing resource is fatal and reports the exact message patches surface`() {
        val failure = assertFailsWith<PatchException> {
            ids[ResourceType.DRAWABLE, "yt_fill_bell_black_24"]
        }
        assertEquals(
            "Could not find resource type: DRAWABLE name: yt_fill_bell_black_24",
            failure.message,
        )
    }
}
