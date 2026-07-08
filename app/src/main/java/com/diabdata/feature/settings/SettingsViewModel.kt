package com.diabdata.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabdata.core.database.DiabDataDatabase
import com.diabdata.feature.dataMatrixScanner.utils.MedicalDevicesInitializer
import com.diabdata.feature.dataMatrixScanner.utils.MedicationInitializer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: DiabDataDatabase
) : ViewModel() {

    fun forceRebuildMedicalDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            MedicalDevicesInitializer(context, db).initialize()
        }
    }

    fun forceRebuildMedications() {
        viewModelScope.launch(Dispatchers.IO) {
            MedicationInitializer(context, db).initialize()
        }
    }
}