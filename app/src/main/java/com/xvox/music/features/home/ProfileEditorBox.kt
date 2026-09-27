package com.xvox.music.features.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.components.XvoxImageCropDialog
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
            .padding(vertical = 6.dp)
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
            },
            edgeToEdge = true
        )

        Spacer(Modifier.height(14.dp))
        // The sheet viewport itself is edge-to-edge for avatar choices. All form controls retain
        // their own readable 16dp inset rather than relying on a negative child padding hack.
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                // This is deliberately plain actionable text, not another option pill. It opens
                // the same image/GIF chooser as Custom while keeping the selected-choice controls
                // below visually unambiguous.
                Text(
                    text = "Image / GIF",
                    color = colors.primaryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable {
                            haptics.tap()
                            headerPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .padding(vertical = 3.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "New",
                    color = colors.primaryAccent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(.7.dp, colors.primaryAccent.copy(alpha = .68f), RoundedCornerShape(50))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
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
                    title = "Custom",
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

        // Chrome still stores a dim overlay for backward compatibility, but the editor exposes
        // the direct user-facing inverse: 100% is the un-darkened Header and 0% is black.
        val visibleBrightness = if (draft.headerDimEnabled) {
            1f - draft.headerDimAmount.coerceIn(0f, 1f)
        } else {
            1f
        }
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Header Brightness", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${(visibleBrightness * 100).toInt()}%",
                    color = colors.primaryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("Drag for continuous Header brightness.", color = colors.secondaryText, fontSize = 11.sp)
            XvoxContinuousSlider(
                value = visibleBrightness,
                onValueChange = { brightness ->
                    val normalized = brightness.coerceIn(0f, 1f)
                    onDraftChange(
                        draft.copy(
                            headerDimEnabled = normalized < .995f,
                            headerDimAmount = (1f - normalized).coerceIn(0f, 1f)
                        )
                    )
                },
                valueRange = 0f..1f,
                defaultValue = 1f,
                contentDescription = "Header brightness"
            )
        }

        Spacer(Modifier.height(4.dp))
        }
    }
}

private const val HeaderIdeasPinterestUrl = "https://in.pinterest.com/ideas/loop-banner-gif/939795684803/"

/** A quiet helper link: only the actionable words are accented. */
@Composable
private fun HeaderIdeasLoopBanner() {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    val line = buildAnnotatedString {
        append("Need more cool Header ideas? ")
        withStyle(SpanStyle(color = colors.primaryAccent, fontWeight = FontWeight.SemiBold)) {
            append("Tap here")
        }
    }
    Text(
        text = line,
        color = colors.secondaryText,
        fontSize = 12.sp,
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
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // The image begins slightly overscanned at rest and eases inward on press. The fixed clipped
    // container never scales, so no card-colour flash or cropped blank ring can appear.
    val imageScale by animateFloatAsState(
        targetValue = if (pressed) 1f else 1.055f,
        animationSpec = tween(120),
        label = "headerChoiceImagePress"
    )
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(colors.cardElevated)
            // Selected Default and Custom choices use the same thin accent outline rather than a
            // filled accent pill, so the header preview remains visible and calm.
            .border(.9.dp, if (active) colors.primaryAccent else colors.cardBorder.copy(alpha = .65f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUri.isNullOrBlank()) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { scaleX = imageScale; scaleY = imageScale }
            )
            Box(Modifier.matchParentSize().background(colors.background.copy(alpha = if (active) .40f else .62f)))
        }
        Text(
            title,
            color = if (!imageUri.isNullOrBlank()) Color.White else colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
