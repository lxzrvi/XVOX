package com.xvox.music.features.settings.sections

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.features.home.XvoxSongArtwork
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.ColorPickerRow
import com.xvox.music.features.settings.components.SettingsAccordionItem
import com.xvox.music.features.settings.components.SettingsChoiceRow
import com.xvox.music.features.settings.components.SettingsControlsEditor
import com.xvox.music.features.settings.components.XvoxThinLineSlider
import kotlin.math.roundToInt

@Composable
fun AppearanceSettingsSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val colors = XvoxTheme.colors
    val chrome = state.chromeStyle
    var expandedGroup by remember { mutableStateOf<String?>(null) }

    fun toggle(group: String) {
        expandedGroup = if (expandedGroup == group) null else group
    }

    SettingsControlsEditor(controls = {
        SettingsAccordionItem(
            title = "Theme",
            expanded = expandedGroup == "Theme",
            onToggle = { toggle("Theme") }
        ) {
            SettingsChoiceRow(
                listOf("System" to "System", "Light" to "Light", "Dark" to "Dark", "AMOLED" to "AMOLED"),
                state.theme
            ) { viewModel.setTheme(it) }
        }

        SettingsAccordionItem(
            title = "Accent",
            expanded = expandedGroup == "Accent",
            onToggle = { toggle("Accent") }
        ) {
            SettingsChoiceRow(
                listOf("White" to "White", "Red" to "Red", "Blue" to "Blue"),
                if (state.accentColor.startsWith("#")) "custom" else state.accentColor
            ) { key -> if (key != "custom") viewModel.setAccentColor(key) }
            Spacer(Modifier.height(8.dp))
            ColorPickerRow(
                label = "Custom accent",
                hex = if (state.accentColor.startsWith("#")) state.accentColor else "",
                onColorChange = { hex -> viewModel.setAccentColor(hex) },
                subtitle = if (state.accentColor.startsWith("#")) "Applied everywhere" else "Pick any colour"
            )
        }

        SettingsAccordionItem(
            title = "Header",
            expanded = expandedGroup == "Header",
            onToggle = { toggle("Header") }
        ) {
            HeaderPhotoRow(state, viewModel)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Transparency", color = colors.primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(118.dp))
                XvoxThinLineSlider(
                    value = chrome.headerBgAlpha,
                    onValueChange = { a ->
                        viewModel.setChromeStyle { it.copy(headerBgAlpha = a) }
                    },
                    valueRange = 0f..1f,
                    defaultValue = 0.92f,
                    modifier = Modifier.weight(1f)
                )
                Text("${(chrome.headerBgAlpha.coerceIn(0f, 1f) * 100).roundToInt()}%",
                    color = colors.primaryText, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            }
        }

        SettingsAccordionItem(
            title = "Text size",
            expanded = expandedGroup == "Text size",
            onToggle = { toggle("Text size") }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A", color = colors.mutedText, fontSize = 12.sp, modifier = Modifier.width(30.dp))
                XvoxThinLineSlider(
                    value = state.fontSizeScale,
                    onValueChange = viewModel::setFontSizeScale,
                    valueRange = 0.8f..1.4f,
                    defaultValue = 1f,
                    modifier = Modifier.weight(1f)
                )
                Text("A", color = colors.primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
            }
        }
    })
}

@Composable
private fun HeaderPhotoRow(state: SettingsState, viewModel: SettingsViewModel) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val context = LocalContext.current
    var croppingUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            croppingUri = uri
        }
    }

    if (croppingUri != null) {
        com.xvox.music.core.ui.components.XvoxImageCropDialog(
            sourceUri = croppingUri!!,
            isCircle = false,
            onCropped = { croppedUri ->
                croppingUri = null
                viewModel.setHeaderImageUri(croppedUri.toString())
                if (viewModel.state.value.chromeStyle.headerBgAlpha >= 0.9f) {
                    viewModel.setChromeStyle { it.copy(headerBgAlpha = 0.28f) }
                }
            },
            onDismiss = { croppingUri = null }
        )
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.headerImageUri != null) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.cardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        XvoxSongArtwork(
                            artwork = Uri.parse(state.headerImageUri),
                            requestSize = 128,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column {
                    Text(
                        if (state.headerImageUri == null) "Choose photo" else "Header photo stored",
                        color = colors.primaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (state.headerImageUri == null) "Select an image for the top header" else "Applied behind top header",
                        color = colors.secondaryText,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.cardElevated)
                        .xvoxPressScale {
                            haptics.tap()
                            picker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        if (state.headerImageUri == null) "Browse" else "Change",
                        color = colors.primaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (state.headerImageUri != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.cardElevated)
                            .xvoxPressScale {
                                haptics.tap()
                                viewModel.setHeaderImageUri(null)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_xvox_close),
                            contentDescription = "Remove header photo",
                            tint = colors.secondaryText,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
