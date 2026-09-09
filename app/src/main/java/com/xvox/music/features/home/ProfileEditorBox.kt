package com.xvox.music.features.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.data.preferences.UserPreferences
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.setup.PfpType
import com.xvox.music.features.setup.XvoxAvatarPicker
import kotlinx.coroutines.launch

/**
 * Profile editor.
 *
 * Adding a picture keeps it: it joins the avatar strip and stays there, selected or not, until
 * its delete badge is tapped. Exactly the same behaviour as the setup screen.
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

    var name by remember(profile.username) { mutableStateOf(profile.username) }
    var selected by remember(profile.selectedPfp) {
        mutableStateOf(runCatching { PfpType.valueOf(profile.selectedPfp) }.getOrDefault(PfpType.DEFAULT))
    }
    var customUri by remember(profile.customPfpUri) { mutableStateOf(profile.customPfpUri) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            // Persist immediately so the picture is part of the stack even before Save.
            prefs.addCustomPfp(uri.toString())?.let { stored ->
                customUri = stored
                selected = PfpType.CUSTOM
            }
        }
    }

    // A deleted picture must not stay selected.
    LaunchedEffect(storedCustoms) {
        if (selected == PfpType.CUSTOM && customUri != null && customUri !in storedCustoms) {
            customUri = storedCustoms.firstOrNull()
            if (customUri == null) selected = PfpType.DEFAULT
        }
    }

    val canSave = name.isNotBlank() && (selected != PfpType.CUSTOM || customUri != null)

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
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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

        // Lines shown under the name (the little messages on the profile). Existing lines keep
        // their X to delete; an "add" field at the end lets new lines be added freely.
        val storedLines by prefs.profileLines.collectAsState(initial = profile.profileLines)
        var lines by remember(profile.username) { mutableStateOf(profile.profileLines) }
        var draft by remember { mutableStateOf("") }

        fun persist(next: List<String>) {
            lines = next
            scope.launch { prefs.setProfileLines(next) }
        }

        Text("Shown under your name", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))

        if (lines.isEmpty()) {
            Text("Nothing yet — add a short message below", color = colors.mutedText, fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp))
        }
        lines.forEach { line ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(colors.card)
                    .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(line, color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).xvoxPressScale(pressedScale = 0.85f) {
                        haptics.tap(); persist(lines - line)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = colors.mutedText, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(5.dp))
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = draft,
                onValueChange = { if (it.length <= 40) draft = it },
                singleLine = true,
                textStyle = TextStyle(color = colors.primaryText, fontSize = 13.sp),
                cursorBrush = SolidColor(colors.primaryAccent),
                modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp)).background(colors.cardElevated),
                decorationBox = { field ->
                    Box(
                        Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) { field() }
                }
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.width(84.dp).height(42.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (draft.isNotBlank() && lines.size < 4) colors.primaryAccent else colors.cardElevated)
                    .xvoxPressScale(enabled = draft.isNotBlank() && lines.size < 4) {
                        haptics.tap()
                        persist((lines + draft.trim()).distinct())
                        draft = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Add", color = if (draft.isNotBlank() && lines.size < 4) colors.background else colors.mutedText,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

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
