package com.diabdata.core.backup

import android.app.Application
import android.util.Log
import com.diabdata.core.database.DataRepository
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

class BackupArchiveManager @Inject constructor(
    private val repository: DataRepository, private val application: Application
) {

    private val TAG = "BackupArchiveManager"

    suspend fun writeBackup(
        output: OutputStream, isScheduledBackup: Boolean = false, isEncrypted: Boolean = false
    ): Result<Unit> {

        return try {
            val jsonData = repository.exportDataAsJsonString()
            val profilePhotoPath = repository.getUserDetails().first()?.profilePhotoPath

            withContext(Dispatchers.IO) {
                ZipOutputStream(output).use { zip ->
                    zip.putNextEntry(ZipEntry("data.json"))
                    zip.write(jsonData.toByteArray())
                    zip.closeEntry()
                    profilePhotoPath?.let { path ->
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
                TAG, "writeBackup Exception: ${e.message}", e
            )
            Result.failure(exception = e)
        }
    }

    suspend fun readBackup(input: InputStream): Result<Unit> {
        return try {
            val bytes = input.readBytes()
            val isZip = bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

            val jsonContent: String
            var photoBytes: ByteArray? = null

            if (isZip) {
                var extractedJson: String? = null
                ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        when (entry.name) {
                            "data.json" -> extractedJson = zis.readBytes().toString(Charsets.UTF_8)
                            "profile_photo.jpg" -> photoBytes = zis.readBytes()
                            else -> Log.w(TAG, "Unknown entry in backup archive: ${entry.name}")
                        }
                        entry = zis.nextEntry
                    }
                }
                jsonContent = extractedJson
                    ?: run {
                        Log.e(TAG, "readBackup: Backup archive is missing data.json")
                        return Result.failure(Exception("Backup archive is missing data.json"))
                    }
            } else {
                jsonContent = String(bytes, Charsets.UTF_8)
                if (jsonContent.isEmpty()) {
                    return Result.failure(Exception("Backup is empty"))
                }
            }

            repository.importDataFromJsonString(jsonContent)

            photoBytes?.let {
                val path = repository.saveProfilePhotoBytes(it, application.filesDir)
                Log.d(TAG, "saveProfilePhotoBytes returned path=$path")
            }

            val checkPath = repository.getUserDetails().first()?.profilePhotoPath
            Log.d(TAG, "UserDetails.profilePhotoPath after import = $checkPath")

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "readBackup Exception: ${e.message}", e)
            Result.failure(exception = e)
        }
    }
}