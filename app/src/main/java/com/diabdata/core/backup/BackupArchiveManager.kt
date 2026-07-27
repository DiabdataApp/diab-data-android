package com.diabdata.core.backup

import android.util.Log
import com.diabdata.core.database.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class BackupArchiveManager @Inject constructor(private val repository: DataRepository) {

    suspend fun writeBackup(output: OutputStream, isScheduledBackup: Boolean = false, isEncrypted: Boolean = false): Result<Unit> {

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
                "BAM - writeBackup IOException",
                e.message,
                e
            )
            Result.failure(exception = e)
        }
    }

//    fun readBackup(input: InputStream) {
//         1. Sniff des magic bytes -> ZIP ou JSON brut
//         2. Si ZIP : lister les entrées
//            - metadata.json présent -> nouveau format, parser fichier par fichier
//            - data.json seul -> format legacy, parser comme un seul bloc (ExportData)
//         3. Si JSON brut -> parser comme legacy direct
//         4. Retourner un résultat unifié que l'appelant utilise pour insérer en DB
//    }
}