package com.xvox.music.features.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.components.XvoxImageCropDialog
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.data.preferences.UserPreferences
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.setup.PfpType
import com.xvox.music.features.setup.XvoxAvatarPicker
import kotlinx.coroutines.launch

/**
 * Profile & Header Editor Box:
 * Avatar picker with crop, username, greeting lines ON/OFF toggle, interval slider,
 * and integrated Header photo customizer.
 */
@Composable
fun ProfileEditorBox(
    profile: UserPreferences,
    onCancel: () -> Unit,
    onSave: (String, String, String?) -> Unit,
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { UserPreferencesRepository(context.applicationContext) }
    val storedCustoms by prefs.customPfpUris.collectAsState(initial = profile.customPfpUris)
    val greetingInterval by prefs.greetingIntervalMs.collectAsState(initial = profile.greetingIntervalMs)
    val currentHeaderUri by prefs.headerImageUri.collectAsState(initial = null)

    var name by remember(profile.username) { mutableStateOf(profile.username) }
    var selected by remember(profile.selectedPfp) {
        mutableStateOf(runCatching { PfpType.valueOf(profile.selectedPfp) }.getOrDefault(PfpType.DEFAULT))
    }
    var customUri by remember(profile.customPfpUri) { mutableStateOf(profile.customPfpUri) }
    var croppingAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var croppingHeaderUri by remember { mutableStateOf<Uri?>(null) }

    val avatarPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            croppingAvatarUri = uri
        }
    }

    val headerPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            croppingHeaderUri = uri
        }
    }

    if (croppingAvatarUri != null) {
        XvoxImageCropDialog(
            sourceUri = croppingAvatarUri!!,
            isCircle = true,
            onCropped = { croppedUri ->
                croppingAvatarUri = null
                scope.launch {
                    prefs.addCustomPfp(croppedUri.toString())?.let { stored ->
                        customUri = stored
                        selected = PfpType.CUSTOM
                    }
                }
            },
            onDismiss = { croppingAvatarUri = null }
        )
    }

    if (croppingHeaderUri != null) {
        XvoxImageCropDialog(
            sourceUri = croppingHeaderUri!!,
            isCircle = false,
            onCropped = { croppedUri ->
                croppingHeaderUri = null
                scope.launch {
                    prefs.setHeaderImageUri(croppedUri.toString())
                }
            },
            onDismiss = { croppingHeaderUri = null }
        )
    }

    LaunchedEffect(storedCustoms) {
        if (selected == PfpType.CUSTOM && customUri != null && customUri !in storedCustoms) {
            customUri = storedCustoms.firstOrNull()
            if (customUri == null) selected = PfpType.DEFAULT
        }
    }

    val canSave = name.isNotBlank() && (selected != PfpType.CUSTOM || customUri != null)
    var showLines by remember(profile.showProfileLines) { mutableStateOf(profile.showProfileLines) }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        XvoxAvatarPicker(
            username = name,
            selectedType = selected,
            selectedCustomUri = if (selected == PfpType.CUSTOM) customUri else null,
            customUris = storedCustoms,
            onSelectBuiltIn = { haptics.tap(); selected = it; customUri = null },
            onSelectCustom = { haptics.tap(); selected = PfpType.CUSTOM; customUri = it },
            onAddCustom = {
                haptics.tap()
                avatarPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDeleteCustom = { uri -> haptics.tap(); scope.launch { prefs.removeCustomPfp(uri) } }
        )

        Spacer(Modifier.height(14.dp))

        Text("Username", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))

        BasicTextField(
            value = name,
            onValueChange = { if (it.length <= 16) name = it },
            singleLine = true,
            textStyle = TextStyle(color = colors.primaryText, fontSize = 14.sp),
            cursorBrush = SolidColor(colors.primaryAccent),
            modifier = Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).background(colors.card),
            decorationBox = { field ->
                Box(
                    modifier = Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) { field() }
            }
        )

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Greeting lines under name", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            Switch(
                checked = showLines,
                onCheckedChange = { on ->
                    showLines = on
                    scope.launch { prefs.setShowProfileLines(on) }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.background,
                    checkedTrackColor = colors.primaryAccent,
                    uncheckedThumbColor = colors.secondaryText,
                    uncheckedTrackColor = colors.cardElevated
                )
            )
        }
        Text(
            text = if (showLines) "Active: greetings rotate under your name" else "Off: your name appears beside the picture only",
            color = if (showLines) colors.secondaryText else colors.mutedText, fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (showLines) {
            // Interval slider
            Text(
                "Change interval: ${(greetingInterval / 1000f).let { if (it % 1f == 0f) it.toInt().toString() else "%.1f".format(it) }}s",
                color = colors.mutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
            com.xvox.music.features.settings.components.XvoxThinLineSlider(
                value = (greetingInterval / 1000f).coerceIn(1.5f, 60f),
                onValueChange = { seconds -> scope.launch { prefs.setGreetingIntervalMs((seconds * 1000f).toLong()) } },
                valueRange = 1.5f..60f,
                defaultValue = 8f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        // Header Background Photo Section in Profile Box
        Text("Header Settings", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (currentHeaderUri != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.cardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        XvoxSongArtwork(
                            artwork = Uri.parse(currentHeaderUri),
                            requestSize = 128,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Column {
                    Text(
                        if (currentHeaderUri != null) "Header Photo Active" else "No Header Photo",
                        color = colors.primaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Custom backdrop above home",
                        color = colors.mutedText,
                        fontSize = 10.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentHeaderUri != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.cardElevated)
                            .xvoxPressScale {
                                haptics.tap()
                                scope.launch { prefs.setHeaderImageUri(null) }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Remove", color = colors.secondaryText, fontSize = 11.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primaryAccent)
                        .xvoxPressScale {
                            haptics.tap()
                            headerPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(if (currentHeaderUri != null) "Change" else "Pick Photo", color = colors.background, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.width(110.dp).height(38.dp).clip(RoundedCornerShape(19.dp))
                    .background(colors.cardElevated).xvoxPressScale { haptics.tap(); onCancel() },
                contentAlignment = Alignment.Center
            ) { Text("Cancel", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier.width(110.dp).height(38.dp).clip(RoundedCornerShape(19.dp))
                    .background(if (canSave) colors.primaryAccent else colors.cardElevated)
                    .xvoxPressScale(enabled = canSave) {
                        haptics.success()
                        onSave(name.trim(), selected.name, customUri)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Save",
                    color = if (canSave) colors.background else colors.mutedText,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
