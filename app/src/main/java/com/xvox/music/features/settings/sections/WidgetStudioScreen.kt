package com.xvox.music.features.settings.sections

import android.app.WallpaperManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    var liveSize by remember { mutableStateOf(XvoxWidgetHelper.activeHomeWidgetSize(context)) }

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
            liveSize = liveSize,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (liveSize.rows >= 2) 226.dp else 184.dp)
                .padding(horizontal = 12.dp)
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

@Composable
private fun WidgetStudioLivePreview(
    state: SettingsState,
    liveSize: XvoxWidgetHelper.LiveWidgetSize,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = XvoxTheme.colors
    val customization = state.widgetSizes[liveSize.sizeKey] ?: state.widgetCustomization
    val display = remember(
        customization,
        liveSize.sizeKey,
        state.widgetTransparency,
        state.widgetTheme,
        state.widgetCustomColor,
        state.widgetShowLogo,
        state.widgetCornerRadius,
        state.widgetPaddingX,
        state.widgetPaddingY
    ) {
        XvoxWidgetHelper.WidgetDisplayState(
            songTitle = "Your favourite track",
            songArtist = "XVOX Music",
            artworkUri = null,
            isPlaying = true,
            isLiked = false,
            transparency = state.widgetTransparency,
            theme = state.widgetTheme,
            customColor = state.widgetCustomColor,
            showLogo = state.widgetShowLogo,
            cornerRadiusDp = state.widgetCornerRadius,
            paddingX = state.widgetPaddingX,
            paddingY = state.widgetPaddingY,
            customization = customization
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
    // Reading and rasterising a launcher wallpaper can be expensive. Load it off the Compose
    // frame so opening Widget Studio keeps the fixed live preview immediately responsive.
    val wallpaper by produceState<android.graphics.Bitmap?>(null, context) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = WallpaperManager.getInstance(context).drawable ?: return@runCatching null
                val width = drawable.intrinsicWidth.coerceIn(1, 1080)
                val height = drawable.intrinsicHeight.coerceIn(1, 1080)
                drawable.toBitmap(width = width, height = height)
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.cardElevated)
            .border(0.8.dp, colors.cardBorder.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
    ) {
        wallpaper?.let { bitmap ->
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (wallpaper == null) 0.12f else 0.28f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (liveSize.hasHomeWidget) "Live home layout · ${liveSize.label}" else "Preview · ${liveSize.label}",
                color = Color.White,
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
