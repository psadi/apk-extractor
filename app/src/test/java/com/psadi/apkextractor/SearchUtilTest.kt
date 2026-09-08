package com.psadi.apkextractor

import com.psadi.apkextractor.util.SearchUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUtilTest {

    @Test
    fun testPlayStationAliasesForSonyApp() {
        val aliases = SearchUtil.getSearchAliases("com.scee.psxandroid", "PS App")
        
        assertTrue("Aliases must contain 'playstation'", aliases.any { it.contains("playstation", ignoreCase = true) })
        assertTrue("Aliases must contain 'sony'", aliases.any { it.contains("sony", ignoreCase = true) })
        assertTrue("Aliases must contain 'psn'", aliases.any { it.contains("psn", ignoreCase = true) })
        assertTrue("Aliases must contain 'ps app'", aliases.any { it.contains("ps app", ignoreCase = true) })
    }

    @Test
    fun testPlayStationBrandTag() {
        val brandTag = SearchUtil.getBrandTag("com.scee.psxandroid", "PS App")
        assertNotNull("Brand tag should not be null for PS App", brandTag)
        assertEquals("PlayStation", brandTag)
    }

    @Test
    fun testGooglePlayStoreAliases() {
        val aliases = SearchUtil.getSearchAliases("com.android.vending", "Play Store")
        assertTrue("Aliases must contain 'google play store'", aliases.contains("google play store"))
        assertTrue("Aliases must contain 'market'", aliases.contains("market"))
        assertEquals("Play Store", SearchUtil.getBrandTag("com.android.vending", "Play Store"))
    }

    @Test
    fun testTwitterXAliases() {
        val aliasesX = SearchUtil.getSearchAliases("com.x.android", "X")
        assertTrue("Aliases for X must contain 'twitter'", aliasesX.contains("twitter"))

        val aliasesTwitter = SearchUtil.getSearchAliases("com.twitter.android", "Twitter")
        assertTrue("Aliases for Twitter must contain 'x'", aliasesTwitter.contains("x"))
    }

    @Test
    fun testQueryMatchingWithAliases() {
        val queries = listOf("playstation", "PlayStation", "PLAYSTATION", "sony", "ps", "ps app", "psn")
        val aliases = SearchUtil.getSearchAliases("com.scee.psxandroid", "PS App")
        
        for (q in queries) {
            val matched = "PS App".contains(q, ignoreCase = true) ||
                    "com.scee.psxandroid".contains(q, ignoreCase = true) ||
                    aliases.any { it.contains(q, ignoreCase = true) }
            assertTrue("Query '$q' should match PS App / com.scee.psxandroid", matched)
        }
    }
}
