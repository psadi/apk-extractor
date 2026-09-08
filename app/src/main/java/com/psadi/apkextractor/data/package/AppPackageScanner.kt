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
            val appInfo = pkg.applicationInfo ?: try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(pkg.packageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getApplicationInfo(pkg.packageName, 0)
                }
            } catch (_: Exception) {
                null
            } ?: continue

            val apkPath = appInfo.publicSourceDir ?: appInfo.sourceDir ?: continue
            val apkFile = File(apkPath)
            if (!apkFile.exists()) continue

            apps.add(createAppInfo(pkg, appInfo, apkPath, apkFile))
        }

        // Sort alphabetically by app name (case-insensitive)
        apps.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
    }

    private fun createAppInfo(
        pkg: PackageInfo?,
        appInfo: ApplicationInfo,
        apkPath: String,
        apkFile: File
    ): AppInfo {
        val packageName = pkg?.packageName ?: appInfo.packageName
        val appName = try {
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        val versionName = pkg?.versionName ?: "1.0"
        val versionCode = if (pkg != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }
        } else 0L

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

        val searchAliases = com.psadi.apkextractor.util.SearchUtil.getSearchAliases(packageName, appName)

        return AppInfo(
            appName = appName,
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            minSdkVersion = minSdkVersion,
            targetSdkVersion = appInfo.targetSdkVersion,
            apkPath = apkPath,
            apkSize = totalSize,
            isSystemApp = isSystemApp,
            firstInstallTime = pkg?.firstInstallTime ?: 0L,
            lastUpdateTime = pkg?.lastUpdateTime ?: 0L,
            splitApkPaths = splitApkPaths,
            searchAliases = searchAliases
        )
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
