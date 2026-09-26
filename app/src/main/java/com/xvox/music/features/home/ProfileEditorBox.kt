package com.xvox.music.features.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.components.XvoxImageCropDialog
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.data.preferences.UserPreferences
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.settings.components.XvoxContinuousSlider
import com.xvox.music.features.setup.PfpType
import com.xvox.music.features.setup.XvoxAvatarPicker
import kotlinx.coroutines.launch

/**
 * All profile/header changes are held here until the fixed sheet footer chooses Save.  Asset
 * creation (cropping a newly chosen image) may happen immediately, but the selected profile,
 * Header image, and dimness values are never written by this editor itself.
 */
data class ProfileEditorDraft(
    val username: String,
    val selectedPfp: String,
    val customPfpUri: String?,
    val showProfileLines: Boolean,
    val headerImageUri: String?,
    val rememberedHeaderImageUri: String?,
    val headerDimEnabled: Boolean,
    val headerDimAmount: Float
) {
    companion object {
        fun from(
            profile: UserPreferences,
            chrome: com.xvox.music.core.ui.chrome.XvoxChromeStyle
        ) = ProfileEditorDraft(
            username = profile.username,
            selectedPfp = profile.selectedPfp,
            customPfpUri = profile.customPfpUri,
            showProfileLines = profile.showProfileLines,
            headerImageUri = profile.headerImageUri,
            rememberedHeaderImageUri = profile.headerImageUri,
            headerDimEnabled = chrome.headerDimEnabled,
            headerDimAmount = chrome.headerDimAmount.coerceIn(0f, 1f)
        )
    }
}

