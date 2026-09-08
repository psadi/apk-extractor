package com.psadi.apkextractor

import android.net.Uri
import com.psadi.apkextractor.data.model.AppCategory
import com.psadi.apkextractor.data.model.AppInfo
import com.psadi.apkextractor.data.model.ExtractedApk
import com.psadi.apkextractor.util.SearchUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilterTest {

    private val sampleApps = listOf(
        AppInfo(
            appName = "PS App",
            packageName = "com.scee.psxandroid",
            versionName = "26.8.0",
            versionCode = 26080002L,
            minSdkVersion = 26,
            targetSdkVersion = 35,
            apkPath = "/data/app/com.scee.psxandroid/base.apk",
            apkSize = 80_000_000L,
            isSystemApp = false,
            firstInstallTime = 1000L,
            lastUpdateTime = 2000L,
            searchAliases = SearchUtil.getSearchAliases("com.scee.psxandroid", "PS App")
        ),
        AppInfo(
            appName = "Play Store",
            packageName = "com.android.vending",
            versionName = "40.0.0",
            versionCode = 84000000L,
            minSdkVersion = 26,
            targetSdkVersion = 35,
            apkPath = "/system/priv-app/Phonesky/Phonesky.apk",
            apkSize = 50_000_000L,
            isSystemApp = true,
            firstInstallTime = 1000L,
            lastUpdateTime = 2000L,
            searchAliases = SearchUtil.getSearchAliases("com.android.vending", "Play Store")
        ),
        AppInfo(
            appName = "Calculator",
            packageName = "com.oneplus.calculator",
            versionName = "2.0.1",
            versionCode = 201L,
            minSdkVersion = 26,
            targetSdkVersion = 35,
            apkPath = "/system/app/Calculator/Calculator.apk",
            apkSize = 10_000_000L,
            isSystemApp = true,
            firstInstallTime = 1000L,
            lastUpdateTime = 2000L,
            searchAliases = emptyList()
        )
    )

    private fun filterApps(
        apps: List<AppInfo>,
        query: String,
        category: AppCategory
    ): List<AppInfo> {
        val trimmed = query.trim()
        return apps.filter { app ->
            val matchesCategory = when (category) {
                AppCategory.USER -> !app.isSystemApp
                AppCategory.SYSTEM -> app.isSystemApp
                AppCategory.EXTRACTED -> false
            }
            val matchesQuery = if (trimmed.isEmpty()) {
                true
            } else {
                app.appName.contains(trimmed, ignoreCase = true) ||
                        app.packageName.contains(trimmed, ignoreCase = true) ||
                        (app.displayBrandTag != null && app.displayBrandTag!!.contains(trimmed, ignoreCase = true)) ||
                        app.searchAliases.any { it.contains(trimmed, ignoreCase = true) }
            }
            matchesCategory && matchesQuery
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
    }

    @Test
    fun testCategoryFiltering() {
        val userApps = filterApps(sampleApps, "", AppCategory.USER)
        assertEquals(1, userApps.size)
        assertEquals("PS App", userApps[0].appName)

        val systemApps = filterApps(sampleApps, "", AppCategory.SYSTEM)
        assertEquals(2, systemApps.size)
    }

    @Test
    fun testPlaystationSearchFindsPsApp() {
        // User searches for "playstation" in User category
        val results = filterApps(sampleApps, "playstation", AppCategory.USER)
        assertEquals(1, results.size)
        assertEquals("PS App", results[0].appName)
        assertEquals("com.scee.psxandroid", results[0].packageName)
        assertEquals("PlayStation", results[0].displayBrandTag)
    }

    @Test
    fun testSonySearchFindsPsApp() {
        val results = filterApps(sampleApps, "sony", AppCategory.USER)
        assertEquals(1, results.size)
        assertEquals("PS App", results[0].appName)
    }

    @Test
    fun testPlayStoreSearch() {
        val results = filterApps(sampleApps, "google play", AppCategory.SYSTEM)
        assertEquals(1, results.size)
        assertEquals("Play Store", results[0].appName)
    }

    @Test
    fun testExtractedApkSearchWithAliases() {
        val mockUri = org.mockito.Mockito.mock(Uri::class.java)
        val extractedApk = ExtractedApk(
            fileName = "PS_App_v26.8.0.apk",
            fileUri = mockUri,
            filePath = "/storage/emulated/0/Download/APK_Extractor/PS_App_v26.8.0.apk",
            fileSize = 78_639_573L,
            lastModified = System.currentTimeMillis(),
            appName = "PS App",
            packageName = "com.scee.psxandroid",
            versionName = "26.8.0",
            versionCode = 26080002L,
            isSplitBundle = false
        )

        val query = "playstation"
        val matches = extractedApk.appName.contains(query, ignoreCase = true) ||
                extractedApk.packageName.contains(query, ignoreCase = true) ||
                extractedApk.fileName.contains(query, ignoreCase = true) ||
                (extractedApk.displayBrandTag != null && extractedApk.displayBrandTag!!.contains(query, ignoreCase = true)) ||
                SearchUtil.getSearchAliases(extractedApk.packageName, extractedApk.appName)
                    .any { it.contains(query, ignoreCase = true) }

        assertTrue("Extracted APK should match 'playstation' query", matches)
    }
}
