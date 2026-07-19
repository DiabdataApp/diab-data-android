package com.diabdata.feature.settings.dataSettingsSection.ui

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.diabdata.core.database.DataViewModel
import com.diabdata.core.notifications.showNotification
import com.diabdata.core.ui.LocalSnackbarHostState
import com.diabdata.core.ui.components.cardsList.CardItem
import com.diabdata.core.ui.components.cardsList.CardsList
import com.diabdata.core.utils.ui.SvgIcon
import com.diabdata.feature.settings.ImExViewModel
import com.diabdata.feature.settings.dataSettingsSection.BackupViewModel
import com.diabdata.feature.settings.dataSettingsSection.ui.components.AutoBackupCard
import com.diabdata.feature.userProfile.UserProfileViewModel
import com.diabdata.shared.utils.dataTypes.BackupFrequency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import com.diabdata.shared.R as shared

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSettingsScreen(
    dataViewModel: DataViewModel
) {
    val dateFormat = SimpleDateFormat("dd-MM-yyyy", LocalLocale.current.platformLocale)
    val currentDate = dateFormat.format(Date())
    val fileName = "diabdata_export_$currentDate.zip"
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val imExViewModel: ImExViewModel = hiltViewModel()
    val userProfileViewModel: UserProfileViewModel = hiltViewModel()

    var showConfirmDialog by remember { mutableStateOf(false) }

    val notifChannelName = stringResource(shared.string.notification_channel_data)
    val dataExportSuccess = stringResource(shared.string.toast_data_export_success)
    val dataImportSuccess = stringResource(shared.string.toast_data_import_success)
    val dataExportError = stringResource(shared.string.toast_data_export_error)
    val dataImportError = stringResource(shared.string.toast_data_import_error)
    val emptyImportFileError = stringResource(shared.string.toast_empty_file_error)

    val backupViewModel: BackupViewModel = hiltViewModel()
    val backupPrefs by backupViewModel.preferences.collectAsState()

    val resetMessage = stringResource(shared.string.settings_backup_policy_reset_snackbar)
    val undoLabel = stringResource(shared.string.common_undo)

    val snackbarHostState = LocalSnackbarHostState.current

    // ── Export launcher ──
    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri: Uri? ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    val profilePhotoPath = userProfileViewModel.getProfilePhotoPath()
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            ZipOutputStream(outputStream).use { zip ->
                                val jsonString = imExViewModel.exportDataAsJsonString()
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
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, dataExportSuccess, Toast.LENGTH_SHORT).show()
                        }
                        context.showNotification(
                            title = dataExportSuccess,
                            content = uri.lastPathSegment.orEmpty(),
                            channelName = notifChannelName,
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        context.showNotification(
                            title = "$dataExportError : ${e.message}",
                            content = uri.lastPathSegment.orEmpty(),
                            channelName = notifChannelName,
                        )
                    }
                }
            }
        }
    )

    // ── Import launcher ──
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val bytes = inputStream.readBytes()
                            val isZip = bytes.size >= 2
                                    && bytes[0] == 0x50.toByte()
                                    && bytes[1] == 0x4B.toByte()
                            if (isZip) {
                                var jsonString: String? = null
                                var photoBytes: ByteArray? = null
                                ZipInputStream(bytes.inputStream()).use { zip ->
                                    var entry = zip.nextEntry
                                    while (entry != null) {
                                        when (entry.name) {
                                            "data.json" -> jsonString = String(zip.readBytes())
                                            "profile_photo.jpg" -> photoBytes = zip.readBytes()
                                        }
                                        zip.closeEntry()
                                        entry = zip.nextEntry
                                    }
                                }
                                var newPhotoPath: String? = null
                                photoBytes?.let { pBytes ->
                                    val photoFile = File(
                                        context.filesDir,
                                        "profile_photo_${System.currentTimeMillis()}.jpg"
                                    )
                                    photoFile.outputStream().use { output ->
                                        output.write(pBytes)
                                    }
                                    context.filesDir.listFiles()
                                        ?.filter {
                                            it.name.startsWith("profile_photo")
                                                    && it.name != photoFile.name
                                        }
                                        ?.forEach { it.delete() }
                                    newPhotoPath = photoFile.absolutePath
                                }
                                jsonString?.let { json ->
                                    if (json.isNotEmpty()) {
                                        imExViewModel.importDataFromJsonString(json, newPhotoPath)
                                    }
                                }
                            } else {
                                val jsonString = String(bytes)
                                if (jsonString.isNotEmpty()) {
                                    imExViewModel.importDataFromJsonString(jsonString)
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            emptyImportFileError,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    return@use
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, dataImportSuccess, Toast.LENGTH_SHORT)
                                    .show()
                            }
                            context.showNotification(
                                title = dataImportSuccess,
                                content = uri.lastPathSegment.orEmpty(),
                                channelName = notifChannelName,
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("Import", "GLOBAL CRASH", e)
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "$dataImportError : ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    )

    // ── UI ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(top = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = spacedBy(32.dp)
    ) {
        val dataBaseSection: List<CardItem> = listOf(
            CardItem(
                leadingIcon = shared.drawable.backup_db_icon_vector,
                content = {
                    Row { Text(stringResource(shared.string.settings_export_data)) }
                },
                onClick = { createFileLauncher.launch(fileName) },
                trailingIcon = shared.drawable.arrow_right_icon
            ),
            CardItem(
                leadingIcon = shared.drawable.restore_db_icon_vector,
                content = {
                    Row { Text(stringResource(shared.string.settings_import_data)) }
                },
                onClick = {
                    importFileLauncher.launch(
                        arrayOf("application/json", "application/zip")
                    )
                },
                trailingIcon = shared.drawable.arrow_right_icon
            ),
            CardItem(
                leadingIcon = shared.drawable.purge_db_icon_vector,
                leadingIconColor = MaterialTheme.colorScheme.error,
                isDestructive = true,
                content = {
                    Row { Text(stringResource(shared.string.settings_purge_database)) }
                },
                onClick = { showConfirmDialog = true },
                trailingIcon = shared.drawable.arrow_right_icon
            )
        )

        AutoBackupCard(
            enabled = backupPrefs?.automaticBackupEnabled ?: false,
            onEnabledChange = { backupViewModel.setAutoBackupEnabled(it) },
            frequency = BackupFrequency.fromKey(backupPrefs?.frequency ?: "weekly"),
            onFrequencyChange = { backupViewModel.setFrequency(it) },
            backupPath = backupPrefs?.backupPath,
            onPathChange = { backupViewModel.setBackupPath(it) },
            lastBackupDate = backupPrefs?.lastBackupDate,
            onResetButtonClick = {
                Log.d("BackupReset", "1. Click - backupPrefs: $backupPrefs")
                scope.launch {
                    val backup = backupPrefs ?: run {
                        Log.d("BackupReset", "2. backupPrefs is NULL, aborting")
                        return@launch
                    }
                    Log.d("BackupReset", "3. Backup saved: $backup")
                    backupViewModel.resetBackupPreferences()
                    Log.d("BackupReset", "4. Reset called, showing snackbar...")
                    try {
                        val result = snackbarHostState.showSnackbar(
                            message = resetMessage,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Short
                        )
                        Log.d("BackupReset", "5. Snackbar result: $result")
                        if (result == SnackbarResult.ActionPerformed) {
                            backupViewModel.restorePreferences(backup)
                            Log.d("BackupReset", "6. Preferences restored")
                        }
                    } catch (e: Exception) {
                        Log.e("BackupReset", "Snackbar error", e)
                    }
                }
            }
        )

        CardsList(
            cards = dataBaseSection
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                SvgIcon(
                    resId = shared.drawable.purge_db_icon_vector,
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(shared.string.dialog_purge_title)) },
            text = { Text(stringResource(shared.string.dialog_purge_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        dataViewModel.clearDatabase(context)
                        showConfirmDialog = false
                    }
                ) {
                    Text(
                        stringResource(shared.string.common_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(shared.string.common_cancel))
                }
            }
        )
    }
}