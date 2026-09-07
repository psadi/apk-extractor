package com.psadi.apkextractor.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.psadi.apkextractor.data.model.AppCategory
import com.psadi.apkextractor.data.model.AppInfo
import com.psadi.apkextractor.data.model.ExtractionState
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

    fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val apps = packageScanner.getInstalledApps()
            val userCount = apps.count { !it.isSystemApp }
            val systemCount = apps.count { it.isSystemApp }

            _uiState.update { state ->
                val filtered = filterAndSortApps(apps, state.searchQuery, state.selectedCategory)
                val indexMap = computeAlphabetIndexMap(filtered)
                state.copy(
                    isLoading = false,
                    allApps = apps,
                    filteredApps = filtered,
                    userAppCount = userCount,
                    systemAppCount = systemCount,
                    alphabetIndexMap = indexMap
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = filterAndSortApps(state.allApps, query, state.selectedCategory)
            val indexMap = computeAlphabetIndexMap(filtered)
            state.copy(
                searchQuery = query,
                filteredApps = filtered,
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
            val indexMap = computeAlphabetIndexMap(filtered)
            state.copy(
                selectedCategory = category,
                filteredApps = filtered,
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
            }
            val matchesQuery = if (trimmed.isEmpty()) {
                true
            } else {
                app.appName.contains(trimmed, ignoreCase = true) ||
                        app.packageName.contains(trimmed, ignoreCase = true)
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
