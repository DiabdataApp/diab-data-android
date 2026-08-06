package com.diabdata.core.backup

import android.app.Application
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
import com.diabdata.shared.utils.dataTypes.BackupFrequency
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.lingala.zip4j.exception.ZipException
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
    private val backupEncryptionKeyManager: BackupEncryptionKeyManager,
    private val backupScheduleCoordinator: BackupScheduleCoordinator
) {
    private val backupManagerTag = "BackupArchiveManager"
    private val gson = GsonFactory.create(prettyPrint = true)

    inline fun <reified T> Gson.toJsonList(list: List<T>): String = this.toJson(list)

    inline fun <reified T> Gson.fromJsonList(json: String): List<T> =
        this.fromJson(json, TypeToken.getParameterized(List::class.java, T::class.java).type)

    sealed class BackupFormat {
        data class NewFormat(val metadata: BackupMetadata) : BackupFormat()
        object LegacyZip : BackupFormat()
        object LegacyRawJson : BackupFormat()
        object Invalid : BackupFormat()
    }

    private fun detectBackupFormat(byteArray: ByteArray): BackupFormat {
        val isZip = byteArray.size >= 2 && byteArray[0] == 0x50.toByte() && byteArray[1] == 0x4B.toByte()
        if (!isZip) {
            return if (byteArray.isNotEmpty()) BackupFormat.LegacyRawJson else BackupFormat.Invalid
        }

        var metadata: BackupMetadata? = null
        val entryNames = mutableListOf<String>()

        ZipInputStream(ByteArrayInputStream(byteArray)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entryNames += entry.fileName

                if (entry.fileName == "metadata.json") {
                    metadata = gson.fromJson(zip.readBytes().toString(Charsets.UTF_8), BackupMetadata::class.java)
                }

                entry = zip.nextEntry
            }
        }

        return when {
            metadata != null -> BackupFormat.NewFormat(metadata)
            "data.json" in entryNames -> BackupFormat.LegacyZip
            else -> BackupFormat.Invalid
        }
    }

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

    suspend fun readBackup(input: InputStream, password: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val bytes = input.readBytes()
            val backupType = detectBackupFormat(bytes)

            when (backupType) {
                is BackupFormat.LegacyRawJson -> legacyJsonImport(String(bytes, Charsets.UTF_8))
                is BackupFormat.LegacyZip -> legacyZipImport(bytes)
                is BackupFormat.NewFormat -> zipImport(bytes, password)
                is BackupFormat.Invalid -> Result.failure(Exception("Unsupported backup format"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(backupManagerTag, "readBackup Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun legacyJsonImport(jsonContent: String): Result<Unit> {
        return try {
            if (jsonContent.isEmpty()) return Result.failure(Exception("Backup is empty"))

            repository.importDataFromJsonString(jsonContent)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(backupManagerTag, "legacyJsonImport Exception: ${e.message}", e)
            Result.failure(exception = e)
        }
    }

    private suspend fun legacyZipImport(input: ByteArray): Result<Unit> {
        return try {
            val entries = mutableMapOf<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(input)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    entries[entry.fileName] = zis.readBytes()
                    entry = zis.nextEntry
                }
            }

            val jsonContent = entries["data.json"]!!.toString(Charsets.UTF_8)
            val photoBytes: ByteArray? = entries["profile_photo.jpg"]

            repository.importDataFromJsonString(jsonContent)
            photoBytes?.let { repository.saveProfilePhotoBytes(it, application.filesDir) }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(backupManagerTag, "legacyZipImport Exception: ${e.message}", e)
            Result.failure(exception = e)
        }
    }

    // New zip format handling
    private val importHandlers: Map<String, suspend (ByteArray) -> Unit> = mapOf(
        "weights.json" to { bytes -> repository.importWeights(gson.fromJsonList(bytes.toString(Charsets.UTF_8))) },
        "hba1c.json" to { bytes -> repository.importHba1c(gson.fromJsonList(bytes.toString(Charsets.UTF_8))) },
        "appointments.json" to { bytes -> repository.importAppointments(gson.fromJsonList(bytes.toString(Charsets.UTF_8))) },
        "treatments.json" to { bytes -> repository.importTreatments(gson.fromJsonList(bytes.toString(Charsets.UTF_8))) },
        "important_dates.json" to { bytes -> repository.importImportantDates(gson.fromJsonList(bytes.toString(Charsets.UTF_8))) },
        "medical_devices.json" to { bytes -> repository.importMedicalDevices(gson.fromJsonList(bytes.toString(Charsets.UTF_8))) },
        "user_preferences.json" to { bytes -> repository.importUserPreferences(gson.fromJson(bytes.toString(Charsets.UTF_8), UserPreferences::class.java)) },
        "user_profile.json" to { bytes -> repository.importUserDetails(gson.fromJson(bytes.toString(Charsets.UTF_8), UserDetails::class.java)) },
        "profile_photo.jpg" to { bytes -> repository.saveProfilePhotoBytes(bytes, application.filesDir) }
    )

    private suspend fun zipImport(input: ByteArray, password: String? = null): Result<Unit> {
        return try {
            val entries = mutableMapOf<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(input), password?.toCharArray()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    entries[entry.fileName] = zis.readBytes()
                    entry = zis.nextEntry
                }
            }

            importHandlers.forEach { (fileName, handler) ->
                entries[fileName]?.let { handler(it) }
            }

            val importedPrefs = repository.getUserPreferences().first()

            importedPrefs?.let {
                val scheduleResult = backupScheduleCoordinator.applyBackupSchedule(it.automaticBackupEnabled, BackupFrequency.fromKey(it.frequency), it.backupPath)
                scheduleResult.onSuccess { didSchedule ->
                    if (it.automaticBackupEnabled && !didSchedule) {
                        repository.setAutoBackupEnabled(false)
                    }
                }
            }

            Result.success(Unit)
        } catch (e: ZipException) {
            if (e.type == ZipException.Type.WRONG_PASSWORD) return Result.failure(Exception("Wrong backup password"))
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(backupManagerTag, "zipImport Exception: ${e.message}", e)
            Result.failure(exception = e)
        }
    }
}