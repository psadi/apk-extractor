package com.psadi.apkextractor.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import com.psadi.apkextractor.data.model.ExtractedApk

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

    fun installApk(context: Context, uri: Uri, appName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        context,
                        "Please allow APK Extractor to install apps in system settings",
                        Toast.LENGTH_LONG
                    ).show()
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot launch installer: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun installAppPackage(context: Context, appInfo: com.psadi.apkextractor.data.model.AppInfo) {
        if (appInfo.isSplitApk) {
            installSplitApks(context, appInfo)
        } else {
            val file = java.io.File(appInfo.apkPath)
            if (!file.exists()) {
                Toast.makeText(context, "Source APK not found", Toast.LENGTH_SHORT).show()
                return
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            installApk(context, uri, appInfo.appName)
        }
    }

    fun installSplitApks(context: Context, appInfo: com.psadi.apkextractor.data.model.AppInfo) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        context,
                        "Please allow APK Extractor to install apps in system settings",
                        Toast.LENGTH_LONG
                    ).show()
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val packageInstaller = context.packageManager.packageInstaller
            val params = android.content.pm.PackageInstaller.SessionParams(
                android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.setRequireUserAction(android.content.pm.PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            val baseFile = java.io.File(appInfo.apkPath)
            java.io.FileInputStream(baseFile).use { input ->
                session.openWrite("base.apk", 0, baseFile.length()).use { output ->
                    input.copyTo(output)
                }
            }

            appInfo.splitApkPaths.forEachIndexed { index, splitPath ->
                val splitFile = java.io.File(splitPath)
                if (splitFile.exists()) {
                    java.io.FileInputStream(splitFile).use { input ->
                        session.openWrite("split_$index.apk", 0, splitFile.length()).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }

            val intent = Intent(context, InstallReceiver::class.java).apply {
                action = "com.psadi.apkextractor.INSTALL_COMPLETE"
                putExtra("appName", appInfo.appName)
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )

            session.commit(pendingIntent.intentSender)
            session.close()

            Toast.makeText(context, "Initiating package install for ${appInfo.appName}…", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Install error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun installExtractedApk(context: Context, item: ExtractedApk) {
        if (item.isSplitBundle) {
            installExtractedSplitBundle(context, item)
        } else {
            val uri = if (item.filePath != null) {
                val file = java.io.File(item.filePath)
                try {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } catch (e: Exception) {
                    item.fileUri
                }
            } else {
                item.fileUri
            }
            installApk(context, uri, item.appName)
        }
    }

    private fun installExtractedSplitBundle(context: Context, item: ExtractedApk) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        context,
                        "Please allow APK Extractor to install apps in system settings",
                        Toast.LENGTH_LONG
                    ).show()
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val packageInstaller = context.packageManager.packageInstaller
            val params = android.content.pm.PackageInstaller.SessionParams(
                android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.setRequireUserAction(android.content.pm.PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            if (item.isDirectory && item.filePath != null) {
                val dir = java.io.File(item.filePath)
                val apkFiles = dir.listFiles { f -> f.isFile && f.name.endsWith(".apk") } ?: emptyArray()
                apkFiles.forEachIndexed { index, apkFile ->
                    java.io.FileInputStream(apkFile).use { input ->
                        session.openWrite("split_$index.apk", 0, apkFile.length()).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } else if (item.filePath != null) {
                val zipFile = java.util.zip.ZipFile(java.io.File(item.filePath))
                val entries = zipFile.entries()
                var idx = 0
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && entry.name.endsWith(".apk")) {
                        val entryName = "split_${idx++}.apk"
                        zipFile.getInputStream(entry).use { input ->
                            session.openWrite(entryName, 0, entry.size).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
                zipFile.close()
            }

            val intent = Intent(context, InstallReceiver::class.java).apply {
                action = "com.psadi.apkextractor.INSTALL_COMPLETE"
                putExtra("appName", item.appName)
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )

            session.commit(pendingIntent.intentSender)
            session.close()

            Toast.makeText(context, "Initiating package install for ${item.appName}…", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Install error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
