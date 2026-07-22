package com.diabdata.feature.settings.ui

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.work.WorkManager
import com.diabdata.BuildConfig
import com.diabdata.core.database.DataViewModel
import com.diabdata.core.ui.components.cardsList.CardItem
import com.diabdata.core.ui.components.cardsList.CardListItem
import com.diabdata.core.ui.components.cardsList.CardsList
import com.diabdata.core.utils.ui.ColoredIconCircleProps
import com.diabdata.feature.settings.SettingsViewModel
import com.diabdata.feature.settings.ui.components.AppInfoCard
import com.diabdata.feature.settings.ui.components.changelog.ChangelogDialog
import com.diabdata.shared.theme.DataIconColor
import com.diabdata.shared.theme.GtinFilesIconColor
import com.diabdata.shared.theme.NotificationIconColor
import com.diabdata.workers.reminders.scheduleAppointmentReminders
import com.diabdata.workers.reminders.scheduleMedicationExpirationReminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.diabdata.shared.R as shared

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun SettingsScreen(
    dataViewModel: DataViewModel,
    onNavigateToDataSettings: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val versionName = BuildConfig.VERSION_NAME

    @Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
    val isBeta = BuildConfig.APP_VARIANT == "development"

    val medicationsGtinFileVersion = BuildConfig.MEDICATION_GTIN_FILE_VERSION
    val medicalDeviceGtinFileVersion = BuildConfig.MEDICAL_DEVICES_GTIN_FILE_VERSION

    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val scope = rememberCoroutineScope()

    var showChangeLogDialog by remember { mutableStateOf(false) }

    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var enableExpirationDateReminder by remember {
        mutableStateOf(prefs.getBoolean("expiration_reminder", false))
    }
    var enableAppointmentReminder by remember {
        mutableStateOf(prefs.getBoolean("appointment_reminder", false))
    }

    val medicationStoreRebuiltText = stringResource(shared.string.medication_store_rebuilt_toast)
    val medicalDevicesStoreRebuiltText =
        stringResource(shared.string.medical_devices_store_rebuilt_toast)

    val nextAppointmentDate by remember {
        dataViewModel.upcomingAppointment
            .map { appointments ->
                appointments.minByOrNull { it.date }?.date
            }
    }.collectAsState(initial = null)

    val nextTreatmentExpirationDate by remember {
        dataViewModel.upcomingExpiringTreatmentDates
            .map { treatments ->
                treatments.minByOrNull { it.expirationDate }?.expirationDate
            }
    }.collectAsState(initial = null)

    val iconCircleProps = ColoredIconCircleProps(
        baseColor = NotificationIconColor,
        iconRes = shared.drawable.notification_filled_icon_vector,
        size = null,
        iconSize = null
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp, end = 16.dp, top = 0.dp, bottom = 0.dp
                )
                .background(Color.Transparent)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = spacedBy(32.dp)
        ) {
            @Suppress("KotlinConstantConditions")
            AppInfoCard(
                isBeta = isBeta,
                versionName = versionName,
                showChangeLogDialog = { showChangeLogDialog = true }
            )

            val toastExpirationEnabled =
                stringResource(shared.string.toast_expiration_reminders_enabled)
            val toastAppointmentReminderEnabled =
                stringResource(shared.string.toast_appointment_reminders_enabled)

            val notificationSection: List<CardItem> = listOf(
                CardItem(
                    leadingColoredCircleIcon = iconCircleProps,
                    content = {
                        val displayText = if (nextTreatmentExpirationDate != null) stringResource(
                            shared.string.settings_notification_next_expiration_reminder,
                            nextTreatmentExpirationDate!!.format(
                                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                            )
                        ) else ""

                        Column {
                            Text(
                                text = stringResource(shared.string.notification_expiration_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    switchState = enableExpirationDateReminder,
                    onSwitchChange = { isChecked ->
                        enableExpirationDateReminder = isChecked
                        prefs.edit { putBoolean("expiration_reminder", isChecked) }
                        val workManager = WorkManager.getInstance(context)
                        if (isChecked) {
                            scope.launch {
                                scheduleMedicationExpirationReminders(
                                    context,
                                    dataViewModel
                                )
                            }
                            Toast.makeText(context, toastExpirationEnabled, Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            workManager.cancelAllWorkByTag("treatments")
                        }
                    },
                    trailingIcon = shared.drawable.notification_filled_icon_vector
                ),
                CardItem(
                    leadingColoredCircleIcon = iconCircleProps.copy(
                        iconRes = shared.drawable.event_notification_icon_vector
                    ),
                    content = {
                        val displayText = if (nextAppointmentDate != null) stringResource(
                            shared.string.settings_notification_next_appointment_reminder,
                            nextAppointmentDate!!.format(
                                DateTimeFormatter.ofLocalizedDateTime(
                                    FormatStyle.MEDIUM,
                                    FormatStyle.SHORT
                                )
                            )
                        ) else ""

                        Column {
                            Text(
                                text = stringResource(shared.string.settings_notification_appointment),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    switchState = enableAppointmentReminder,
                    onSwitchChange = { isChecked ->
                        enableAppointmentReminder = isChecked
                        prefs.edit { putBoolean("appointment_reminder", isChecked) }
                        val workManager = WorkManager.getInstance(context)
                        if (isChecked) {
                            scope.launch {
                                scheduleAppointmentReminders(
                                    context,
                                    dataViewModel
                                )
                            }
                            Toast.makeText(
                                context,
                                toastAppointmentReminderEnabled,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            workManager.cancelAllWorkByTag("appointments")
                        }
                    },
                    trailingIcon = shared.drawable.notification_filled_icon_vector
                )
            )

            val aboutApplicationSection: List<CardItem> = listOf(
                CardItem(
                    leadingColoredCircleIcon = iconCircleProps.copy(
                        baseColor = GtinFilesIconColor,
                        iconRes = shared.drawable.medication_info_icon_vector
                    ),
                    content = {
                        Row {
                            Text("Medication information file version $medicationsGtinFileVersion")
                        }
                    },
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            settingsViewModel.forceRebuildMedications()

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    medicationStoreRebuiltText,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    trailingIcon = shared.drawable.refresh_icon_vector
                ),
                CardItem(
                    leadingColoredCircleIcon = iconCircleProps.copy(
                        baseColor = GtinFilesIconColor,
                        iconRes = shared.drawable.medical_device_info_version_icon_vector
                    ),
                    content = {
                        Row {
                            Text("Medical devices information file version $medicalDeviceGtinFileVersion")
                        }
                    },
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            settingsViewModel.forceRebuildMedicalDevices()

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    medicalDevicesStoreRebuiltText,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    trailingIcon = shared.drawable.refresh_icon_vector
                )
            )

            // Database section
            CardListItem(
                CardItem(
                    leadingColoredCircleIcon = iconCircleProps.copy(
                        baseColor = DataIconColor,
                        iconRes = shared.drawable.database_icon_vector
                    ),
                    content = {
                        Column {
                            Text(
                                text = stringResource(shared.string.settings_section_data),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(shared.string.settings_data_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = onNavigateToDataSettings,
                    trailingIcon = shared.drawable.arrow_right_icon
                ),
                shape = RoundedCornerShape(20.dp)
            )

            // Notification section
            CardsList(
                cards = notificationSection
            )

            // About app section
            CardsList(
                cards = aboutApplicationSection
            )
        }
    }

    if (showChangeLogDialog) {
        ChangelogDialog(onDismiss = { showChangeLogDialog = false })
    }
}