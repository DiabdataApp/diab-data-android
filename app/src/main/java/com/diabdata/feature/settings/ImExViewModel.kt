package com.diabdata.feature.settings

import androidx.lifecycle.ViewModel
import com.diabdata.core.database.DataRepository
import com.diabdata.core.database.ExportData
import com.diabdata.core.utils.data.GsonFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ImExViewModel @Inject constructor (
    val repository: DataRepository
): ViewModel() {
    suspend fun exportDataAsJsonString(): String = repository.exportDataAsJsonString()

    suspend fun importDataFromJsonString(json: String, profilePhotoPath: String? = null) = repository.importDataFromJsonString(json, profilePhotoPath)
}