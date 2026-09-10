package com.xvox.music.features.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.data.preferences.ProfileDefaults
import com.xvox.music.data.preferences.UserPreferences
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.setup.PfpType
import com.xvox.music.features.setup.XvoxAvatarPicker
import kotlinx.coroutines.launch

/**
 * Profile editor:
 * Shows avatar picker, username, greeting lines (all default 20 lines numbered 1..20 with '✕',
 * custom lines 21, 22, 23...), interval slider, and an accent-bordered add text box.
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

    var name by remember(profile.username) { mutableStateOf(profile.username) }
    var selected by remember(profile.selectedPfp) {
        mutableStateOf(runCatching { PfpType.valueOf(profile.selectedPfp) }.getOrDefault(PfpType.DEFAULT))
    }
    var customUri by remember(profile.customPfpUri) { mutableStateOf(profile.customPfpUri) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            prefs.addCustomPfp(uri.toString())?.let { stored ->
                customUri = stored
                selected = PfpType.CUSTOM
            }
        }
    }

    LaunchedEffect(storedCustoms) {
        if (selected == PfpType.CUSTOM && customUri != null && customUri !in storedCustoms) {
            customUri = storedCustoms.firstOrNull()
            if (customUri == null) selected = PfpType.DEFAULT
        }
    }

    val canSave = name.isNotBlank() && (selected != PfpType.CUSTOM || customUri != null)

    var lines by remember(profile.username) {
        mutableStateOf(
            if (profile.profileLinesInitialized) profile.profileLines
            else GreetingLines
        )
    }
    var showLines by remember(profile.showProfileLines) { mutableStateOf(profile.showProfileLines) }
    var draft by remember { mutableStateOf("") }

    fun persist(next: List<String>) {
        lines = next
        scope.launch {
            prefs.setProfileLines(next)
            prefs.setProfileLinesInitialized(true)
        }
    }

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

        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Lines under my name", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
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
            text = if (showLines) "Shown under your name" else "Hidden — your name appears beside the picture only",
            color = if (showLines) colors.secondaryText else colors.mutedText, fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Interval slider
        Text(
            "Change every ${(greetingInterval / 1000f).let { if (it % 1f == 0f) it.toInt().toString() else "%.1f".format(it) }}s",
            color = colors.mutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )
        com.xvox.music.features.settings.components.XvoxThinLineSlider(
            value = (greetingInterval / 1000f).coerceIn(1.5f, 60f),
            onValueChange = { seconds -> scope.launch { prefs.setGreetingIntervalMs((seconds * 1000f).toLong()) } },
            valueRange = 1.5f..60f,
            defaultValue = 8f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Text("All greeting lines (${lines.size})", color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))

        // All lines: numbered 1, 2, 3... 20, 21, 22... with '✕' buttons to remove
        lines.forEachIndexed { index, line ->
            ProfileNumberedLineField(
                index = index + 1,
                line = line,
                onCommit = { next ->
                    val updated = lines.toMutableList().also { if (index in it.indices) it[index] = next }
                    persist(updated.filter { it.isNotBlank() })
                },
                onRemove = {
                    haptics.tap()
                    persist(lines.filterIndexed { i, _ -> i != index })
                }
            )
            Spacer(Modifier.height(5.dp))
        }

        Spacer(Modifier.height(8.dp))

        // Add text field with visible accent border
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = draft,
                onValueChange = { if (it.length <= 60) draft = it },
                singleLine = true,
                textStyle = TextStyle(color = colors.primaryText, fontSize = 13.sp),
                cursorBrush = SolidColor(colors.primaryAccent),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardElevated)
                    .border(1.2.dp, colors.primaryAccent, RoundedCornerShape(12.dp)),
                decorationBox = { field ->
                    Box(
                        Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (draft.isEmpty()) {
                            Text("Add new line #${lines.size + 1}…", color = colors.mutedText, fontSize = 12.sp)
                        }
                        field()
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.width(84.dp).height(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (draft.isNotBlank()) colors.primaryAccent else colors.cardElevated)
                    .xvoxPressScale(enabled = draft.isNotBlank()) {
                        haptics.tap()
                        persist((lines + draft.trim()).distinct())
                        draft = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Add",
                    color = if (draft.isNotBlank()) colors.background else colors.mutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(18.dp))

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

/** One numbered editable profile line with ✕ button. */
@Composable
private fun ProfileNumberedLineField(
    index: Int,
    line: String,
    onCommit: (String) -> Unit,
    onRemove: () -> Unit
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    var text by remember(line) { mutableStateOf(line) }
    var focused by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(colors.card)
            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$index.",
            color = colors.primaryAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )
        BasicTextField(
            value = text,
            onValueChange = { if (it.length <= 60) text = it },
            singleLine = true,
            textStyle = TextStyle(
                color = if (focused) colors.primaryText else colors.secondaryText,
                fontSize = 12.sp
            ),
            cursorBrush = SolidColor(colors.primaryAccent),
            modifier = Modifier.weight(1f).height(30.dp)
                .onFocusChanged { state ->
                    val was = focused
                    focused = state.isFocused
                    if (was && !state.isFocused) {
                        val next = text.trim()
                        if (next != line) { haptics.tap(); onCommit(next) }
                    }
                }
        )
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).xvoxPressScale(pressedScale = 0.85f) {
                haptics.tap()
                onRemove()
            },
            contentAlignment = Alignment.Center
        ) {
            Text("✕", color = colors.mutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
