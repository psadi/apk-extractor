package com.psadi.apkextractor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.psadi.apkextractor.ui.screens.AppListScreen
import com.psadi.apkextractor.ui.theme.ApkExtractorTheme
import com.psadi.apkextractor.ui.viewmodel.AppListViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            } catch (_: Exception) {}
            viewModel.setCustomFolderUri(uri)
        }
    }

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.loadApps(showLoading = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Register package receivers for full activity lifetime so background/external installs
        // are never missed while the activity is paused behind an installer or store dialog
        val systemFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(packageChangeReceiver, systemFilter, ContextCompat.RECEIVER_EXPORTED)
            } else {
                registerReceiver(packageChangeReceiver, systemFilter)
            }
        } catch (_: Exception) {}

        val internalFilter = IntentFilter("com.psadi.apkextractor.REFRESH_APPS")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(packageChangeReceiver, internalFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(packageChangeReceiver, internalFilter)
            }
        } catch (_: Exception) {}

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

    override fun onResume() {
        super.onResume()
        // Immediate silent refresh
        viewModel.loadApps(showLoading = false)

        // Delayed secondary refresh to allow PackageManager cache to settle after external install/uninstall
        lifecycleScope.launch {
            delay(1200)
            viewModel.loadApps(showLoading = false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(packageChangeReceiver)
        } catch (_: Exception) {}
    }
}
