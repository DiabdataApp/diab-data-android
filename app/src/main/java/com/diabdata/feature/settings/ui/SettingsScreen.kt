package com.diabdata.feature.settings.ui

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.work.WorkManager
import com.diabdata.BuildConfig
import com.diabdata.core.database.DataViewModel
import com.diabdata.core.ui.components.cardsList.CardItem
import com.diabdata.core.ui.components.cardsList.CardListItem
import com.diabdata.core.ui.components.cardsList.CardsList
import com.diabdata.core.ui.theme.GoogleSansFlexFontFamily
import com.diabdata.core.utils.ui.ColoredIconCircle
import com.diabdata.core.utils.ui.SvgIcon
import com.diabdata.feature.settings.SettingsViewModel
import com.diabdata.feature.settings.ui.components.changelog.ChangelogDialog
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

    val nextAppointmentDate by dataViewModel.upcomingAppointment
        .map { appointments ->
            appointments.minByOrNull { it.date }?.date
        }
        .collectAsState(initial = null)

    val nextTreatmentExpirationDate by dataViewModel.upcomingExpiringTreatmentDates
        .map { treatments ->
            treatments.minByOrNull { it.expirationDate }?.expirationDate
        }
        .collectAsState(initial = null)

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
            Card(
                colors = CardDefaults.cardColors(
                    MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                    horizontalArrangement = spacedBy(10.dp),
                ) {
                    ColoredIconCircle(
                        baseColor = if (isBeta) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        iconRes = if (isBeta) {
                            shared.drawable.ic_logo_outlined
                        } else {
                            shared.drawable.ic_logo_filled
                        },
                        size = 38.dp,
                        iconSize = 28.dp
                    )
                    Column {
                        Text(
                            text = stringResource(shared.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = GoogleSansFlexFontFamily,
                            fontWeight = FontWeight(1000),
                            fontStyle = Italic
                        )
                        Text(
                            text = stringResource(shared.string.app_tagline),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = GoogleSansFlexFontFamily,
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier
                        .padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
                    horizontalArrangement = spacedBy(8.dp),
                ) {
                    val uriHandler = LocalUriHandler.current

                    AssistChip(
                        label = { Text("v$versionName${if (isBeta) "-beta" else "" }") },
                        leadingIcon = {
                            SvgIcon(
                                resId = shared.drawable.app_version_filled_icon_vector,
                                modifier = Modifier
                                    .size(18.dp)
                                    .border(
                                        0.dp,
                                        Color.Transparent,
                                        shape = RoundedCornerShape(50)
                                    ),
                                color = if (isBeta) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                }
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isBeta) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            labelColor = if (isBeta) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            }
                        ),
                        onClick = {},
                    )

                    AssistChip(
                        onClick = {
                            uriHandler.openUri(
                                "https://github.com/DiabdataApp/diab-data-android/releases/tag/v$versionName"
                            )
                        },
                        label = { Text("Github") },
                        leadingIcon = {
                            SvgIcon(
                                resId = shared.drawable.github_icon_vector,
                                modifier = Modifier
                                    .size(18.dp)
                                    .border(
                                        0.dp,
                                        Color.Transparent,
                                        shape = RoundedCornerShape(50)
                                    ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                    )

                    AssistChip(
                        onClick = { showChangeLogDialog = true },
                        label = { Text(stringResource(shared.string.settings_section_changelogs_label)) },
                        leadingIcon = {
                            SvgIcon(
                                resId = shared.drawable.breaking_new_filled_icon_vector,
                                modifier = Modifier
                                    .size(18.dp)
                                    .border(
                                        0.dp,
                                        Color.Transparent,
                                        shape = RoundedCornerShape(50)
                                    ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                    )

                    AssistChip(
                        onClick = {
                            uriHandler.openUri("https://app.diabdata.fr/")
                        },
                        label = { Text(stringResource(shared.string.settings_section_website_label)) },
                        leadingIcon = {
                            SvgIcon(
                                resId = shared.drawable.arrow_outward_icon_vector,
                                modifier = Modifier
                                    .size(18.dp)
                                    .border(
                                        0.dp,
                                        Color.Transparent,
                                        shape = RoundedCornerShape(50)
                                    ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                    )
                }
            }

            val toastExpirationEnabled =
                stringResource(shared.string.toast_expiration_reminders_enabled)
            val toastAppointmentReminderEnabled =
                stringResource(shared.string.toast_appointment_reminders_enabled)

            val notificationSection: List<CardItem> = listOf(
                CardItem(
                    leadingIcon = shared.drawable.medication_expiry_notification_icon_vector,
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
                    leadingIcon = shared.drawable.event_notification_icon_vector,
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
                    leadingIcon = shared.drawable.medication_info_icon_vector,
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
                    leadingIcon = shared.drawable.medical_device_info_version_icon_vector,
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
                    leadingIcon = shared.drawable.database_icon_vector,
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