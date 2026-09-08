package com.psadi.apkextractor.data.model

import android.net.Uri
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExtractedApk(
    val fileName: String,
    val fileUri: Uri,
    val filePath: String?,
    val fileSize: Long,
    val lastModified: Long,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSplitBundle: Boolean,
    val isDirectory: Boolean = false
) {
    val displayBrandTag: String? get() = com.psadi.apkextractor.util.SearchUtil.getBrandTag(packageName, appName)

    val formattedSize: String
        get() {
            if (fileSize <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(fileSize.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val format = DecimalFormat("#,##0.#")
            return "${format.format(fileSize / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }
}
