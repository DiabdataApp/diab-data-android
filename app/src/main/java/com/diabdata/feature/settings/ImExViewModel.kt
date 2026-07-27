package com.diabdata.feature.settings

import androidx.lifecycle.ViewModel
import com.diabdata.core.backup.BackupArchiveManager
import com.diabdata.core.database.DataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.OutputStream
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class ImExViewModel @Inject constructor (
    val repository: DataRepository,
    private val backupArchiveManager: BackupArchiveManager
): ViewModel() {
    suspend fun exportData(output: OutputStream): Result<Unit> =
        backupArchiveManager.writeBackup(output)

    suspend fun importDataFromJsonString(json: String, profilePhotoPath: String? = null) = repository.importDataFromJsonString(json, profilePhotoPath)
}