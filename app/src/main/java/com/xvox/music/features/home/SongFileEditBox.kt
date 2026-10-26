package com.xvox.music.features.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import com.xvox.music.media.SongFileEditDestination
import com.xvox.music.media.SongFileEditOutcome
import com.xvox.music.media.SongFileEditRequest
import com.xvox.music.media.SongFileEditor
import kotlinx.coroutines.launch

/**
 * A single edit surface for a song title and embedded artwork. The header pencil always opens
 * this surface so Rename and Edit cover never need separate long-press action cards. The user
 * explicitly picks whether Android should mutate the original shared file or create a separately
 * indexed copy before any bytes are written; there is no hidden app-only rename/cover path.
 */
@Composable
fun SongFileEditBox(
    song: Song,
    onCancel: () -> Unit,
    onSaved: (SongFileEditOutcome.Saved) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = XvoxTheme.colors
    val scope = rememberCoroutineScope()
    val editor = remember(context) { SongFileEditor(context.applicationContext) }
    val scrollState = rememberScrollState()
    var title by remember(song.contentUri, song.title) {
        mutableStateOf(TextFieldValue(song.title, TextRange(song.title.length)))
    }
    var selectedCover by remember(song.contentUri) { mutableStateOf<Uri?>(null) }
    var destination by remember(song.contentUri) { mutableStateOf(SongFileEditDestination.ORIGINAL) }
    var pendingRequest by remember(song.contentUri) { mutableStateOf<SongFileEditRequest?>(null) }
    var saving by remember(song.contentUri) { mutableStateOf(false) }
    var error by remember(song.contentUri) { mutableStateOf<String?>(null) }

    fun apply(request: SongFileEditRequest) {
        saving = true
        error = null
        scope.launch {
            when (val outcome = editor.apply(song, request)) {
                is SongFileEditOutcome.Saved -> onSaved(outcome)
                is SongFileEditOutcome.Failed -> {
                    saving = false
                    error = outcome.message
                }
            }
        }
    }

    val writeConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val request = pendingRequest
        pendingRequest = null
        if (request != null) {
            if (result.resultCode == Activity.RESULT_OK) {
                apply(request)
            } else {
                error = "Original file was not changed because Android permission was not granted"
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // The chosen image is copied into app storage on Save. Taking the grant while the
            // editor is visible keeps slow image decodes reliable without depending on a path.
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            selectedCover = uri
            error = null
        }
    }

    fun save() {
        val cleanTitle = title.text.trim()
        if (cleanTitle.isBlank()) {
            error = "Enter a song name before saving"
            return
        }
        val request = SongFileEditRequest(
            title = cleanTitle,
            coverUri = selectedCover,
            destination = destination,
            renameRequested = cleanTitle != song.title.trim()
        )
        error = null
        if (destination == SongFileEditDestination.ORIGINAL) {
            val consent = editor.originalWriteRequest(song)
            if (consent == null) {
                // App-owned MediaStore files may already be writable. Try only in that case; any
                // real denial returns a clear result instead of falling back to a fake UI edit.
                apply(request)
            } else {
                pendingRequest = request
                runCatching {
                    writeConsentLauncher.launch(IntentSenderRequest.Builder(consent.intentSender).build())
                }.onFailure {
                    pendingRequest = null
                    error = "Android could not open the original-file permission prompt"
                }
            }
        } else {
            apply(request)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 680.dp)
            .verticalScroll(scrollState)
            .xvoxBoxScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Edit Song",
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Rename the real MediaStore file or select a new embedded cover, then choose whether to update the original or make a copy.",
            color = colors.secondaryText,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Text(
            text = "All audio files can be renamed. Cover embedding depends on the audio container; unsupported files show a clear failure instead of a visual-only change.",
            color = colors.mutedText,
            fontSize = 10.5.sp,
            lineHeight = 14.sp
        )

        SongEditIdentityCard(
            song = song,
            previewCover = selectedCover,
            onPickCover = { imagePicker.launch(arrayOf("image/*")) }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardElevated)
                .border(.8.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Song name",
                color = colors.secondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            BasicTextField(
                value = title,
                onValueChange = { title = it; error = null },
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(colors.primaryAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp)
            )
        }

        Text(
            text = "Save to",
            color = colors.primaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        SongEditDestinationCard(
            selected = destination == SongFileEditDestination.ORIGINAL,
            title = "Change original file",
            detail = "Replace the selected file's title and cover after Android confirmation.",
            onClick = { destination = SongFileEditDestination.ORIGINAL }
        )
        SongEditDestinationCard(
            selected = destination == SongFileEditDestination.COPY,
            title = "Make a copy",
            detail = "Create another audio file with this name and cover; keep the original unchanged.",
            onClick = { destination = SongFileEditDestination.COPY }
        )

        error?.let { message ->
            Text(
                text = message,
                color = com.xvox.music.core.design.theme.XvoxRed,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SongEditButton(
                title = "Cancel",
                enabled = !saving,
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            )
            SongEditButton(
                title = if (saving) "Saving…" else "Save",
                enabled = !saving && title.text.isNotBlank(),
                onClick = ::save,
                accent = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SongEditIdentityCard(song: Song, previewCover: Uri?, onPickCover: () -> Unit) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardElevated)
            .border(.8.dp, colors.cardBorder, shape)
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            XvoxSongArtwork(
                artwork = previewCover ?: song.artworkUri,
                fallbackSeed = song.id.hashCode(),
                requestSize = 256,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (previewCover == null) song.title else "New cover selected",
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (previewCover == null) "Tap the pencil to choose any image" else "Tap the pencil to replace the chosen image",
                color = colors.secondaryText,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        // Requirement: the cover card has a visible edit/pencil control on its right edge.
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(colors.accentSoft)
                .xvoxPressScale(onClick = onPickCover),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_xvox_edit),
                contentDescription = "Choose cover image",
                tint = colors.primaryAccent,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun SongEditDestinationCard(selected: Boolean, title: String, detail: String, onClick: () -> Unit) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.accentSoft else colors.cardElevated)
            .border(
                if (selected) 1.2.dp else .8.dp,
                if (selected) colors.primaryAccent else colors.cardBorder,
                shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(1.5.dp, if (selected) colors.primaryAccent else colors.secondaryText, CircleShape)
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(Modifier.size(8.dp).background(colors.primaryAccent, CircleShape))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                detail,
                color = colors.secondaryText,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SongEditButton(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .heightIn(min = 46.dp)
            .clip(shape)
            .background(if (accent) colors.primaryAccent else colors.cardElevated)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (title == "Saving…") {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = colors.background,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = title,
                color = if (accent) colors.background else if (enabled) colors.primaryText else colors.mutedText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
