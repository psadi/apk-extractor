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

            val fileName = appInfo.sanitizedApkFileName
            val totalBytes = sourceFile.length()

            if (customFolderUri != null) {
                extractToSafTree(sourceFile, fileName, customFolderUri, totalBytes, onProgress)
            } else {
                extractToDefaultDownloads(sourceFile, fileName, totalBytes, onProgress)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractToSafTree(
        sourceFile: File,
        fileName: String,
        treeUri: Uri,
        totalBytes: Long,
        onProgress: (Float) -> Unit
    ): Result<ExtractedApkResult> {
        val treeDoc = DocumentFile.fromTreeUri(context, treeUri)
            ?: return Result.failure(Exception("Cannot access destination folder"))

        // Overwrite existing file if present
        treeDoc.findFile(fileName)?.delete()

        val createdFile = treeDoc.createFile(APK_MIME_TYPE, fileName)
            ?: return Result.failure(Exception("Failed to create file in destination folder"))

        val outputStream = context.contentResolver.openOutputStream(createdFile.uri)
            ?: return Result.failure(Exception("Failed to open output stream"))

        FileInputStream(sourceFile).use { input ->
            outputStream.use { output ->
                copyStreamWithProgress(input, output, totalBytes, onProgress)
            }
        }

        // Prepare shareable URI via cache for robust sharing compatibility
        val shareableUri = createShareableCacheUri(sourceFile, fileName)

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
        sourceFile: File,
        fileName: String,
        totalBytes: Long,
        onProgress: (Float) -> Unit
    ): Result<ExtractedApkResult> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, APK_MIME_TYPE)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DEFAULT_FOLDER_NAME")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val collectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val itemUri = resolver.insert(collectionUri, contentValues)
                ?: return Result.failure(Exception("Unable to create MediaStore entry in Downloads"))

            try {
                resolver.openOutputStream(itemUri)?.use { output ->
                    FileInputStream(sourceFile).use { input ->
                        copyStreamWithProgress(input, output, totalBytes, onProgress)
                    }
                } ?: return Result.failure(Exception("Unable to open output stream for MediaStore entry"))

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)

                val shareableUri = createShareableCacheUri(sourceFile, fileName)

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

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(targetFile).use { output ->
                    copyStreamWithProgress(input, output, totalBytes, onProgress)
                }
            }

            val shareableUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                )
            } catch (e: Exception) {
                createShareableCacheUri(sourceFile, fileName)
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
            val uri = createShareableCacheUri(sourceFile, appInfo.sanitizedApkFileName)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
