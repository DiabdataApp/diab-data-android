package com.diabdata.feature.settings.imEx

sealed class ImportUiState {
    object PasswordRequired : ImportUiState()
    object Idle : ImportUiState()
    object Loading : ImportUiState()
    object Success : ImportUiState()
    data class Error(val message: String) : ImportUiState()
}