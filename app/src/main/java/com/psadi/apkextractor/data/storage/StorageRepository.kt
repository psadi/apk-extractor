package com.psadi.apkextractor.data.storage

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.psadi.apkextractor.data.model.AppInfo
import com.psadi.apkextractor.data.model.ExtractedApk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExtractedApkResult(
    val fileName: String,
    val destinationDisplayName: String,
    val shareableUri: Uri,
    val folderUri: Uri?
)

class StorageRepository(private val context: Context) {

    companion object {
        const val DEFAULT_FOLDER_NAME = "APK_Extractor"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }

    suspend fun extractApk(
        appInfo: AppInfo,
        customFolderUri: Uri?,
        onProgress: (Float) -> Unit = {}
    ): Result<ExtractedApkResult> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(appInfo.apkPath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("Source APK file not found at ${appInfo.apkPath}"))
            }

            val fileName = appInfo.sanitizedBundleFileName
            val isSplit = appInfo.isSplitApk
            val mimeType = if (isSplit) "application/zip" else APK_MIME_TYPE

            if (customFolderUri != null) {
                extractToSafTree(appInfo, fileName, mimeType, customFolderUri, onProgress)
            } else {
                extractToDefaultDownloads(appInfo, fileName, mimeType, onProgress)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkApkAlreadyExists(
        appInfo: AppInfo,
        customFolderUri: Uri?
    ): Boolean = withContext(Dispatchers.IO) {
        val fileName = appInfo.sanitizedBundleFileName
        if (customFolderUri != null) {
            val treeDoc = DocumentFile.fromTreeUri(context, customFolderUri)
            treeDoc?.findFile(fileName) != null
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(fileName, "%${DEFAULT_FOLDER_NAME}%")
                resolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.MediaColumns._ID),
                    selection, selectionArgs, null
                )?.use { cursor -> cursor.count > 0 } ?: false
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(File(downloadsDir, DEFAULT_FOLDER_NAME), fileName)
                targetFile.exists()
            }
        }
    }

    private fun extractToSafTree(
        appInfo: AppInfo,
        fileName: String,
        mimeType: String,
        treeUri: Uri,
        onProgress: (Float) -> Unit
    ): Result<ExtractedApkResult> {
        val treeDoc = DocumentFile.fromTreeUri(context, treeUri)
            ?: return Result.failure(Exception("Cannot access destination folder"))

        // Overwrite existing bundle file if present
        treeDoc.findFile(fileName)?.delete()

        val createdFile = treeDoc.createFile(mimeType, fileName)
            ?: return Result.failure(Exception("Failed to create file in destination folder"))

        val outputStream = context.contentResolver.openOutputStream(createdFile.uri)
            ?: return Result.failure(Exception("Failed to open output stream"))

        val sourceFile = File(appInfo.apkPath)
        val isSplit = appInfo.isSplitApk

        outputStream.use { output ->
            if (isSplit) {
                writeSplitApksBundle(appInfo, output, onProgress)
            } else {
                FileInputStream(sourceFile).use { input ->
                    copyStreamWithProgress(input, output, sourceFile.length(), onProgress)
                }
            }
        }

        val shareableUri = if (isSplit) {
            createShareableSplitBundleUri(appInfo, fileName)
        } else {
            createShareableCacheUri(sourceFile, fileName)
        }

        return Result.success(
            ExtractedApkResult(
                fileName = fileName,
                destinationDisplayName = treeDoc.name ?: "Custom Folder",
                shareableUri = shareableUri,
                folderUri = treeUri
            )
        )
    }

    private fun extractToDefaultDownloads(
        appInfo: AppInfo,
        fileName: String,
        mimeType: String,
        onProgress: (Float) -> Unit
    ): Result<ExtractedApkResult> {
        val sourceFile = File(appInfo.apkPath)
        val isSplit = appInfo.isSplitApk

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DEFAULT_FOLDER_NAME")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val collectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val itemUri = resolver.insert(collectionUri, contentValues)
                ?: return Result.failure(Exception("Unable to create MediaStore entry in Downloads"))

            try {
                resolver.openOutputStream(itemUri)?.use { output ->
                    if (isSplit) {
                        writeSplitApksBundle(appInfo, output, onProgress)
                    } else {
                        FileInputStream(sourceFile).use { input ->
                            copyStreamWithProgress(input, output, sourceFile.length(), onProgress)
                        }
                    }
                } ?: return Result.failure(Exception("Unable to open output stream for MediaStore entry"))

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)

                val shareableUri = if (isSplit) {
                    createShareableSplitBundleUri(appInfo, fileName)
                } else {
                    createShareableCacheUri(sourceFile, fileName)
                }

                Result.success(
                    ExtractedApkResult(
                        fileName = fileName,
                        destinationDisplayName = "Downloads/$DEFAULT_FOLDER_NAME",
                        shareableUri = shareableUri,
                        folderUri = collectionUri
                    )
                )
            } catch (e: Exception) {
                resolver.delete(itemUri, null, null)
                Result.failure(e)
            }
        } else {
            // Legacy external storage directory for Android 8.0 - 9.0
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = File(downloadsDir, DEFAULT_FOLDER_NAME).apply { mkdirs() }
            val targetFile = File(targetDir, fileName)

            if (isSplit) {
                FileOutputStream(targetFile).use { output ->
                    writeSplitApksBundle(appInfo, output, onProgress)
                }
            } else {
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        copyStreamWithProgress(input, output, sourceFile.length(), onProgress)
                    }
                }
            }

            val shareableUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                )
            } catch (e: Exception) {
                if (isSplit) createShareableSplitBundleUri(appInfo, fileName)
                else createShareableCacheUri(sourceFile, fileName)
            }

            Result.success(
                ExtractedApkResult(
                    fileName = fileName,
                    destinationDisplayName = "Downloads/$DEFAULT_FOLDER_NAME",
                    shareableUri = shareableUri,
                    folderUri = Uri.fromFile(targetDir)
                )
            )
        }
    }

    suspend fun getShareableUriForApp(appInfo: AppInfo): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(appInfo.apkPath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("APK file not found"))
            }
            val fileName = appInfo.sanitizedBundleFileName
            val uri = if (appInfo.isSplitApk) {
                createShareableSplitBundleUri(appInfo, fileName)
            } else {
                createShareableCacheUri(sourceFile, fileName)
            }
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeSplitApksBundle(
        appInfo: AppInfo,
        outputStream: OutputStream,
        onProgress: (Float) -> Unit
    ) {
        val allFiles = mutableListOf(File(appInfo.apkPath))
        appInfo.splitApkPaths.forEach { path ->
            val f = File(path)
            if (f.exists()) allFiles.add(f)
        }
        val totalBytes = allFiles.sumOf { it.length() }
        var bytesWritten = 0L

        ZipOutputStream(outputStream).use { zipOut ->
            val buffer = ByteArray(64 * 1024)
            allFiles.forEach { file ->
                val entryName = if (file.absolutePath == appInfo.apkPath) "base.apk" else file.name
                val entry = ZipEntry(entryName)
                zipOut.putNextEntry(entry)
                FileInputStream(file).use { input ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        zipOut.write(buffer, 0, read)
                        bytesWritten += read
                        if (totalBytes > 0) {
                            onProgress(bytesWritten.toFloat() / totalBytes)
                        }
                    }
                }
                zipOut.closeEntry()
            }
        }
    }

    private fun createShareableSplitBundleUri(appInfo: AppInfo, fileName: String): Uri {
        val shareDir = File(context.cacheDir, "shared_apks").apply { mkdirs() }
        val targetCacheFile = File(shareDir, fileName)
        FileOutputStream(targetCacheFile).use { output ->
            writeSplitApksBundle(appInfo, output) {}
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetCacheFile
        )
    }

    private fun createShareableCacheUri(sourceFile: File, fileName: String): Uri {
        val shareDir = File(context.cacheDir, "shared_apks").apply { mkdirs() }
        val targetCacheFile = File(shareDir, fileName)
        if (!targetCacheFile.exists() || targetCacheFile.length() != sourceFile.length()) {
            sourceFile.copyTo(targetCacheFile, overwrite = true)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetCacheFile
        )
    }

    private fun copyStreamWithProgress(
        input: InputStream,
        output: OutputStream,
        totalBytes: Long,
        onProgress: (Float) -> Unit
    ) {
        val buffer = ByteArray(64 * 1024)
        var bytesCopied: Long = 0
        var read: Int
        var lastReported = 0

        while (input.read(buffer).also { read = it } >= 0) {
            output.write(buffer, 0, read)
            bytesCopied += read
            if (totalBytes > 0) {
                val progressPercent = ((bytesCopied * 100) / totalBytes).toInt()
                if (progressPercent > lastReported) {
                    lastReported = progressPercent
                    onProgress(bytesCopied.toFloat() / totalBytes)
                }
            }
        }
        output.flush()
        onProgress(1.0f)
    }

    suspend fun getExtractedApks(customFolderUri: Uri?): List<ExtractedApk> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ExtractedApk>()
        try {
            // 1. Scan default Downloads/APK_Extractor
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val defaultDir = File(downloadsDir, DEFAULT_FOLDER_NAME)
            if (defaultDir.exists() && defaultDir.isDirectory) {
                scanDirectoryForApks(defaultDir, result)
            }

            // 2. Scan custom SAF folder if configured
            if (customFolderUri != null) {
                val treeDoc = DocumentFile.fromTreeUri(context, customFolderUri)
                if (treeDoc != null && treeDoc.isDirectory) {
                    scanSafFolderForApks(treeDoc, result)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        result.distinctBy { it.filePath ?: it.fileUri.toString() }
            .sortedByDescending { it.lastModified }
    }

    private fun scanDirectoryForApks(dir: File, result: MutableList<ExtractedApk>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            val name = file.name
            // Skip hidden or trashed files
            if (name.startsWith(".")) continue

            if (file.isDirectory) {
                if (name.endsWith(".apks") || name.endsWith(".bundle")) {
                    val baseApk = File(file, "base.apk")
                    var appName = file.nameWithoutExtension.replace(Regex("_v.*"), "").replace("_", " ")
                    var packageName = ""
                    var versionName = "1.0"
                    var versionCode = 0L

                    if (baseApk.exists()) {
                        val pi = context.packageManager.getPackageArchiveInfo(baseApk.absolutePath, 0)
                        if (pi != null) {
                            packageName = pi.packageName ?: ""
                            versionName = pi.versionName ?: "1.0"
                            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                pi.longVersionCode
                            } else {
                                @Suppress("DEPRECATION") pi.versionCode.toLong()
                            }
                            val appInfo = pi.applicationInfo
                            if (appInfo != null) {
                                appInfo.sourceDir = baseApk.absolutePath
                                appInfo.publicSourceDir = baseApk.absolutePath
                                try {
                                    val label = appInfo.loadLabel(context.packageManager).toString()
                                    if (label.isNotBlank()) appName = label
                                } catch (_: Exception) {}
                            }
                        }
                    }

                    val totalSize = file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    val fileUri = Uri.fromFile(file)
                    result.add(
                        ExtractedApk(
                            fileName = name,
                            fileUri = fileUri,
                            filePath = file.absolutePath,
                            fileSize = totalSize,
                            lastModified = file.lastModified(),
                            appName = appName,
                            packageName = packageName,
                            versionName = versionName,
                            versionCode = versionCode,
                            isSplitBundle = true,
                            isDirectory = true
                        )
                    )
                }
            } else if (file.isFile) {
                val isApksZip = name.endsWith(".apks") || name.endsWith(".apks.zip")
                val isApk = name.endsWith(".apk")

                if (isApk || isApksZip) {
                    var appName = file.nameWithoutExtension.replace(Regex("_v.*"), "").replace("_", " ")
                    var packageName = ""
                    var versionName = "1.0"
                    var versionCode = 0L

                    if (isApk) {
                        val pi = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                        if (pi != null) {
                            packageName = pi.packageName ?: ""
                            versionName = pi.versionName ?: "1.0"
                            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                pi.longVersionCode
                            } else {
                                @Suppress("DEPRECATION") pi.versionCode.toLong()
                            }
                            val appInfo = pi.applicationInfo
                            if (appInfo != null) {
                                appInfo.sourceDir = file.absolutePath
                                appInfo.publicSourceDir = file.absolutePath
                                try {
                                    val label = appInfo.loadLabel(context.packageManager).toString()
                                    if (label.isNotBlank()) appName = label
                                } catch (_: Exception) {}
                            }
                        }
                    }

                    val fileUri = try {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } catch (e: Exception) {
                        Uri.fromFile(file)
                    }

                    result.add(
                        ExtractedApk(
                            fileName = name,
                            fileUri = fileUri,
                            filePath = file.absolutePath,
                            fileSize = file.length(),
                            lastModified = file.lastModified(),
                            appName = appName,
                            packageName = packageName,
                            versionName = versionName,
                            versionCode = versionCode,
                            isSplitBundle = isApksZip,
                            isDirectory = false
                        )
                    )
                }
            }
        }
    }

    private fun scanSafFolderForApks(treeDoc: DocumentFile, result: MutableList<ExtractedApk>) {
        val files = treeDoc.listFiles()
        for (doc in files) {
            val name = doc.name ?: continue
            if (name.startsWith(".")) continue
            val isApk = name.endsWith(".apk")
            val isApks = name.endsWith(".apks") || name.endsWith(".apks.zip")
            if (isApk || isApks) {
                var appName = name.substringBeforeLast(".").replace(Regex("_v.*"), "").replace("_", " ")
                result.add(
                    ExtractedApk(
                        fileName = name,
                        fileUri = doc.uri,
                        filePath = null,
                        fileSize = doc.length(),
                        lastModified = doc.lastModified(),
                        appName = appName,
                        packageName = "",
                        versionName = "1.0",
                        versionCode = 0L,
                        isSplitBundle = isApks,
                        isDirectory = doc.isDirectory
                    )
                )
            }
        }
    }

    suspend fun deleteExtractedApk(item: ExtractedApk): Boolean = withContext(Dispatchers.IO) {
        var deleted = false

        // 1. Try MediaStore deletion (Downloads and Files tables on Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            try {
                val downloadsUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(item.fileName)
                resolver.query(downloadsUri, arrayOf(MediaStore.MediaColumns._ID), selection, selectionArgs, null)?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        val rowUri = ContentUris.withAppendedId(downloadsUri, id)
                        val rows = resolver.delete(rowUri, null, null)
                        if (rows > 0) deleted = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (!deleted && item.filePath != null) {
                try {
                    val filesUri = MediaStore.Files.getContentUri("external")
                    val pathSelection = "${MediaStore.MediaColumns.DATA} = ?"
                    val pathArgs = arrayOf(item.filePath)
                    resolver.query(filesUri, arrayOf(MediaStore.MediaColumns._ID), pathSelection, pathArgs, null)?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                            val rowUri = ContentUris.withAppendedId(filesUri, id)
                            val rows = resolver.delete(rowUri, null, null)
                            if (rows > 0) deleted = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 2. Try Storage Access Framework (SAF / DocumentsContract)
        try {
            if (item.fileUri.scheme == "content" && !item.fileUri.authority.orEmpty().contains("fileprovider")) {
                if (DocumentsContract.isDocumentUri(context, item.fileUri)) {
                    val safDeleted = DocumentsContract.deleteDocument(context.contentResolver, item.fileUri)
                    if (safDeleted) deleted = true
                } else {
                    val count = context.contentResolver.delete(item.fileUri, null, null)
                    if (count > 0) deleted = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Try DocumentFile
        try {
            val doc = DocumentFile.fromSingleUri(context, item.fileUri)
            if (doc != null && doc.exists()) {
                if (doc.delete()) deleted = true
            }
        } catch (_: Exception) {}

        // 4. Try direct filesystem File.delete() / deleteRecursively()
        if (item.filePath != null) {
            try {
                val file = File(item.filePath)
                if (file.exists()) {
                    val fileDeleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
                    if (fileDeleted) deleted = true
                } else {
                    deleted = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 5. Final check: verify if the file is gone from the filesystem
        if (item.filePath != null) {
            val file = File(item.filePath)
            if (!file.exists()) {
                deleted = true
            }
        }

        deleted
    }
}
