package com.psadi.apkextractor.data.model

import android.net.Uri

sealed interface ExtractionState {
    object Idle : ExtractionState

    data class Extracting(
        val packageName: String,
        val appName: String,
        val progress: Float = 0f
    ) : ExtractionState

    data class Success(
        val appName: String,
        val fileName: String,
        val destinationPath: String,
        val shareableUri: Uri,
        val destinationFolderUri: Uri?,
        val appInfo: AppInfo? = null
    ) : ExtractionState

    data class Error(
        val appName: String,
        val message: String
    ) : ExtractionState
}
