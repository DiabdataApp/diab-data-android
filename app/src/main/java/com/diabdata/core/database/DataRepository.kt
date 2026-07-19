package com.diabdata.core.database

import com.diabdata.feature.appointments.data.AppointmentDao
import com.diabdata.feature.hba1c.data.HBA1CDao
import com.diabdata.feature.importantDates.data.ImportantDateDao
import com.diabdata.feature.devices.data.MedicalDeviceDao
import com.diabdata.feature.devices.data.MedicalDevicesInfoDao
import com.diabdata.feature.dataMatrixScanner.data.MedicationDao
import com.diabdata.feature.treatments.data.TreatmentDao
import com.diabdata.feature.userProfile.data.UserDetailsDao
import com.diabdata.feature.weight.data.WeightDao
import com.diabdata.core.model.Appointment
import com.diabdata.core.model.Hba1c
import com.diabdata.core.model.ImportantDate
import com.diabdata.core.model.MedicalDevice
import com.diabdata.core.model.MedicalDeviceInfoEntity
import com.diabdata.core.model.Medication
import com.diabdata.core.model.Treatment
import com.diabdata.core.model.UserDetails
import com.diabdata.core.model.UserPreferences
import com.diabdata.core.model.Weight
import com.diabdata.feature.devices.classes.FaultyBatchCount
import com.diabdata.feature.graphs.classes.PlotPoint
import com.diabdata.feature.settings.data.UserPreferencesDao
import com.diabdata.shared.utils.dataTypes.MedicalDeviceInfoType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

