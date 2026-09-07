package com.psadi.apkextractor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.psadi.apkextractor.ui.screens.AppListScreen
import com.psadi.apkextractor.ui.theme.ApkExtractorTheme
import com.psadi.apkextractor.ui.viewmodel.AppListViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppListViewModel by viewModels()

    private val safFolderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Log or handle if persistable permissions not supported
            }
            viewModel.setCustomFolderUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ApkExtractorTheme {
                AppListScreen(
                    viewModel = viewModel,
                    onPickSafFolder = {
                        safFolderPicker.launch(null)
                    }
                )
            }
        }
    }
}
