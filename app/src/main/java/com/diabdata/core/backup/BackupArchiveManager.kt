package com.diabdata.core.backup

import android.app.Application
import android.os.Build
import android.util.Log
import com.diabdata.BuildConfig
import com.diabdata.core.database.DataRepository
import com.diabdata.core.model.BackupMetadata
import com.diabdata.core.model.DataSummary
import com.diabdata.core.model.UserDetails
import com.diabdata.core.model.UserPreferences
import com.diabdata.core.utils.data.GsonFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.java
import kotlin.time.Clock

class BackupArchiveManager @Inject constructor(
    private val repository: DataRepository, private val application: Application
) {

    private val backupManagerTag = "BackupArchiveManager"

    inline fun <reified T> Gson.toJsonList(list: List<T>): String = this.toJson(list)

    inline fun <reified T> Gson.fromJsonList(json: String): List<T> =
        this.fromJson(json, TypeToken.getParameterized(List::class.java, T::class.java).type)

    suspend fun writeBackup(
        output: OutputStream, isScheduledBackup: Boolean = false, isEncrypted: Boolean = false
    ): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val createdAt = Clock.System.now().toString()

                val weights = repository.getAllWeights().first()
                val weightCount = weights.size
                val hba1cs = repository.getAllHba1c().first()
                val hba1cCount = hba1cs.size
                val appointments = repository.getAllAppointments().first()
                val appointmentsCount = appointments.size
                val treatments = repository.getAllTreatments().first()
                val treatmentsCount = treatments.size
                val importantDates = repository.getAllImportantDates().first()
                val importantDatesCount = importantDates.size
                val medicalDevices = repository.getAllDevices().first()
                val medicalDevicesCount = medicalDevices.size
                val userDetails = repository.getUserDetails().first()
                val userPreferences = repository.getUserPreferences().first()

                val totalEntriesCount =
                    weightCount + hba1cCount + appointmentsCount + treatmentsCount + importantDatesCount + medicalDevicesCount

                val backupMetadata = BackupMetadata(
                    app = BuildConfig.APPLICATION_ID,
                    appVersion = BuildConfig.VERSION_NAME,
                    formatVersion = 1,
                    createdAt = createdAt,
                    isScheduledBackup = isScheduledBackup,
                    isEncrypted = isEncrypted,
                    deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                    dataSummary = DataSummary(
                        totalEntriesCount = totalEntriesCount,
                        weightEntriesCount = weightCount,
                        hba1cEntriesCount = hba1cCount,
                        appointmentEntriesCount = appointmentsCount,
                        treatmentEntriesCount = treatmentsCount,
                        importantDateEntriesCount = importantDatesCount,
                        medicalDeviceEntriesCount = medicalDevicesCount,
                        hasUserProfile = userDetails != null,
                        hasUserPreferences = userPreferences != null,
                        hasProfilePhoto = userDetails?.profilePhotoPath != null,
                    )
                )

                val gson = GsonFactory.create(prettyPrint = true)

                ZipOutputStream(output).use { zip ->
                    fun writeEntry(name: String, content: String) {
                        zip.putNextEntry(ZipEntry(name))
                        zip.write(content.toByteArray())
                        zip.closeEntry()
                    }

                    writeEntry("metadata.json", gson.toJson(backupMetadata))
                    writeEntry("weights.json", gson.toJsonList(weights))
                    writeEntry("hba1c.json", gson.toJsonList(hba1cs))
                    writeEntry("appointments.json", gson.toJsonList(appointments))
                    writeEntry("treatments.json", gson.toJsonList(treatments))
                    writeEntry("important_dates.json", gson.toJsonList(importantDates))
                    writeEntry("medical_devices.json", gson.toJsonList(medicalDevices))
                    userDetails?.let { writeEntry("user_profile.json", gson.toJson(it)) }
                    userPreferences?.let { writeEntry("user_preferences.json", gson.toJson(it)) }

                    userDetails?.profilePhotoPath?.let { path ->
                        val photoFile = File(path)
                        if (photoFile.exists()) {
                            zip.putNextEntry(ZipEntry("profile_photo.jpg"))
                            photoFile.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                backupManagerTag, "writeBackup Exception: ${e.message}", e
            )
            Result.failure(exception = e)
        }
    }

    suspend fun readBackup(input: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val bytes = input.readBytes()
            val isZip = bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

            val gson = GsonFactory.create()

            // Handling of legacy JSON backup files
            if (!isZip) {
                val jsonContent = String(bytes, Charsets.UTF_8)
                if (jsonContent.isEmpty()) return@withContext Result.failure(Exception("Backup is empty"))
                repository.importDataFromJsonString(jsonContent)
                return@withContext Result.success(Unit)
            }

            val entries = mutableMapOf<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    entries[entry.name] = zis.readBytes()
                    entry = zis.nextEntry
                }
            }

            // Import data from new backup Zip format
            if (entries.containsKey("metadata.json")) {
                entries["weights.json"]?.let {
                    repository.importWeights(gson.fromJsonList(it.toString(Charsets.UTF_8)))
                }
                entries["hba1c.json"]?.let {
                    repository.importHba1c(gson.fromJsonList(it.toString(Charsets.UTF_8)))
                }
                entries["appointments.json"]?.let {
                    repository.importAppointments(gson.fromJsonList(it.toString(Charsets.UTF_8)))
                }
                entries["treatments.json"]?.let {
                    repository.importTreatments(gson.fromJsonList(it.toString(Charsets.UTF_8)))
                }
                entries["important_dates.json"]?.let {
                    repository.importImportantDates(gson.fromJsonList(it.toString(Charsets.UTF_8)))
                }
                entries["medical_devices.json"]?.let {
                    repository.importMedicalDevices(gson.fromJsonList(it.toString(Charsets.UTF_8)))
                }
                entries["user_preferences.json"]?.let {
                    repository.importUserPreferences(gson.fromJson(it.toString(Charsets.UTF_8), UserPreferences::class.java))
                }
                entries["user_profile.json"]?.let {
                    val userDetails = gson.fromJson(it.toString(Charsets.UTF_8), UserDetails::class.java)
                    repository.importUserDetails(userDetails)
                }
                entries["profile_photo.jpg"]?.let { photoBytes ->
                    repository.saveProfilePhotoBytes(photoBytes, application.filesDir)
                }
            } else if (entries.containsKey("data.json")) {
                val jsonContent = entries["data.json"]!!.toString(Charsets.UTF_8)
                val photoBytes: ByteArray? = entries["profile_photo.jpg"]
                repository.importDataFromJsonString(jsonContent)
                photoBytes?.let { repository.saveProfilePhotoBytes(it, application.filesDir) }

            } else {
                Log.w(backupManagerTag, "readBackup: unrecognized ZIP content")
                return@withContext Result.failure(Exception("Backup archive is missing data.json or metadata.json"))
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(backupManagerTag, "readBackup Exception: ${e.message}", e)
            Result.failure(exception = e)
        }
    }
}