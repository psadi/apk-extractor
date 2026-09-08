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
            appName = "Document Scanner",
            packageName = "com.example.docscanner",
            versionName = "2.1.0",
            versionCode = 210L,
            minSdkVersion = 26,
            targetSdkVersion = 35,
            apkPath = "/data/app/com.example.docscanner/base.apk",
            apkSize = 45_000_000L,
            isSystemApp = false,
            isUpdatedSystemApp = false,
            firstInstallTime = 1000L,
            lastUpdateTime = 2000L,
            searchAliases = SearchUtil.getSearchAliases("com.example.docscanner", "Document Scanner")
        ),
        AppInfo(
            appName = "System Settings",
            packageName = "com.android.settings",
            versionName = "15.0.0",
            versionCode = 1500L,
            minSdkVersion = 26,
            targetSdkVersion = 35,
            apkPath = "/system/priv-app/Settings/Settings.apk",
            apkSize = 30_000_000L,
            isSystemApp = true,
            isUpdatedSystemApp = false,
            firstInstallTime = 1000L,
            lastUpdateTime = 2000L,
            searchAliases = SearchUtil.getSearchAliases("com.android.settings", "System Settings")
        ),
        AppInfo(
            appName = "Camera Pro",
            packageName = "com.oem.camera",
            versionName = "5.0.1",
            versionCode = 501L,
            minSdkVersion = 26,
            targetSdkVersion = 35,
            apkPath = "/data/app/com.oem.camera/base.apk",
            apkSize = 65_000_000L,
            // Preinstalled app updated by user via store -> treated as user app
            isSystemApp = false,
            isUpdatedSystemApp = true,
            firstInstallTime = 1000L,
            lastUpdateTime = 3000L,
            searchAliases = SearchUtil.getSearchAliases("com.oem.camera", "Camera Pro")
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
                AppCategory.ALL -> true
                AppCategory.USER -> !app.isSystemApp
                AppCategory.SYSTEM -> app.isSystemApp
                AppCategory.EXTRACTED -> false
            }
            val matchesQuery = SearchUtil.matchesApp(app, trimmed)
            matchesCategory && matchesQuery
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
    }

    @Test
    fun testCategoryFilteringWithUpdatedSystemApp() {
        val allApps = filterApps(sampleApps, "", AppCategory.ALL)
        assertEquals(3, allApps.size)

        // User tab should include both standard user apps and updated system apps
        val userApps = filterApps(sampleApps, "", AppCategory.USER)
        assertEquals(2, userApps.size)
        assertTrue(userApps.any { it.appName == "Document Scanner" })
        assertTrue(userApps.any { it.appName == "Camera Pro" })

        // System tab should only contain pure system apps
        val systemApps = filterApps(sampleApps, "", AppCategory.SYSTEM)
        assertEquals(1, systemApps.size)
        assertEquals("System Settings", systemApps[0].appName)
    }

    @Test
    fun testAllCategorySearch() {
        val results = filterApps(sampleApps, "pro", AppCategory.ALL)
        assertEquals(1, results.size)
        assertEquals("Camera Pro", results[0].appName)
    }

    @Test
    fun testSearchByPackageSegment() {
        val results = filterApps(sampleApps, "docscanner", AppCategory.ALL)
        assertEquals(1, results.size)
        assertEquals("Document Scanner", results[0].appName)
    }

    @Test
    fun testSearchByAcronym() {
        val results = filterApps(sampleApps, "ds", AppCategory.ALL)
        assertEquals(1, results.size)
        assertEquals("Document Scanner", results[0].appName)
    }

    @Test
    fun testExtractedApkGenericSearch() {
        val mockUri = org.mockito.Mockito.mock(Uri::class.java)
        val extractedApk = ExtractedApk(
            fileName = "Document_Scanner_v2.1.0.apk",
            fileUri = mockUri,
            filePath = "/storage/emulated/0/Download/APK_Extractor/Document_Scanner_v2.1.0.apk",
            fileSize = 45_000_000L,
            lastModified = System.currentTimeMillis(),
            appName = "Document Scanner",
            packageName = "com.example.docscanner",
            versionName = "2.1.0",
            versionCode = 210L,
            isSplitBundle = false
        )

        assertTrue(SearchUtil.matchesExtracted(extractedApk, "document"))
        assertTrue(SearchUtil.matchesExtracted(extractedApk, "scanner"))
        assertTrue(SearchUtil.matchesExtracted(extractedApk, "docscanner"))
        assertTrue(SearchUtil.matchesExtracted(extractedApk, "ds"))
    }
}