/** Scrollable body for the Profile sheet; its Save/Reset/Cancel footer is owned by the caller. */
@Composable
fun ProfileEditorBox(
    profile: UserPreferences,
    draft: ProfileEditorDraft,
    onDraftChange: (ProfileEditorDraft) -> Unit
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { UserPreferencesRepository(context.applicationContext) }
    val storedCustoms by prefs.customPfpUris.collectAsState(initial = profile.customPfpUris)
    val persistedHeaderUri by prefs.savedCustomHeaderUri.collectAsState(initial = null)
    var croppingAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var croppingHeaderUri by remember { mutableStateOf<Uri?>(null) }

    val selected = remember(draft.selectedPfp) {
        runCatching { PfpType.valueOf(draft.selectedPfp) }.getOrDefault(PfpType.DEFAULT)
    }

    // A saved custom Header remains available after choosing Default, but it is only activated
    // when the user presses Custom and finally saves the transaction.
    LaunchedEffect(persistedHeaderUri, draft.rememberedHeaderImageUri) {
        if (draft.rememberedHeaderImageUri.isNullOrBlank() && !persistedHeaderUri.isNullOrBlank()) {
            onDraftChange(draft.copy(rememberedHeaderImageUri = persistedHeaderUri))
        }
    }

    val avatarPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) croppingAvatarUri = uri
    }
    val headerPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val isGif = uri.toString().contains(".gif", ignoreCase = true) ||
            (context.contentResolver.getType(uri)?.contains("gif", ignoreCase = true) == true)
        if (isGif) {
            // Preserve animation by keeping an internal copy rather than sending a GIF through a
            // bitmap cropper.  Selecting it is still draft-only until Save.
            val saved = runCatching {
                val file = java.io.File(context.filesDir, "xvox_header_gif_${System.currentTimeMillis()}.gif")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                Uri.fromFile(file).toString()
            }.getOrElse { uri.toString() }
            onDraftChange(draft.copy(headerImageUri = saved, rememberedHeaderImageUri = saved))
        } else {
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
                        onDraftChange(
                            draft.copy(
                                selectedPfp = PfpType.CUSTOM.name,
                                customPfpUri = stored
                            )
                        )
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
            aspectRatio = 2.2f,
            onCropped = { croppedUri ->
                croppingHeaderUri = null
                val saved = croppedUri.toString()
                onDraftChange(draft.copy(headerImageUri = saved, rememberedHeaderImageUri = saved))
            },
            onDismiss = { croppingHeaderUri = null }
        )
    }

    LaunchedEffect(storedCustoms, draft.selectedPfp, draft.customPfpUri) {
        if (selected == PfpType.CUSTOM && draft.customPfpUri != null && draft.customPfpUri !in storedCustoms) {
            val fallback = storedCustoms.firstOrNull()
            onDraftChange(
                draft.copy(
                    selectedPfp = if (fallback == null) PfpType.DEFAULT.name else PfpType.CUSTOM.name,
                    customPfpUri = fallback
                )
            )
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .xvoxBoxScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        XvoxAvatarPicker(
            username = draft.username,
            selectedType = selected,
            selectedCustomUri = if (selected == PfpType.CUSTOM) draft.customPfpUri else null,
            customUris = storedCustoms,
            onSelectBuiltIn = {
                haptics.tap()
                onDraftChange(draft.copy(selectedPfp = it.name, customPfpUri = null))
            },
            onSelectCustom = {
                haptics.tap()
                onDraftChange(draft.copy(selectedPfp = PfpType.CUSTOM.name, customPfpUri = it))
            },
            onAddCustom = {
                haptics.tap()
                avatarPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDeleteCustom = { uri ->
                haptics.tap()
                scope.launch { prefs.removeCustomPfp(uri) }
            }
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Username", color = colors.secondaryText, fontSize = 11.sp)
            BasicTextField(
                value = draft.username,
                onValueChange = { value ->
                    if (value.length <= 16) onDraftChange(draft.copy(username = value))
                },
                singleLine = true,
                textStyle = TextStyle(color = colors.primaryText, fontSize = 14.sp),
                cursorBrush = SolidColor(colors.primaryAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardElevated),
                decorationBox = { field ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) { field() }
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Greeting lines under name", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Keep your rotating profile greeting visible", color = colors.secondaryText, fontSize = 11.sp)
            }
            Switch(
                checked = draft.showProfileLines,
                onCheckedChange = { enabled ->
                    haptics.tap()
                    onDraftChange(draft.copy(showProfileLines = enabled))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.background,
                    checkedTrackColor = colors.primaryAccent,
                    uncheckedThumbColor = colors.secondaryText,
                    uncheckedTrackColor = colors.cardElevated
                )
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Header", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Image / GIF",
                    color = colors.secondaryText,
                    fontSize = 11.sp
                )
                Text(
                    text = "NEW",
                    color = colors.background,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(colors.primaryAccent)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeaderImageChoice(
                    title = "Default",
                    active = draft.headerImageUri.isNullOrBlank(),
                    onClick = {
                        haptics.tap()
                        onDraftChange(draft.copy(headerImageUri = null))
                    },
                    modifier = Modifier.weight(1f)
                )
                HeaderImageChoice(
                    title = if (draft.headerImageUri.isNullOrBlank()) "Image / GIF" else "Image / GIF ✓",
                    active = !draft.headerImageUri.isNullOrBlank(),
                    imageUri = draft.headerImageUri ?: draft.rememberedHeaderImageUri,
                    onClick = {
                        haptics.tap()
                        val remembered = draft.rememberedHeaderImageUri
                        if (draft.headerImageUri.isNullOrBlank() && !remembered.isNullOrBlank()) {
                            onDraftChange(draft.copy(headerImageUri = remembered))
                        } else {
                            headerPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            HeaderIdeasLoopBanner()
        }

        val visibleDimness = if (draft.headerDimEnabled) draft.headerDimAmount.coerceIn(0f, 1f) else 0f
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Header Dimness", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${(visibleDimness * 100).toInt()}%",
                    color = colors.primaryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("Drag for a continuous dim overlay.", color = colors.secondaryText, fontSize = 11.sp)
            XvoxContinuousSlider(
                value = visibleDimness,
                onValueChange = { amount ->
                    onDraftChange(
                        draft.copy(
                            headerDimEnabled = amount > .005f,
                            headerDimAmount = amount.coerceIn(0f, 1f)
                        )
                    )
                },
                valueRange = 0f..1f,
                defaultValue = .50f,
                contentDescription = "Header dimness"
            )
        }

        Spacer(Modifier.height(4.dp))
    }
}

private const val HeaderIdeasPinterestUrl = "https://in.pinterest.com/ideas/loop-banner-gif/939795684803/"

/** A deliberately plain link: profile editing has no live Header preview or animated banner. */
@Composable
private fun HeaderIdeasLoopBanner() {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    Text(
        text = "Need more cool Header ideas? Tap here",
        color = colors.primaryAccent,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .clickable {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(HeaderIdeasPinterestUrl)))
                }
            }
            .padding(vertical = 4.dp)
    )
}

@Composable
private fun HeaderImageChoice(
    title: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageUri: String? = null
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(21.dp)
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(if (active) colors.primaryAccent else colors.cardElevated)
            .border(.8.dp, if (active) Color.Transparent else colors.cardBorder.copy(alpha = .65f), shape)
            .xvoxPressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUri.isNullOrBlank()) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(Modifier.matchParentSize().background(colors.background.copy(alpha = if (active) .40f else .62f)))
        }
        Text(
            title,
            color = if (!imageUri.isNullOrBlank()) Color.White else if (active) colors.background else colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
