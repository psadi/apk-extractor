package com.psadi.apkextractor.data.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.psadi.apkextractor.data.model.AppInfo
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
}
