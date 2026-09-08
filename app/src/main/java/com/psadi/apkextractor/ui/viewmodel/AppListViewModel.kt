package com.psadi.apkextractor.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.psadi.apkextractor.data.model.AppCategory
import com.psadi.apkextractor.data.model.AppInfo
import com.psadi.apkextractor.data.model.ExtractionState
import com.psadi.apkextractor.data.model.ExtractedApk
import com.psadi.apkextractor.data.`package`.AppPackageScanner
import com.psadi.apkextractor.data.preferences.PreferencesManager
import com.psadi.apkextractor.data.storage.StorageRepository
import com.psadi.apkextractor.util.IntentUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val packageScanner = AppPackageScanner(application)
    private val storageRepository = StorageRepository(application)
    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedCategory = preferencesManager.selectedCategory.first()
            val savedFolder = preferencesManager.customFolderUri.first()
            _uiState.update {
                it.copy(
                    selectedCategory = savedCategory,
                    customFolderUriString = savedFolder
                )
            }
            loadApps()
        }
    }

    fun loadApps(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true) }
            }
            val customUri = _uiState.value.customFolderUriString?.let { Uri.parse(it) }
            val apps = packageScanner.getInstalledApps()
            val extracted = storageRepository.getExtractedApks(customUri)
            val userCount = apps.count { !it.isSystemApp }
            val systemCount = apps.count { it.isSystemApp }
            val extractedCount = extracted.size

            _uiState.update { state ->
                val filtered = filterAndSortApps(apps, state.searchQuery, state.selectedCategory)
                val filteredExtracted = filterExtractedApps(extracted, state.searchQuery)
                val indexMap = computeAlphabetIndexMap(filtered)
                state.copy(
                    isLoading = false,
                    allApps = apps,
                    filteredApps = filtered,
                    allExtractedApps = extracted,
                    filteredExtractedApps = filteredExtracted,
                    userAppCount = userCount,
                    systemAppCount = systemCount,
                    extractedAppCount = extractedCount,
                    alphabetIndexMap = indexMap
                )
            }
        }
    }

    fun loadExtractedApps() {
        viewModelScope.launch {
            val customUri = _uiState.value.customFolderUriString?.let { Uri.parse(it) }
            val extracted = storageRepository.getExtractedApks(customUri)
            _uiState.update { state ->
                val filteredExtracted = filterExtractedApps(extracted, state.searchQuery)
                state.copy(
                    allExtractedApps = extracted,
                    filteredExtractedApps = filteredExtracted,
                    extractedAppCount = extracted.size
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = filterAndSortApps(state.allApps, query, state.selectedCategory)
            val filteredExtracted = filterExtractedApps(state.allExtractedApps, query)
            val indexMap = computeAlphabetIndexMap(filtered)
            state.copy(
                searchQuery = query,
                filteredApps = filtered,
                filteredExtractedApps = filteredExtracted,
                alphabetIndexMap = indexMap
            )
        }
    }

    fun onCategorySelected(category: AppCategory) {
        viewModelScope.launch {
            preferencesManager.setSelectedCategory(category)
        }
        _uiState.update { state ->
            val filtered = filterAndSortApps(state.allApps, state.searchQuery, category)
            val filteredExtracted = filterExtractedApps(state.allExtractedApps, state.searchQuery)
            val indexMap = computeAlphabetIndexMap(filtered)
            state.copy(
                selectedCategory = category,
                filteredApps = filtered,
                filteredExtractedApps = filteredExtracted,
                alphabetIndexMap = indexMap
            )
        }
    }

    fun onAppClicked(appInfo: AppInfo) {
        extractApk(appInfo)
    }

    fun onAppActionRequested(appInfo: AppInfo) {
        _uiState.update { it.copy(selectedAppForSheet = appInfo) }
    }

    fun onDismissBottomSheet() {
        _uiState.update { it.copy(selectedAppForSheet = null) }
    }

    fun extractApk(appInfo: AppInfo) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeExtractionState = ExtractionState.Extracting(
                        packageName = appInfo.packageName,
                        appName = appInfo.appName,
                        progress = 0f
                    )
                )
            }

            val customUri = _uiState.value.customFolderUriString?.let { Uri.parse(it) }

            val result = storageRepository.extractApk(
                appInfo = appInfo,
                customFolderUri = customUri,
                onProgress = { progress ->
                    _uiState.update { state ->
                        if (state.activeExtractionState is ExtractionState.Extracting) {
                            state.copy(
                                activeExtractionState = state.activeExtractionState.copy(progress = progress)
                            )
                        } else {
                            state
                        }
                    }
                }
            )

            result.fold(
                onSuccess = { extracted ->
                    _uiState.update {
                        it.copy(
                            activeExtractionState = ExtractionState.Success(
                                appName = appInfo.appName,
                                fileName = extracted.fileName,
                                destinationPath = extracted.destinationDisplayName,
                                shareableUri = extracted.shareableUri,
                                destinationFolderUri = extracted.folderUri,
                                appInfo = appInfo
                            )
                        )
                    }
                    loadExtractedApps()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            activeExtractionState = ExtractionState.Error(
                                appName = appInfo.appName,
                                message = error.localizedMessage ?: "Unknown extraction error"
                            )
                        )
                    }
                }
            )
        }
    }

    fun shareApk(appInfo: AppInfo, context: Context) {
        viewModelScope.launch {
            val result = storageRepository.getShareableUriForApp(appInfo)
            result.fold(
                onSuccess = { uri ->
                    IntentUtil.shareApk(context, uri, appInfo.appName)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            activeExtractionState = ExtractionState.Error(
                                appName = appInfo.appName,
                                message = "Cannot share APK: ${error.localizedMessage}"
                            )
                        )
                    }
                }
            )
        }
    }

    fun openAppInfo(appInfo: AppInfo, context: Context) {
        IntentUtil.openAppInfo(context, appInfo.packageName)
    }

    fun openFolder(folderUri: Uri?, context: Context) {
        IntentUtil.openFolder(context, folderUri)
    }

    fun setCustomFolderUri(uri: Uri?) {
        val uriString = uri?.toString()
        viewModelScope.launch {
            preferencesManager.setCustomFolderUri(uriString)
        }
        _uiState.update { it.copy(customFolderUriString = uriString) }
    }

    fun setSettingsDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isSettingsDialogOpen = isOpen) }
    }

    fun clearActiveExtraction() {
        _uiState.update { it.copy(activeExtractionState = ExtractionState.Idle) }
    }

    fun installExtractedApk(context: Context, item: ExtractedApk) {
        IntentUtil.installExtractedApk(context, item)
    }

    fun shareExtractedApk(context: Context, item: ExtractedApk) {
        IntentUtil.shareApk(context, item.fileUri, item.appName)
    }

    fun deleteExtractedApk(item: ExtractedApk) {
        viewModelScope.launch {
            storageRepository.deleteExtractedApk(item)
            // Always reload extracted apps so UI updates to actual storage state
            loadExtractedApps()
        }
    }

    private fun filterExtractedApps(
        apps: List<ExtractedApk>,
        query: String
    ): List<ExtractedApk> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return apps
        return apps.filter { item ->
            item.appName.contains(trimmed, ignoreCase = true) ||
                    item.packageName.contains(trimmed, ignoreCase = true) ||
                    item.fileName.contains(trimmed, ignoreCase = true) ||
                    (item.displayBrandTag != null && item.displayBrandTag!!.contains(trimmed, ignoreCase = true)) ||
                    com.psadi.apkextractor.util.SearchUtil.getSearchAliases(item.packageName, item.appName)
                        .any { it.contains(trimmed, ignoreCase = true) }
        }
    }

    private fun filterAndSortApps(
        apps: List<AppInfo>,
        query: String,
        category: AppCategory
    ): List<AppInfo> {
        val trimmed = query.trim()
        return apps.filter { app ->
            val matchesCategory = when (category) {
                AppCategory.USER -> !app.isSystemApp
                AppCategory.SYSTEM -> app.isSystemApp
                AppCategory.EXTRACTED -> false
            }
            val matchesQuery = if (trimmed.isEmpty()) {
                true
            } else {
                app.appName.contains(trimmed, ignoreCase = true) ||
                        app.packageName.contains(trimmed, ignoreCase = true) ||
                        (app.displayBrandTag != null && app.displayBrandTag!!.contains(trimmed, ignoreCase = true)) ||
                        app.searchAliases.any { it.contains(trimmed, ignoreCase = true) }
            }
            matchesCategory && matchesQuery
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
    }

    private fun computeAlphabetIndexMap(apps: List<AppInfo>): Map<Char, Int> {
        val map = LinkedHashMap<Char, Int>()
        apps.forEachIndexed { index, app ->
            val firstChar = app.appName.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (firstChar in 'A'..'Z') firstChar else '#'
            if (!map.containsKey(key)) {
                map[key] = index
            }
        }
        return map
    }
}
