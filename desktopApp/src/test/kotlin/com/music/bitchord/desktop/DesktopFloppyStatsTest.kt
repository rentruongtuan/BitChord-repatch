package com.music.bitchord.desktop

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopFloppyStatsTest {

    @Test
    fun testFetchOverviewFromFloppy() {
        val summary = DesktopFloppyStats.summary(DesktopReplayPeriod.ALL_TIME)
        if (summary != null) {
            println("Floppy Total Minutes: ${summary.minutes}")
            println("Floppy Total Plays: ${summary.totalPlays}")
            println("Floppy Songs count: ${summary.songs.size}")
            println("Floppy Artists count: ${summary.artists.size}")
            assertTrue(summary.minutes >= 0)
            assertTrue(summary.songs.isNotEmpty())
        } else {
            println("Cannot connect to Floppy (offline or firewall)")
        }
    }
}
