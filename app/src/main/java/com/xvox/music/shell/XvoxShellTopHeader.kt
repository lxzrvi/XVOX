// EXACT CODE REPLACE KARO (poori file) - koi extra kaam nahi. Path: app/src/main/java/com/xvox/music/shell/XvoxShellTopHeader.kt
package com.xvox.music.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.blur.LocalXvoxHeaderSky
import com.xvox.music.core.ui.blur.LocalXvoxShellContentSky
import com.xvox.music.core.ui.blur.LocalXvoxSurfaceSky
import com.xvox.music.core.ui.blur.XvoxBlurLevel
import com.xvox.music.core.ui.blur.xvoxLiveBlur
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.data.preferences.UserPreferences
import com.xvox.music.features.home.HomeProfileAvatar
import com.xvox.music.features.playlist.XvoxHomeLibraryMode

/** Height of the (now empty) header slot below the status-bar area; pages keep their top offset. */
val XvoxShellTopHeaderBodyHeight = 66.dp

/**
 * The header no longer paints anything: no background, no blur, no text. It is only the spacer that
 * keeps every page's content starting below the floating controls ([XvoxShellFloatingControls]).
 */
@Composable
fun XvoxShellTopHeader(useSystemInsets: Boolean = true) {
    val density = LocalDensity.current
    val statusBarHeight = with(density) {
        if (useSystemInsets) WindowInsets.statusBars.getTop(this).toDp() else 0.dp
    }
    Spacer(Modifier.fillMaxWidth().height(statusBarHeight + XvoxShellTopHeaderBodyHeight))
}

private data class AlbumOption(val mode: XvoxHomeLibraryMode, val icon: Int, val label: String)

/**
 * Fixed floating controls (they do not scroll with the page): the profile button on the top right
 * and, while the Album tab is open, the Recent / Liked / Playlists option pill on the top left.
 * Only these buttons are live blurred; there is no header background.
 */
@Composable
fun XvoxShellFloatingControls(
    profile: UserPreferences,
    libraryMode: XvoxHomeLibraryMode,
    albumOptionsVisible: Boolean,
    onRecentClick: () -> Unit,
    onLikedClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val sky = LocalXvoxShellContentSky.current ?: LocalXvoxSurfaceSky.current
    val surface = colors.cardElevated.copy(alpha = colors.cardElevated.alpha * if (colors.isLight) .72f else .64f)
    var menuOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .height(54.dp)
    ) {
        // ---- Album options (left) -------------------------------------------------------------
        val optionsAlpha by animateFloatAsState(if (albumOptionsVisible) 1f else 0f, tween(220), label = "albumOptionsA")
        if (optionsAlpha > .01f) {
            val options = listOf(
                AlbumOption(XvoxHomeLibraryMode.RECENT, R.drawable.ic_xvox_recent, "Recently Played"),
                AlbumOption(XvoxHomeLibraryMode.LIKED, R.drawable.ic_xvox_heart, "Liked Songs"),
                AlbumOption(XvoxHomeLibraryMode.ALBUMS, R.drawable.ic_xvox_disc, "Albums")
            )
            val clicks = listOf(onRecentClick, onLikedClick, onAlbumsClick)
            val width = 6.dp + 40.dp * options.size.toFloat()
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer { alpha = optionsAlpha; translationY = -(1f - optionsAlpha) * 18.dp.toPx() }
                    .height(42.dp)
                    .width(width)
                    .xvoxLiveBlur(
                        shape = RoundedCornerShape(21.dp),
                        level = XvoxBlurLevel.Chrome,
                        baseColor = surface,
                        sky = sky,
                        borderAlpha = 0f,
                        borderWidth = 0.dp,
                        edgeInBlur = true
                    )
            ) {
                val selected = options.indexOfFirst { it.mode == libraryMode }
                val x by animateFloatAsState(
                    if (selected >= 0) selected.toFloat() else 0f,
                    spring(dampingRatio = .66f, stiffness = 430f), label = "albumOptionX"
                )
                val indicatorAlpha by animateFloatAsState(if (selected >= 0) 1f else 0f, tween(170), label = "albumOptionA")
                val slot = with(LocalDensity.current) { 40.dp.roundToPx() }
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 3.dp)
                        .offset { IntOffset((x * slot).toInt(), 0) }
                        .size(36.dp)
                        .graphicsLayer { alpha = indicatorAlpha }
                        .background(colors.primaryAccent.copy(alpha = .20f), CircleShape)
                )
                Row(Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    options.forEachIndexed { i, option ->
                        val current = option.mode == libraryMode
                        val tint by animateColorAsState(
                            if (current) colors.primaryAccent else colors.primaryText.copy(alpha = .70f),
                            tween(220), label = "albumOptionTint"
                        )
                        Box(
                            Modifier.size(width = 40.dp, height = 36.dp).clip(CircleShape)
                                .xvoxPressScale(pressedScale = .86f, onClick = clicks[i]),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(option.icon), option.label, tint = tint, modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }
        }

        // ---- Profile (right) --------------------------------------------------------------------
        Box(Modifier.align(Alignment.CenterEnd)) {
            CompositionLocalProvider(LocalXvoxHeaderSky provides sky) {
                HomeProfileAvatar(profile = profile, modifier = Modifier.size(42.dp), onClick = { menuOpen = true })
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = colors.background.copy(alpha = if (colors.isLight) .96f else .94f)
            ) {
                @Composable
                fun item(label: String, icon: Int, onClick: () -> Unit) {
                    DropdownMenuItem(
                        text = { Text(label, color = colors.primaryText) },
                        leadingIcon = { Icon(painterResource(icon), null, tint = colors.primaryText.copy(alpha = .85f), modifier = Modifier.size(20.dp)) },
                        onClick = { menuOpen = false; onClick() }
                    )
                }
                item("Settings", R.drawable.ic_xvox_settings, onOpenSettings)
                item("Edit profile", R.drawable.ic_xvox_artist, onEditProfile)
                item("Refresh library", R.drawable.ic_xvox_refresh, onRefresh)
            }
        }
    }
}
