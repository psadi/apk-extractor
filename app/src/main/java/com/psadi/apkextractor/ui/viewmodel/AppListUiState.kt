package com.psadi.apkextractor.ui.viewmodel

import com.psadi.apkextractor.data.model.AppCategory
import com.psadi.apkextractor.data.model.AppInfo
import com.psadi.apkextractor.data.model.ExtractionState
import com.psadi.apkextractor.data.model.ExtractedApk

data class AppListUiState(
    val isLoading: Boolean = true,
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val allExtractedApps: List<ExtractedApk> = emptyList(),
    val filteredExtractedApps: List<ExtractedApk> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: AppCategory = AppCategory.USER,
    val userAppCount: Int = 0,
    val systemAppCount: Int = 0,
    val extractedAppCount: Int = 0,
    val alphabetIndexMap: Map<Char, Int> = emptyMap(),
    val activeExtractionState: ExtractionState = ExtractionState.Idle,
    val selectedAppForSheet: AppInfo? = null,
    val customFolderUriString: String? = null,
    val isSettingsDialogOpen: Boolean = false
)
