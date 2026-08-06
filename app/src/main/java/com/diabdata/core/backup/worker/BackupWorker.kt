package com.diabdata.core.backup.worker

import android.content.Context
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.diabdata.core.backup.BackupArchiveManager
import com.diabdata.core.database.DataRepository
import com.diabdata.core.notifications.NotificationImportance
import com.diabdata.core.notifications.showNotification
import com.diabdata.shared.R
import com.diabdata.shared.utils.utils.uriStringToReadablePath
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataRepository: DataRepository,
    private val backupArchiveManager: BackupArchiveManager
) : CoroutineWorker(appContext, workerParams) {
    private val tag = "BackupWorker"

    private fun failWithNotification (technicalMessage: String, localisedString: Int): Result {
        Log.w(tag, technicalMessage)
        applicationContext.showNotification(
            title = applicationContext.getString(R.string.settings_notifications_scheduled_backup_error_title),
            content = applicationContext.getString(localisedString),
            channelName = applicationContext.getString(R.string.settings_notifications_scheduled_data_backup_channel_name),
            importance = NotificationImportance.DEFAULT
        )

        return Result.failure()
    }

    override suspend fun doWork(): Result {
        return try {
            val prefs = dataRepository.getUserPreferences().first() ?: run {
                return failWithNotification(
                    "No preferences found, skipping",
                    R.string.settings_notifications_scheduled_backup_no_preferences_error
                )
            }

            val backupPath = prefs.backupPath ?: run {
                return failWithNotification(
                    "No backup path configured, skipping",
                    R.string.settings_notifications_scheduled_backup_undefined_backup_directory_error
                )
            }

            val isEncrypted = dataRepository.isBackupEncryptionEnabled()

            val treeUri = backupPath.toUri()
            val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH-mm", Locale.getDefault())
            val readableDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            val date = dateFormat.format(Date())
            val readableDate = readableDateFormat.format(Date())

            val fileName = "diabdata_backup_${date}.zip"

            val docUri = DocumentsContract.createDocument(
                applicationContext.contentResolver,
                DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                ),
                "application/zip",
                fileName
            ) ?: run {
                return failWithNotification(
                    "createDocument returned null",
                    R.string.settings_notifications_scheduled_backup_file_creation_error
                )
            }

            val outputStream = applicationContext.contentResolver.openOutputStream(docUri)
                ?: run {
                    return failWithNotification(
                        "openOutputStream returned null",
                        R.string.settings_notifications_scheduled_backup_file_access_error
                    )
                }

            outputStream.use {
                backupArchiveManager.writeBackup(it, isScheduledBackup = true, isEncrypted = isEncrypted).getOrThrow()
            }

            dataRepository.setLastBackupUpdate(
                LocalDateTime.now().toString()
            )

            applicationContext.showNotification(
                title = applicationContext.getString(R.string.settings_notifications_scheduled_backup_success_title),
                content = applicationContext.getString(
                    R.string.settings_notifications_scheduled_backup_success,
                    backupPath.uriStringToReadablePath(applicationContext),
                    readableDate
                ),
                channelName = applicationContext.getString(R.string.settings_notifications_scheduled_data_backup_channel_name),
                importance = NotificationImportance.LOW
            )

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            Log.e(tag, "Permission denied", e)
            applicationContext.showNotification(
                title = applicationContext.getString(R.string.settings_notifications_scheduled_backup_error_title),
                content = applicationContext.getString(
                    R.string.settings_notifications_scheduled_backup_error_permission_denied,
                    e.message.toString()
                ),
                channelName = applicationContext.getString(R.string.settings_notifications_scheduled_data_backup_channel_name),
                importance = NotificationImportance.DEFAULT
            )
            Result.failure()
        } catch (e: Exception) {
            Log.e(tag, "Backup failed", e)
            val errorDetail =
                "${e.javaClass.simpleName}: ${e.message}\nat ${e.stackTrace.firstOrNull()}"
            applicationContext.showNotification(
                title = applicationContext.getString(R.string.settings_notifications_scheduled_backup_error_title),
                content = errorDetail,
                channelName = applicationContext.getString(R.string.settings_notifications_scheduled_data_backup_channel_name),
                importance = NotificationImportance.DEFAULT
            )

            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}