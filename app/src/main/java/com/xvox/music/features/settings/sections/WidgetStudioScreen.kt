package com.xvox.music.features.settings.sections

import android.app.WallpaperManager
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.features.settings.SettingsState
import com.xvox.music.features.settings.SettingsViewModel
import com.xvox.music.features.settings.components.UniformPreview
import com.xvox.music.widget.XvoxWidgetHelper

/**
 * A dedicated settings page for the home-screen widget.  The preview is deliberately outside the
 * scrolling controls: while the user tunes an option they always see the complete, actual-sized
 * widget against the current device wallpaper.
 */
@Composable
fun WidgetStudioScreen(
    state: SettingsState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val colors = XvoxTheme.colors
    val context = LocalContext.current
    // Activity-scoped playback state makes the Studio preview use the actual current title,
    // artwork, play state, and like state when the app has a song loaded.
    val playerViewModel: com.xvox.music.player.playback.MainPlayerViewModel = viewModel()
    val playerState by playerViewModel.state.collectAsState()
    var liveSize by remember { mutableStateOf(XvoxWidgetHelper.activeHomeWidgetSize(context)) }
    // Preview-only canvas choice: it helps users judge contrast against several real-world home
    // surfaces without altering the actual launcher wallpaper or widget configuration.
    var previewBackground by rememberSaveable { mutableStateOf(WidgetStudioPreviewBackground.WALLPAPER) }
    var previewPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    val previewPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            previewPhotoUri = uri.toString()
            previewBackground = WidgetStudioPreviewBackground.PHOTO
        }
    }

    LaunchedEffect(Unit) {
        // Re-read options once the screen is on top so a freshly pinned/resized widget is used.
        liveSize = XvoxWidgetHelper.activeHomeWidgetSize(context)
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.cardElevated)
                    .border(0.8.dp, colors.cardBorder.copy(alpha = 0.75f), CircleShape)
                    .xvoxPressScale(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_xvox_arrow_left),
                    contentDescription = "Back to settings",
                    tint = colors.primaryText,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Widget Studio", color = colors.primaryAccent, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (liveSize.hasHomeWidget) "Live ${liveSize.label} home widget" else "Add a home widget to use its live size",
                    color = colors.secondaryText,
                    fontSize = 11.sp
                )
            }
        }

        WidgetStudioLivePreview(
            state = state,
            playerState = playerState,
            liveSize = liveSize,
            previewBackground = previewBackground,
            previewPhotoUri = previewPhotoUri ?: state.backgroundImageUri,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (liveSize.rows >= 2) 226.dp else 184.dp)
                .padding(horizontal = 12.dp)
        )

        WidgetStudioBackgroundSelector(
            selected = previewBackground,
            onSelect = { mode ->
                if (mode == WidgetStudioPreviewBackground.PHOTO &&
                    previewPhotoUri == null && state.backgroundImageUri.isNullOrBlank()
                ) {
                    // No configured app photo exists, so make Photo a real selectable preview
                    // surface rather than an empty placeholder.
                    previewPhotoPicker.launch("image/*")
                } else {
                    previewBackground = mode
                }
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WidgetSettingsSection(
                state = state,
                viewModel = viewModel,
                liveSize = liveSize
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

/** Preview-only surfaces available in Widget Studio; none changes the launcher wallpaper. */
private object WidgetStudioPreviewBackground {
    const val WALLPAPER = "wallpaper"
    const val PHOTO = "photo"
    const val DARK = "dark"
    const val LIGHT = "light"
    const val GRADIENT = "gradient"

    val options = listOf(
        WALLPAPER to "Wallpaper",
        PHOTO to "Photo",
        DARK to "Dark",
        LIGHT to "Light",
        GRADIENT to "Gradient"
    )
}

@Composable
private fun WidgetStudioBackgroundSelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Preview background", color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WidgetStudioPreviewBackground.options.forEach { (key, label) ->
                val selectedOption = selected == key
                val shape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .clip(shape)
                        .background(if (selectedOption) colors.primaryAccent else colors.cardElevated)
                        .border(.8.dp, if (selectedOption) colors.primaryAccent else colors.cardBorder.copy(alpha = .78f), shape)
                        .xvoxPressScale { onSelect(key) }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selectedOption) colors.background else colors.secondaryText,
                        fontSize = 10.5.sp,
                        fontWeight = if (selectedOption) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetStudioLivePreview(
    state: SettingsState,
    playerState: com.xvox.music.player.playback.MainPlayerUiState,
    liveSize: XvoxWidgetHelper.LiveWidgetSize,
    previewBackground: String,
    previewPhotoUri: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = XvoxTheme.colors
    val customization = state.widgetSizes[liveSize.sizeKey] ?: state.widgetCustomization
    val currentSong = playerState.queue.getOrNull(playerState.currentIndex)
        ?.takeIf { it.id == playerState.currentSongId }
        ?: playerState.queue.firstOrNull { it.id == playerState.currentSongId }
    val display = remember(
        customization,
        liveSize.sizeKey,
        state.widgetTransparency,
        state.widgetTheme,
        state.widgetCustomColor,
        state.widgetShowLogo,
        state.widgetCornerRadius,
        state.widgetPaddingX,
        state.widgetPaddingY,
        state.accentColor,
        currentSong,
        playerState.isPlaying,
        playerState.currentSongId
    ) {
        XvoxWidgetHelper.WidgetDisplayState(
            songTitle = currentSong?.title ?: "Your favourite track",
            songArtist = currentSong?.artist ?: "XVOX Music",
            artworkUri = currentSong?.artworkUri,
            isPlaying = currentSong?.let { playerState.isPlaying } ?: true,
            // Widget Studio has no library-like flow of its own; the live installed widget still
            // receives the authoritative liked state from XvoxAppWidgetProvider.
            isLiked = false,
            currentPosition = 0L,
            duration = currentSong?.duration ?: 0L,
            transparency = state.widgetTransparency,
            theme = state.widgetTheme,
            customColor = state.widgetCustomColor,
            showLogo = state.widgetShowLogo,
            cornerRadiusDp = state.widgetCornerRadius,
            paddingX = state.widgetPaddingX,
            paddingY = state.widgetPaddingY,
            customization = customization,
            fallbackSeed = currentSong?.id?.hashCode() ?: 11,
            appAccentColor = state.accentColor
        )
    }
    val remoteViews by produceState<android.widget.RemoteViews?>(
        null,
        display,
        liveSize.widthDp,
        liveSize.heightDp
    ) {
        value = XvoxWidgetHelper.buildRemoteViews(
            context = context,
            state = display,
            widthDp = liveSize.widthDp,
            heightDp = liveSize.heightDp,
            interactive = false,
            preferStateCustomization = true
        )
    }
    // Reading and rasterising a launcher wallpaper can be expensive. Only do that work for its
    // selected preview mode; dark/light/gradient and a supplied app photo stay instantaneous.
    val wallpaper by produceState<android.graphics.Bitmap?>(null, context, previewBackground) {
        value = if (previewBackground == WidgetStudioPreviewBackground.WALLPAPER) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val drawable = WallpaperManager.getInstance(context).drawable ?: return@runCatching null
                    val width = drawable.intrinsicWidth.coerceIn(1, 1080)
                    val height = drawable.intrinsicHeight.coerceIn(1, 1080)
                    drawable.toBitmap(width = width, height = height)
                }.getOrNull()
            }
        } else null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.cardElevated)
            .border(0.8.dp, colors.cardBorder.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
    ) {
        when (previewBackground) {
            WidgetStudioPreviewBackground.WALLPAPER -> wallpaper?.let { bitmap ->
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            WidgetStudioPreviewBackground.PHOTO -> {
                val photo = previewPhotoUri
                if (!photo.isNullOrBlank()) {
                    coil3.compose.AsyncImage(
                        model = android.net.Uri.parse(photo),
                        contentDescription = "Selected photo preview background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(
                        Brush.linearGradient(listOf(Color(0xFF596778), Color(0xFF26333F)))
                    ))
                }
            }
            WidgetStudioPreviewBackground.DARK -> Box(Modifier.fillMaxSize().background(Color(0xFF111216)))
            WidgetStudioPreviewBackground.LIGHT -> Box(Modifier.fillMaxSize().background(Color(0xFFF2F3F5)))
            WidgetStudioPreviewBackground.GRADIENT -> Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(colors.primaryAccent.copy(alpha = .82f), Color(0xFF332354), Color(0xFF0D3D50)))
                )
            )
        }
        val shade = when (previewBackground) {
            WidgetStudioPreviewBackground.LIGHT -> Color.White.copy(alpha = .10f)
            WidgetStudioPreviewBackground.DARK -> Color.Black.copy(alpha = .16f)
            WidgetStudioPreviewBackground.PHOTO -> Color.Black.copy(alpha = .26f)
            WidgetStudioPreviewBackground.GRADIENT -> Color.Black.copy(alpha = .20f)
            else -> Color.Black.copy(alpha = if (wallpaper == null) 0.12f else 0.28f)
        }
        Box(Modifier.fillMaxSize().background(shade))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val previewLabelColor = if (previewBackground == WidgetStudioPreviewBackground.LIGHT) {
                Color(0xFF17181B)
            } else Color.White
            Text(
                text = if (liveSize.hasHomeWidget) "Live home layout · ${liveSize.label}" else "Preview · ${liveSize.label}",
                color = previewLabelColor,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
            UniformPreview(
                width = liveSize.widthDp.dp,
                height = liveSize.heightDp.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 7.dp, bottom = 3.dp)
            ) {
                AndroidView(
                    factory = { FrameLayout(it) },
                    modifier = Modifier.fillMaxSize(),
                    update = { host ->
                        remoteViews?.let { remote ->
                            if (host.tag !== remote) {
                                host.removeAllViews()
                                host.addView(remote.apply(context, host))
                                host.tag = remote
                            }
                        }
                    }
                )
            }
        }
    }
}
