package com.diabdata.workers.scheduledBackups

import android.content.Context
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.diabdata.core.database.DataRepository
import com.diabdata.core.model.UserDetails
import com.diabdata.core.notifications.NotificationImportance
import com.diabdata.core.notifications.showNotification
import com.diabdata.shared.utils.utils.uriStringToReadablePath
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import com.diabdata.shared.R as shared

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataRepository: DataRepository
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val prefs = dataRepository.getUserPreferences().first()
                ?: return Result.failure()
            val backupPath = prefs.backupPath
                ?: return Result.failure()

            val userDetails: UserDetails? = dataRepository.getUserDetails().first()
            val profilePhotoPath = userDetails?.profilePhotoPath

            val jsonString = dataRepository.exportDataAsJsonString()

            val treeUri = backupPath.toUri()
            val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH-mm", Locale.getDefault())
            val readabbleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            val date = dateFormat.format(Date())
            val readableDate = readabbleDateFormat.format(Date())

            val fileName = "diabdata_backup_${date}.zip"

            val docUri = DocumentsContract.createDocument(
                applicationContext.contentResolver,
                DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                ),
                "application/zip",
                fileName
            ) ?: return Result.failure()

            applicationContext.contentResolver.openOutputStream(docUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zip ->
                    zip.putNextEntry(ZipEntry("data.json"))
                    zip.write(jsonString.toByteArray())
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

            dataRepository.setLastBackupUpdate(
                LocalDateTime.now().toString()
            )

            applicationContext.showNotification(
                title = applicationContext.getString(shared.string.notification_scheduled_backup_success_title),
                content = applicationContext.getString(shared.string.notification_scheduled_backup_success, backupPath.uriStringToReadablePath(applicationContext), readableDate),
                channelName = applicationContext.getString(shared.string.notification_channel_scheduled_backup),
                importance = NotificationImportance.LOW
            )

            Result.success()
        } catch (e: SecurityException) {
            Log.e("BackupWorker", "Permission denied", e)
            applicationContext.showNotification(
                title = applicationContext.getString(shared.string.notification_scheduled_backup_error_title),
                content = applicationContext.getString(shared.string.notification_scheduled_backup_error_permission_denied, e.message),
                channelName = applicationContext.getString(shared.string.notification_channel_scheduled_backup),
                importance = NotificationImportance.DEFAULT
            )
            Result.failure()
        } catch (e: Exception) {
            Log.e("BackupWorker", "Backup failed", e)
            applicationContext.showNotification(
                title = applicationContext.getString(shared.string.notification_scheduled_backup_error_title),
                content = applicationContext.getString(shared.string.notification_scheduled_backup_error, e.message),
                channelName = applicationContext.getString(shared.string.notification_channel_scheduled_backup),
                importance = NotificationImportance.DEFAULT
            )
            Result.failure()
        }
    }
}