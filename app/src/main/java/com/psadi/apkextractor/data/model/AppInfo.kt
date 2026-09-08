package com.psadi.apkextractor.data.model

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val apkPath: String,
    val apkSize: Long,
    val isSystemApp: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val splitApkPaths: List<String> = emptyList(),
    val searchAliases: List<String> = emptyList()
) {
    val displayBrandTag: String? get() = com.psadi.apkextractor.util.SearchUtil.getBrandTag(packageName, appName)
    val isSplitApk: Boolean get() = splitApkPaths.isNotEmpty()
    val totalSplitCount: Int get() = if (isSplitApk) splitApkPaths.size + 1 else 1

    val formattedSize: String
        get() {
            if (apkSize <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(apkSize.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val format = DecimalFormat("#,##0.#")
            return "${format.format(apkSize / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
        }

    val formattedInstallDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return sdf.format(Date(firstInstallTime))
        }

    val sanitizedApkFileName: String
        get() {
            val cleanName = appName.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            val cleanVersion = versionName.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            return "${cleanName}_v${cleanVersion}.apk"
        }

    val sanitizedBundleFileName: String
        get() {
            val cleanName = appName.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            val cleanVersion = versionName.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            return if (isSplitApk) "${cleanName}_v${cleanVersion}.apks" else "${cleanName}_v${cleanVersion}.apk"
        }
}
