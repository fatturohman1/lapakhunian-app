package com.lapakhunian.app

import com.lapakhunian.app.core.util.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class UpdateNotesTest {

    @Test
    fun releaseNotesStripMarkdownAndPullRequestAttribution() {
        val body = """
            ## What's Changed
            * fix login by @resa in https://github.com/fatturohman1/lapakhunian-app/pull/1
            * add zoom by @resa in https://github.com/fatturohman1/lapakhunian-app/pull/2
            **Full Changelog**: https://github.com/fatturohman1/lapakhunian-app/compare/v1...v2
        """.trimIndent()
        assertEquals("• fix login\n• add zoom", UpdateChecker.cleanReleaseNotes(body))
    }

    @Test
    fun releaseNotesCapAtEightLines() {
        val body = (1..12).joinToString("\n") { "* perubahan $it" }
        assertEquals(8, UpdateChecker.cleanReleaseNotes(body).lines().size)
    }

    @Test
    fun blankBodyProducesEmptyNotes() {
        assertEquals("", UpdateChecker.cleanReleaseNotes(""))
        assertEquals("", UpdateChecker.cleanReleaseNotes("## What's Changed\n**Full Changelog**: https://x/y"))
    }

    @Test
    fun parseReleaseReturnsVersionAndNotesWhenNewer() {
        val body = """{"tag_name":"v1.0.0-beta.6","body":"* perbaikan galeri"}"""
        val info = UpdateChecker.parseRelease(body, "1.0.0-beta.5")
        assertEquals("1.0.0-beta.6", info?.version)
        assertEquals("• perbaikan galeri", info?.notes)
    }

    @Test
    fun parseReleaseReturnsNullWhenNotNewer() {
        assertNull(UpdateChecker.parseRelease("""{"tag_name":"v1.0.0-beta.5"}""", "1.0.0-beta.5"))
    }

    @Test
    fun parseReleaseWithoutTagFails() {
        try {
            UpdateChecker.parseRelease("""{"body":"x"}""", "1.0.0-beta.5")
            fail("Tag kosong harus throw")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun missingBodyStillYieldsUpdateWithEmptyNotes() {
        val info = UpdateChecker.parseRelease("""{"tag_name":"v9.0.0"}""", "1.0.0-beta.5")
        assertTrue(info != null)
        assertEquals("", info?.notes)
    }
}