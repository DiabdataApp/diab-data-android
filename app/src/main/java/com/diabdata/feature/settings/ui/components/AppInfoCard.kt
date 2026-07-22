package com.diabdata.feature.settings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.diabdata.core.ui.theme.DiabDataTheme
import com.diabdata.core.ui.theme.ExtendedTheme
import com.diabdata.core.ui.theme.GoogleSansFlexFontFamily
import com.diabdata.core.utils.ui.ColoredIconCircle
import com.diabdata.core.utils.ui.SvgIcon
import com.diabdata.feature.settings.dataSettingsSection.ui.components.locale
import com.diabdata.shared.R as shared

@Composable
fun AppInfoCard(isBeta: Boolean, versionName: String, showChangeLogDialog: () -> Unit){
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
                    ExtendedTheme.colors.betaContainer
                } else {
                    ExtendedTheme.colors.releaseVersionContainer
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
                    color = if (isBeta) {
                        ExtendedTheme.colors.betaContainer
                    } else {
                        ExtendedTheme.colors.releaseVersionContainer
                    },
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
                            ExtendedTheme.colors.onBetaContainer
                        } else {
                            ExtendedTheme.colors.onReleaseVersionContainer
                        }
                    )
                },
                shape = RoundedCornerShape(50),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isBeta) {
                        ExtendedTheme.colors.betaContainer
                    } else {
                        ExtendedTheme.colors.releaseVersionContainer
                    },
                    labelColor = if (isBeta) {
                        ExtendedTheme.colors.onBetaContainer
                    } else {
                        ExtendedTheme.colors.onReleaseVersionContainer
                    }
                ),
                onClick = {},
            )

            AssistChip(
                onClick = {
                    if (!isBeta) {
                        uriHandler.openUri(
                            "https://github.com/DiabdataApp/diab-data-android"
                        )
                    } else {
                        uriHandler.openUri(
                            "https://github.com/DiabdataApp/diab-data-android/tree/dev"
                        )
                    }
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
                onClick = { showChangeLogDialog() },
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
}

const val isBeta = false

@Preview(name = "LIGHT", showBackground = true, locale = locale)
@Composable
fun AutoBackupCardPreviewLight() {
    DiabDataTheme(dynamicColor = false, darkTheme = false) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(30.dp)
        ) {
            AppInfoCard(
                isBeta = isBeta,
                versionName = "1.0.0",
                showChangeLogDialog = {}
            )
        }
    }
}

@Preview(name = "DARK", showBackground = true, locale = locale)
@Composable
fun AutoBackupCardPreviewDark() {
    DiabDataTheme(dynamicColor = false, darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(30.dp)
        ) {
            AppInfoCard(
                isBeta = isBeta,
                versionName = "1.0.0",
                showChangeLogDialog = {}
            )
        }
    }
}
