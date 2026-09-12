package com.psadi.apkextractor.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psadi.apkextractor.data.model.AppCategory
import com.psadi.apkextractor.data.model.ExtractionState
import com.psadi.apkextractor.ui.components.AlphabeticalFastScroller
import com.psadi.apkextractor.ui.components.AppCardItem
import com.psadi.apkextractor.ui.components.AppDetailBottomSheet
import com.psadi.apkextractor.ui.components.AppFilterTabs
import com.psadi.apkextractor.ui.components.ExtractedApkCardItem
import com.psadi.apkextractor.ui.components.PersistentSearchBar
import com.psadi.apkextractor.ui.components.SettingsDialog
import com.psadi.apkextractor.ui.viewmodel.AppListViewModel
import com.psadi.apkextractor.util.IntentUtil
import kotlinx.coroutines.launch

@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onPickSafFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // React to extraction errors and duplicate detection
    LaunchedEffect(uiState.activeExtractionState) {
        val state = uiState.activeExtractionState
        when (state) {
            is ExtractionState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "${state.appName}: ${state.message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.clearActiveExtraction()
            }
            is ExtractionState.AlreadyExists -> {
                val result = snackbarHostState.showSnackbar(
                    message = "\"${state.appName}\" already extracted as ${state.fileName}",
                    actionLabel = "Re-extract",
                    duration = SnackbarDuration.Long,
                    withDismissAction = true
                )
                viewModel.clearActiveExtraction()
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.extractApkForce(state.appInfo)
                }
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Persistent Search Bar
                PersistentSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onRefreshClick = { viewModel.loadApps() },
                    onSettingsClick = { viewModel.setSettingsDialogOpen(true) }
                )

                // Category Filter Tabs
                val isSearching = uiState.searchQuery.trim().isNotEmpty()
                AppFilterTabs(
                    selectedCategory = uiState.selectedCategory,
                    allCount = if (isSearching) uiState.searchMatchAllCount else uiState.allAppCount,
                    userCount = if (isSearching) uiState.searchMatchUserCount else uiState.userAppCount,
                    systemCount = if (isSearching) uiState.searchMatchSystemCount else uiState.systemAppCount,
                    extractedCount = if (isSearching) uiState.searchMatchExtractedCount else uiState.extractedAppCount,
                    onCategorySelected = { viewModel.onCategorySelected(it) }
                )

                // Main Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Indexing applications…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (uiState.selectedCategory == AppCategory.EXTRACTED) {
                        if (uiState.filteredExtractedApps.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Text(
                                        text = if (uiState.searchQuery.isNotEmpty()) {
                                            "No extracted backups match \"${uiState.searchQuery}\""
                                        } else {
                                            "No extracted APK backups found\n\nExtract any app from the Installed or System tabs to view, share, or reinstall backups here."
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    if (uiState.searchQuery.isNotEmpty() && uiState.searchMatchAllCount > 0) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        FilledTonalButton(
                                            onClick = { viewModel.onCategorySelected(AppCategory.ALL) }
                                        ) {
                                            Text("Found ${uiState.searchMatchAllCount} match in installed apps → View All")
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.filteredExtractedApps,
                                    key = { it.filePath ?: it.fileUri.toString() }
                                ) { extractedItem ->
                                    ExtractedApkCardItem(
                                        item = extractedItem,
                                        onInstallClick = {
                                            viewModel.installExtractedApk(context, extractedItem)
                                        },
                                        onShareClick = {
                                            viewModel.shareExtractedApk(context, extractedItem)
                                        },
                                        onDeleteClick = {
                                            viewModel.deleteExtractedApk(extractedItem)
                                        }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(80.dp))
                                }
                            }
                        }
                    } else if (uiState.filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text(
                                    text = if (uiState.searchQuery.isNotEmpty()) {
                                        val categoryName = when (uiState.selectedCategory) {
                                            AppCategory.ALL -> "all apps"
                                            AppCategory.USER -> "installed apps"
                                            AppCategory.SYSTEM -> "system apps"
                                            AppCategory.EXTRACTED -> "backups"
                                        }
                                        "No applications match \"${uiState.searchQuery}\" in $categoryName"
                                    } else {
                                        "No applications found"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                if (uiState.searchQuery.isNotEmpty()) {
                                    if (uiState.selectedCategory == AppCategory.USER && uiState.searchMatchSystemCount > 0) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        FilledTonalButton(
                                            onClick = { viewModel.onCategorySelected(AppCategory.SYSTEM) }
                                        ) {
                                            Text("Found ${uiState.searchMatchSystemCount} match in System apps → Switch")
                                        }
                                    } else if (uiState.selectedCategory == AppCategory.SYSTEM && uiState.searchMatchUserCount > 0) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        FilledTonalButton(
                                            onClick = { viewModel.onCategorySelected(AppCategory.USER) }
                                        ) {
                                            Text("Found ${uiState.searchMatchUserCount} match in Installed apps → Switch")
                                        }
                                    }
                                    if (uiState.searchMatchExtractedCount > 0) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = { viewModel.onCategorySelected(AppCategory.EXTRACTED) }
                                        ) {
                                            Text("Found ${uiState.searchMatchExtractedCount} match in Backups → Switch")
                                        }
                                    }
                                    if (uiState.selectedCategory != AppCategory.ALL && uiState.searchMatchAllCount > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(
                                            onClick = { viewModel.onCategorySelected(AppCategory.ALL) }
                                        ) {
                                            Text("Show All (${uiState.searchMatchAllCount})")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // App List
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = uiState.filteredApps,
                                key = { it.packageName }
                            ) { appInfo ->
                                val isCurrentlyExtracting =
                                    (uiState.activeExtractionState as? ExtractionState.Extracting)?.packageName == appInfo.packageName
                                val progress = if (isCurrentlyExtracting) {
                                    (uiState.activeExtractionState as ExtractionState.Extracting).progress
                                } else 0f

                                AppCardItem(
                                    appInfo = appInfo,
                                    isExtracting = isCurrentlyExtracting,
                                    extractionProgress = progress,
                                    onClick = { viewModel.onAppClicked(appInfo) },
                                    onLongClick = { viewModel.onAppActionRequested(appInfo) },
                                    onMoreClick = { viewModel.onAppActionRequested(appInfo) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }

                        // Alphabetical Fast Scroller Sidebar
                        if (uiState.alphabetIndexMap.isNotEmpty()) {
                            AlphabeticalFastScroller(
                                alphabetMap = uiState.alphabetIndexMap,
                                onLetterSelected = { index ->
                                    coroutineScope.launch {
                                        listState.scrollToItem(index)
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            }

            // Extraction Success Banner with Action Buttons (Share & Open Folder)
            AnimatedVisibility(
                visible = uiState.activeExtractionState is ExtractionState.Success,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                val success = uiState.activeExtractionState as? ExtractionState.Success
                if (success != null) {
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Extracted ${success.appName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Saved to ${success.destinationPath}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(onClick = { viewModel.clearActiveExtraction() }) {
                                    Text("Dismiss")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Action: Share APK
                                Button(
                                    onClick = {
                                        IntentUtil.shareApk(context, success.shareableUri, success.appName)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Share APK",
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }

                                // Action: Open Folder
                                OutlinedButton(
                                    onClick = {
                                        IntentUtil.openFolder(context, success.destinationFolderUri)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Open Folder",
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // App Detail Bottom Sheet
            uiState.selectedAppForSheet?.let { appInfo ->
                AppDetailBottomSheet(
                    appInfo = appInfo,
                    onDismiss = { viewModel.onDismissBottomSheet() },
                    onExtractClick = { viewModel.extractApk(appInfo) },
                    onShareClick = { viewModel.shareApk(appInfo, context) },
                    onAppInfoClick = { viewModel.openAppInfo(appInfo, context) }
                )
            }

            // Settings Dialog
            if (uiState.isSettingsDialogOpen) {
                SettingsDialog(
                    currentCustomUri = uiState.customFolderUriString,
                    onPickFolderClick = {
                        viewModel.setSettingsDialogOpen(false)
                        onPickSafFolder()
                    },
                    onResetToDefault = {
                        viewModel.setCustomFolderUri(null)
                    },
                    onDismiss = { viewModel.setSettingsDialogOpen(false) }
                )
            }
        }
    }
}
