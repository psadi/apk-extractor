package com.psadi.apkextractor.data.`package`

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.psadi.apkextractor.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppPackageScanner(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageInfos = getInstalledPackageInfos()
        val apps = ArrayList<AppInfo>(packageInfos.size)

        for (pkg in packageInfos) {
            val appInfo = pkg.applicationInfo ?: continue
            val apkPath = appInfo.publicSourceDir ?: appInfo.sourceDir ?: continue
            val apkFile = File(apkPath)
            if (!apkFile.exists()) continue

            val appName = try {
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg.packageName
            }

            val versionName = pkg.versionName ?: "1.0"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }

            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            val minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                appInfo.minSdkVersion
            } else {
                26
            }

            val splitApkPaths = (appInfo.splitPublicSourceDirs ?: appInfo.splitSourceDirs)
                ?.filter { !it.isNullOrBlank() && File(it).exists() }
                ?: emptyList()

            val totalSize = apkFile.length() + splitApkPaths.sumOf { File(it).length() }

            apps.add(
                AppInfo(
                    appName = appName,
                    packageName = pkg.packageName,
                    versionName = versionName,
                    versionCode = versionCode,
                    minSdkVersion = minSdkVersion,
                    targetSdkVersion = appInfo.targetSdkVersion,
                    apkPath = apkPath,
                    apkSize = totalSize,
                    isSystemApp = isSystemApp,
                    firstInstallTime = pkg.firstInstallTime,
                    lastUpdateTime = pkg.lastUpdateTime,
                    splitApkPaths = splitApkPaths
                )
            )
        }

        // Sort alphabetically by app name (case-insensitive)
        apps.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    private fun getInstalledPackageInfos(): List<PackageInfo> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(0)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
