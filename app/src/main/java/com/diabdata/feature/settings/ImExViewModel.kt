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
    suspend fun exportDataAsJsonString(): String {
        val gson = GsonFactory.create(prettyPrint = true)

        val weights = repository.getAllWeights().first()
        val hba1cEntries = repository.getAllHba1c().first()
        val appointments = repository.getAllAppointments().first()
        val treatments = repository.getAllTreatments().first()
        val importantDates = repository.getAllImportantDates().first()
        val medicalDevices = repository.getAllDevices().first()
        val userDetails = repository.getUserDetails().first()
        val userPreferences = repository.getUserPreferences().first()


        val exportData = ExportData(
            weights = weights,
            hba1c = hba1cEntries,
            appointments = appointments,
            treatments = treatments,
            importantDates = importantDates,
            devices = medicalDevices,
            userDetails = userDetails,
            userPreferences = userPreferences
        )

        return gson.toJson(exportData)
    }

    suspend fun importDataFromJsonString(json: String, profilePhotoPath: String? = null) {
        val gson = GsonFactory.create()

        val importedData: ExportData = gson.fromJson(json, ExportData::class.java)

        withContext(Dispatchers.IO) {
            importedData.weights.forEach { weight ->
                repository.insertWeight(weight.copy()) // Reset IDs to have them auto incremented by Room to prevent app crashes
            }
            importedData.hba1c.forEach { hba1c ->
                repository.insertHba1c(hba1c.copy())
            }
            importedData.appointments.forEach { appointment ->
                repository.insertAppointment(appointment.copy())
            }
            importedData.treatments.forEach { treatment ->
                repository.insertTreatment(treatment.copy())
            }
            importedData.importantDates.forEach { diagnosis ->
                repository.insertImportantDate(diagnosis.copy())
            }
            importedData.devices.forEach { device ->
                repository.insertDevice(device.copy())
            }
            importedData.userDetails?.let { userDetails ->
                repository.updateUserDetails(userDetails.copy(profilePhotoPath = profilePhotoPath))
            }
            importedData.userPreferences?.let { userPreferences ->
                repository.insertOrUpdate(userPreferences.copy())
            }
        }
    }
}