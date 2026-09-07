package com.psadi.apkextractor.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast

object IntentUtil {

    fun shareApk(context: Context, uri: Uri, appName: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "APK for $appName")
                putExtra(Intent.EXTRA_TEXT, "Here is the APK file for $appName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share $appName APK").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun openAppInfo(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open app settings: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openFolder(context: Context, folderUri: Uri?) {
        try {
            if (folderUri != null && folderUri.scheme == "content" && folderUri.authority?.contains("documents") == true) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(folderUri, DocumentsContract.Document.MIME_TYPE_DIR)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }

            // Fallback for downloads
            val downloadIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(downloadIntent)
        } catch (e: Exception) {
            // Generic file manager intent fallback
            try {
                val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(folderUri ?: Uri.parse("content://media/external/file"), "*/*")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(genericIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "No file manager found to open folder", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
