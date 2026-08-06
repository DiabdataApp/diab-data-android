package com.diabdata.core.backup

import android.app.Application
import android.app.backup.BackupManager
import android.os.Build
import android.util.Log
import com.diabdata.BuildConfig
import com.diabdata.core.backup.encryption.BackupEncryptionKeyManager
import com.diabdata.core.database.DataRepository
import com.diabdata.core.model.BackupMetadata
import com.diabdata.core.model.DataSummary
import com.diabdata.core.model.UserDetails
import com.diabdata.core.model.UserPreferences
import com.diabdata.core.utils.data.GsonFactory
import com.diabdata.feature.settings.sections.dataSettings.workers.BackupScheduler
import com.diabdata.shared.utils.dataTypes.BackupFrequency
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

class BackupArchiveManager @Inject constructor(
    private val repository: DataRepository,
    private val application: Application,
    private val backupEncryptionKeyManager: BackupEncryptionKeyManager
) {
    private val backupManagerTag = "BackupArchiveManager"

    inline fun <reified T> Gson.toJsonList(list: List<T>): String = this.toJson(list)

    inline fun <reified T> Gson.fromJsonList(json: String): List<T> =
        this.fromJson(json, TypeToken.getParameterized(List::class.java, T::class.java).type)

    suspend fun writeBackup(
        output: OutputStream, isScheduledBackup: Boolean = false, isEncrypted: Boolean = false
    ): Result<Unit> {
        var encryptionKey: CharArray? = null

        return try {
            withContext(Dispatchers.IO) {

                if (isEncrypted) {
                    encryptionKey = backupEncryptionKeyManager.getPassword().let {
                        if (it.isSuccess) {
                            if (it.getOrNull() == null) {
                                throw Exception("Unable to get encryption key expected non-null")
                            } else {
                                it.getOrNull()!!.toCharArray()
                            }
                        } else {
                            throw Exception("Unable to get encryption key: ${it.exceptionOrNull()?.message}")
                        }
                    }
                }

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
                    formatVersion = 2,
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

                val plainFiles: List<Triple<String, ByteArray, Boolean>> = buildList {
                    add(Triple("metadata.json", gson.toJson(backupMetadata).toByteArray(), false))
                }

                val photoToEncrypt: Triple<String, ByteArray, Boolean>? = userDetails?.profilePhotoPath?.let { path ->
                    val photoFile = File(path)
                    if (photoFile.exists()) {
                        Triple("profile_photo.jpg", photoFile.readBytes(), isEncrypted)
                    } else {
                        null
                    }
                }

                val filesToEncrypt: List<Triple<String, ByteArray, Boolean>> = buildList {
                    add(Triple("weights.json", gson.toJsonList(weights).toByteArray(), isEncrypted))
                    add(Triple("hba1c.json", gson.toJsonList(hba1cs).toByteArray(), isEncrypted))
                    add(Triple("appointments.json", gson.toJsonList(appointments).toByteArray(), isEncrypted))
                    add(Triple("treatments.json", gson.toJsonList(treatments).toByteArray(), isEncrypted))
                    add(Triple("important_dates.json", gson.toJsonList(importantDates).toByteArray(), isEncrypted))
                    add(Triple("medical_devices.json", gson.toJsonList(medicalDevices).toByteArray(), isEncrypted))
                    userDetails?.let {
                        add(Triple("user_profile.json", gson.toJson(it).toByteArray(), isEncrypted))
                    }
                    userPreferences?.let {
                        add(Triple("user_preferences.json", gson.toJson(it).toByteArray(), isEncrypted))
                    }
                }

                val fileList: List<Triple<String, ByteArray, Boolean>> = buildList {
                    addAll(plainFiles)
                    photoToEncrypt?.let { add(it) }
                    addAll(filesToEncrypt)
                }

                ZipOutputStream(output, encryptionKey).use { zip ->
                    fileList.forEach {
                        zip.putNextEntry(ZipParameters().apply {
                            fileNameInZip = it.first
                            isEncryptFiles = it.third
                            if (it.third) {
                                encryptionMethod = EncryptionMethod.AES
                                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                            }
                        })
                        zip.write(it.second)
                        zip.closeEntry()
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
                    entries[entry.fileName] = zis.readBytes()
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

            val importedPrefs = repository.getUserPreferences().first()
            importedPrefs?.let {
                if (it.frequency.isNotEmpty() && it.automaticBackupEnabled) {
                    BackupScheduler.cancel(application)
                    BackupScheduler.scheduleFromUser(
                        context = application,
                        BackupFrequency.fromKey(it.frequency)
                    )
                } else {
                    BackupScheduler.cancel(application)
                }
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