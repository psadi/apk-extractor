package com.psadi.apkextractor

import com.psadi.apkextractor.data.model.AppInfo
import com.psadi.apkextractor.data.model.ExtractedApk
import com.psadi.apkextractor.util.SearchUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUtilTest {

    @Test
    fun testGenericCamelCaseSplitting() {
        val aliases = SearchUtil.getSearchAliases("com.example.apkextractor", "APKExtractor")
        assertTrue("Aliases must contain 'apk extractor'", aliases.contains("apk extractor"))
        assertTrue("Aliases must contain 'apk'", aliases.contains("apk"))
        assertTrue("Aliases must contain 'extractor'", aliases.contains("extractor"))
    }

    @Test
    fun testGenericDelimiterSplittingAndAcronyms() {
        val aliases = SearchUtil.getSearchAliases("com.example.gamedemo", "Call of Duty")
        assertTrue("Aliases must contain 'call'", aliases.contains("call"))
        assertTrue("Aliases must contain 'duty'", aliases.contains("duty"))
        assertTrue("Aliases must contain acronym 'cod'", aliases.contains("cod"))
    }

    @Test
    fun testPackageSegmentExtraction() {
        val aliases = SearchUtil.getSearchAliases("org.mozilla.firefox", "Browser")
        assertTrue("Aliases must contain 'mozilla'", aliases.contains("mozilla"))
        assertTrue("Aliases must contain 'firefox'", aliases.contains("firefox"))
    }

    @Test
    fun testGenericQueryMatchingNormalized() {
        val app = AppInfo(
            appName = "My Files Manager",
            packageName = "com.sample.files.manager",
            versionName = "1.0.0",
            versionCode = 100L,
            minSdkVersion = 26,
            targetSdkVersion = 35,
            apkPath = "/data/app/com.sample.files.manager/base.apk",
            apkSize = 15_000_000L,
            isSystemApp = false,
            firstInstallTime = 1000L,
            lastUpdateTime = 2000L,
            searchAliases = SearchUtil.getSearchAliases("com.sample.files.manager", "My Files Manager")
        )

        // Matching without spaces
        assertTrue(SearchUtil.matchesApp(app, "myfiles"))
        assertTrue(SearchUtil.matchesApp(app, "filesmanager"))
        // Matching acronym
        assertTrue(SearchUtil.matchesApp(app, "mfm"))
        // Multi-word matching
        assertTrue(SearchUtil.matchesApp(app, "files manager"))
        // Partial package matching
        assertTrue(SearchUtil.matchesApp(app, "sample"))
    }
}
