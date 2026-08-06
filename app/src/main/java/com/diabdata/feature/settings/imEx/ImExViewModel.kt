package com.diabdata.feature.settings.imEx

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabdata.core.backup.BackupArchiveManager
import com.diabdata.core.database.DataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class ImExViewModel @Inject constructor (
    val repository: DataRepository,
    private val backupArchiveManager: BackupArchiveManager
): ViewModel() {
    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()
    suspend fun exportData(output: OutputStream): Result<Unit> {
        val encrypted = repository.isBackupEncryptionEnabled()
        return backupArchiveManager.writeBackup(
            output,
            isScheduledBackup = false,
            isEncrypted = encrypted
        )
    }

    suspend fun importData(input: InputStream): Result<Unit> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                backupArchiveManager.readBackup(input)
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun onFileSelected(input: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = ImportUiState.Loading
            try {
                val fileBuffer = input.use { it.readBytes() }
                if (fileBuffer.isEmpty()) {
                    _uiState.value = ImportUiState.Error("Empty file")
                    return@launch
                }
                val result = ByteArrayInputStream(fileBuffer).use { importData(it) }
                _uiState.value = result.fold(
                    onSuccess = { ImportUiState.Success },
                    onFailure = { ImportUiState.Error(it.message ?: "Unknown error") }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ImportUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    suspend fun importDataFromJsonString(json: String, profilePhotoPath: String? = null) = repository.importDataFromJsonString(json, profilePhotoPath)
}