class DataRepository(
    private val weightDao: WeightDao,
    private val hba1cDao: HBA1CDao,
    private val appointmentDao: AppointmentDao,
    private val treatmentDao: TreatmentDao,
    private val importantDateDao: ImportantDateDao,
    private val medicationDao: MedicationDao,
    private val medicalDevicesDao: MedicalDeviceDao,
    private val medicalDeviceInfo: MedicalDevicesInfoDao,
    private val userDetailsDao: UserDetailsDao,
    private val userPreferencesDao: UserPreferencesDao,
    val database: DiabDataDatabase,
) {
    // ----------------
    // Weight
    // ----------------
    /** Insert a new weight entry */
    suspend fun insertWeight(weight: Weight) = weightDao.insert(weight)

    /** Update weight entry */
    suspend fun updateWeight(weight: Weight) = weightDao.update(weight)

    /** Flow of all weight entries, sorted by date */
    fun getAllWeights(): Flow<List<Weight>> = weightDao.getAllWeightsFlow()

    /** Flow of weight points to plot in graph between min and max date */
    fun getWeightPlotData(minDate: LocalDate, maxDate: LocalDate): Flow<List<PlotPoint>> =
        weightDao.getWeightPlotData(minDate, maxDate)

    /** Flow of weight entries from the last year */
    fun getRecentWeights(): Flow<List<Weight>> {
        val oneYearAgo = LocalDate.now().minusYears(1)
        return weightDao.getWeightsSince(oneYearAgo)
    }

    /** Delete a weight record by Id**/
    suspend fun deleteWeight(id: Int) = weightDao.deleteById(id)

    // ----------------
    // HBA1C
    // ----------------
    /** Insert a new HBA1C entry */
    suspend fun insertHba1c(hba1C: Hba1c) = hba1cDao.insert(hba1C)

    /** Update HBA1C */
    suspend fun updateHBA1C(hba1C: Hba1c) = hba1cDao.update(hba1C)

    /** Flow of all HBA1C entries */
    fun getAllHba1c(): Flow<List<Hba1c>> = hba1cDao.getAllHBA1CFlow()

    /** Flow of weight points to plot in graph between min and max date */
    fun getHba1cPlotData(minDate: LocalDate, maxDate: LocalDate): Flow<List<PlotPoint>> =
        hba1cDao.getHBA1CPlotData(minDate, maxDate)

    /** Flow of HBA1C entries from the last year */
    fun getRecentHba1c(): Flow<List<Hba1c>> {
        val oneYearAgo = LocalDate.now().minusYears(1)
        return hba1cDao.getHBA1CEntriesSince(oneYearAgo)
    }

    /** Delete an HBA1C record by Id**/
    suspend fun deleteHba1c(id: Int) = hba1cDao.deleteById(id)

    // ----------------
    // Appointment
    // ----------------
    /** Insert a new appointment */
    suspend fun insertAppointment(appointment: Appointment) = appointmentDao.insert(appointment)

    /** Update appointment */
    suspend fun updateAppointment(appointment: Appointment) = appointmentDao.update(appointment)

    /** Flow of all appointments */
    fun getAllAppointments(): Flow<List<Appointment>> = appointmentDao.getAllAppointmentsFlow()

    /** Flow of upcoming appointments starting today */
    fun getUpcomingAppointments(): Flow<List<Appointment>> {
        return appointmentDao.getUpcomingAppointmentsFlow(LocalDateTime.now())
    }

    /** Delete an Appointment record by Id**/
    suspend fun deleteAppointment(id: Int) = appointmentDao.deleteById(id)

    // ----------------
    // Treatment
    // ----------------
    /** Insert a new treatments */
    suspend fun insertTreatment(treatment: Treatment) = treatmentDao.insert(treatment)

    /** Update treatments */
    suspend fun updateTreatment(treatment: Treatment) = treatmentDao.update(treatment)

    /** Flow of all treatments */
    fun getAllTreatments(): Flow<List<Treatment>> = treatmentDao.getAllTreatmentsFlow()

    /** Flow of upcoming treatments expiration dates */
    fun getUpcomingExpDates(): Flow<List<Treatment>> =
        treatmentDao.getUpcomingExpirationDatesFlow(LocalDate.now())

    /** Delete a treatments record by Id**/
    suspend fun deleteTreatment(id: Int) = treatmentDao.deleteById(id)

    // ----------------
    // Medication
    // ----------------
    /** Find a medication by its code (GTIN or CIP) */
    suspend fun findMedicationByCode(code: String): Medication? =
        medicationDao.findByCode(code)

    // ----------------
    // Important Dates
    // ----------------
    /** Insert a new important date */
    suspend fun insertImportantDate(importantDate: ImportantDate) =
        importantDateDao.insert(importantDate)

    /** Update diagnosis date */
    suspend fun updateImportantDate(importantDate: ImportantDate) =
        importantDateDao.update(importantDate)

    /** Archive appointment */
    suspend fun setArchivedImportantDate(id: Int, archived: Boolean) =
        importantDateDao.setArchived(id, archived)

    /** Flow of all diagnosis dates */
    fun getAllImportantDates(): Flow<List<ImportantDate>> =
        importantDateDao.getAllImportantDates()

    /** Delete a diagnosis date record by Id**/
    suspend fun deleteImportantDate(id: Int) = importantDateDao.deleteById(id)

    // ----------------
    // Medical Devices
    // ----------------
    /** Insert a new devices */
    suspend fun insertDevice(device: MedicalDevice) = medicalDevicesDao.insert(device)

    /** Update devices */
    suspend fun updateDevice(device: MedicalDevice) = medicalDevicesDao.update(device)

    /** Flow of all devices */
    fun getAllDevices(): Flow<List<MedicalDevice>> = medicalDevicesDao.getAllMedicalDevices()

    /** Flow of current medical devices */
    fun getAllCurrentConsumableDevices(): Flow<List<MedicalDevice>> =
        medicalDevicesDao.getAllCurrentConsumableMedicalDevices(today = LocalDate.now())

    /** Flow of all non-consumable devices */
    fun getAllNonConsumableDevices(): Flow<List<MedicalDevice>> =
        medicalDevicesDao.getAllNonConsumableDevices()

    /** Flow of all consumable devices */
    fun getAllConsumableDevices(): Flow<List<MedicalDevice>> =
        medicalDevicesDao.getAllConsumableDevices()

    /** Flow of all unreported faulty devices **/
    fun getAllUnreportedFaultyDevices(): Flow<List<MedicalDevice>> =
        medicalDevicesDao.getAllFaultyUnreportedMedicalDevices()

    /** Flow of all reported faulty devices **/
    fun getAllReportedFaultyDevices(): Flow<List<MedicalDevice>> =
        medicalDevicesDao.getAllFaultyReportedMedicalDevices()

    /** Flow of all faulty devices that have or are expiring today **/
    fun getAllFaultyDevicesExpiredToday(): List<MedicalDevice> =
        medicalDevicesDao.getSimilarExpiringConsumableDevices(
            today = LocalDate.now(),
            deviceType = MedicalDeviceInfoType.UNKNOWN
        )

    /** Flow of all faulty batch numbers **/
    fun getAllFaultyBatchNumbers(): Flow<List<FaultyBatchCount>> =
        medicalDevicesDao.getFaultyBatchNumbersCounts()

    // ----------------
    // Medical devices
    // ----------------
    /** Find a medical devices by its code (GTIN or CIP) */
    suspend fun findMedicalDeviceByCode(code: String): MedicalDeviceInfoEntity? =
        medicalDeviceInfo.findByCode(code)

    // ----------------
    // User details
    // ----------------
    /** Flow of user details */
    fun getUserDetails(): Flow<UserDetails?> = userDetailsDao.getUserDetails()

    /** Update user details */
    suspend fun updateUserDetails(userDetails: UserDetails) = userDetailsDao.upsertUserDetails(userDetails)

    /** Delete user details */
    suspend fun deleteUserDetails() = userDetailsDao.deleteUserDetails()

    /** Add profile photo path */
    suspend fun addProfilePhotoPath(path: String?) = userDetailsDao.updateProfilePhotoPath(path)

    // ----------------
    // User preferences
    // ----------------
    /** Flow of user preferences */
    suspend fun insertOrUpdate(preferences: UserPreferences) = userPreferencesDao.insertOrUpdate(preferences)
    fun getUserPreferences(): Flow<UserPreferences?> =
        userPreferencesDao.getUserPreferences()

    suspend fun setAutoBackupEnabled(enabled: Boolean) =
        userPreferencesDao.setAutoBackupEnabled(enabled)

    suspend fun setBackupFrequency(frequency: String) =
        userPreferencesDao.setFrequency(frequency)

    suspend fun setBackupPath(path: String) =
        userPreferencesDao.setBackupPath(path)

    suspend fun setLastBackupUpdate(date: String) =
        userPreferencesDao.setLastBackupDate(date)

    suspend fun restorePreferences(preferences: UserPreferences) =
        userPreferencesDao.insertOrUpdate(preferences)

    suspend fun resetBackupPreferences() =
        userPreferencesDao.resetBackupPreferences()

    /** Delete a devices record by Id**/
    suspend fun deleteDevice(id: Int) = medicalDevicesDao.deleteById(id)

    // ----------------
    // Generic / Database Utilities
    // ----------------
    /** Clear the entire database and reset autoincrement IDs */
    fun clearAllDataAndReset() = database.clearAllDataAndReset()
}