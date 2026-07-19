package com.diabdata.feature.settings.dataSettingsSection.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.diabdata.core.ui.components.cardsList.CardItem
import com.diabdata.core.ui.components.cardsList.CardListItem
import com.diabdata.core.ui.theme.DiabDataTheme
import com.diabdata.core.utils.ui.SvgIcon
import com.diabdata.shared.utils.dataTypes.BackupFrequency
import com.diabdata.shared.utils.utils.uriStringToReadablePath
import com.diabdata.shared.R as shared

@Composable
fun AutoBackupCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    frequency: BackupFrequency? = null,
    onFrequencyChange: (BackupFrequency) -> Unit,
    backupPath: String?,
    onPathChange: (String) -> Unit,
    lastBackupDate: String?,
    onResetButtonClick: () -> Unit,
) {
    val transition = updateTransition(targetState = enabled, label = "AutoBackupTransition")

    val bottomCornerRadius by transition.animateDp(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "bottomCornerRadius"
    ) { state ->
        if (state) 3.dp else 20.dp
    }

    val safeRadius = bottomCornerRadius.coerceAtLeast(0.dp)

    val subMenuSurfaceTintAlpha by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "subMenuSurfaceTint"
    ) { state ->
        if (state) 0.8f else 0.0f
    }

    // File picker launcher
    val context = LocalContext.current
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                onPathChange(it.toString())
            }
        }
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(
            alpha = subMenuSurfaceTintAlpha
        ),
    ) {
        Column(
            verticalArrangement = spacedBy(3.dp),
        ) {
            CardListItem(
                cardItem = CardItem(
                    leadingIcon = shared.drawable.recurring_backup_folder_icon_vector,
                    content = {
                        Column {
                            Text(
                                text = stringResource(shared.string.settings_set_data_backup_scheduler_label),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(shared.string.settings_set_data_backup_scheduler_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    switchState = enabled,
                    onSwitchChange = onEnabledChange,
                    trailingIcon = shared.drawable.tick_icon_vector
                ),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = safeRadius,
                    bottomEnd = safeRadius
                )
            )

            transition.AnimatedVisibility(
                visible = { targetEnabled -> targetEnabled },
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
            ) {
                Column(
                    verticalArrangement = spacedBy(3.dp),
                    modifier = Modifier.padding(
                        top = 9.dp,
                        bottom = 12.dp,
                        start = 12.dp,
                        end = 12.dp
                    )
                ) {
                    CardListItem(
                        alignTop = true,
                        cardItem = CardItem(
                            leadingIcon = shared.drawable.recurring_event_filled_icon_vector,
                            content = {
                                Column(
                                    verticalArrangement = spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Récurrence",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    FlowRow(
                                        Modifier
                                            .padding(horizontal = 8.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                        verticalArrangement = spacedBy(2.dp),
                                    ) {
                                        BackupFrequency.entries.forEachIndexed { index, freq ->
                                            ToggleButton(
                                                checked = frequency == freq,
                                                onCheckedChange = { onFrequencyChange(freq) },
                                                shapes = when (index) {
                                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                                    BackupFrequency.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                                },
                                                modifier = Modifier.semantics {
                                                    role = Role.RadioButton
                                                },
                                            ) {
                                                Text(
                                                    text = stringResource(freq.labelRes),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                        ),
                        shape = RoundedCornerShape(3.dp)
                    )

                    CardListItem(
                        cardItem = CardItem(
                            leadingIcon = shared.drawable.folder_open_icon_vector,
                            content = {
                                Column(verticalArrangement = spacedBy(8.dp)) {
                                    Text(
                                        text = "Emplacement de sauvegarde",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (!backupPath.isNullOrBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = spacedBy(8.dp),
                                        ) {
                                            Text(
                                                text = backupPath.uriStringToReadablePath(context),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f),
                                            )
                                            IconButton(
                                                onClick = { folderPickerLauncher.launch(null) },
                                                modifier = Modifier.size(24.dp),
                                            ) {
                                                SvgIcon(
                                                    resId = shared.drawable.folder_swap_icon_vector,
                                                    modifier = Modifier.size(18.dp),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Button(
                                                onClick = { folderPickerLauncher.launch(null) },
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = spacedBy(8.dp),
                                                ) {
                                                    SvgIcon(
                                                        resId = shared.drawable.backup_folder_settings_icon_vector,
                                                        modifier = Modifier.size(20.dp),
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                    Text(
                                                        text = stringResource(shared.string.settings_set_data_backup_path),
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        ),
                        shape = RoundedCornerShape(3.dp),
                        alignTop = true,
                    )

                    CardListItem(
                        cardItem = CardItem(
                            leadingIcon = shared.drawable.recurring_event_filled_icon_vector,
                            content = {
                                Column {
                                    Text(
                                        text = "Dernière sauvegarde",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        text = lastBackupDate?.takeIf { it.isNotBlank() }
                                            ?: stringResource(shared.string.settings_no_data_backup_file),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            },
                        ),
                        alignTop = true,
                        shape = RoundedCornerShape(
                            topStart = 3.dp,
                            topEnd = 3.dp,
                            bottomStart = 8.dp,
                            bottomEnd = 8.dp
                        )
                    )

                    Button(
                        onClick = onResetButtonClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors().copy(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                    ) {
                        Row(
                            horizontalArrangement = spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SvgIcon(
                                resId = shared.drawable.reset_settings_icon_vector,
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(stringResource(shared.string.common_reset))
                        }
                    }
                }
            }
        }
    }
}

const val locale = "fr"
const val darkTheme = false
const val testData = true
const val testBackupPath = "content://com.android.externalstorage.documents/tree/primary%3ADiabdata"
const val testLastBackupDate = "18 juil. 2026, 10:37"

@Preview(name = "OFF", showBackground = true, locale = locale)
@Composable
fun AutoBackupCardPreviewOffLight() {
    DiabDataTheme(dynamicColor = false, darkTheme = darkTheme) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(30.dp)
        ) {
            AutoBackupCard(
                enabled = false,
                onEnabledChange = {},
                frequency = null,
                backupPath = "",
                lastBackupDate = null,
                onFrequencyChange = {},
                onPathChange = {},
                onResetButtonClick = {}
            )
        }
    }
}

@Preview(name = "ON", showBackground = true, locale = locale)
@Composable
fun AutoBackupCardPreviewOnLight() {
    DiabDataTheme(dynamicColor = false, darkTheme = darkTheme) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(30.dp)
        ) {
            @Suppress("UNNECESSARY_SAFE_CALL", "BOOLEAN_VARIABLE_IS_CONSTANT")
            AutoBackupCard(
                enabled = true,
                onEnabledChange = {},
                frequency = BackupFrequency.DAILY,
                backupPath = testBackupPath?.takeIf { it.isNotBlank() && testData } ?: "",
                lastBackupDate = testLastBackupDate?.takeIf { it.isNotBlank() && testData } ?: "",
                onFrequencyChange = {},
                onPathChange = {},
                onResetButtonClick = {}
            )
        }
    }
}