package com.xvox.music.features.settings.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.chrome.XvoxChromeStyle
import com.xvox.music.core.ui.components.XvoxImageCropDialog
import com.xvox.music.core.ui.effects.xvoxPressScale
import com.xvox.music.core.ui.overlay.xvoxBoxScroll
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Draft-only body for the long-press Mini Player / Navbar editor.  Its caller owns the fixed
 * Cancel, Reset, and Okay footer and commits [chrome] only on Okay.
 */
@Composable
fun MiniPlayerSettingsBoxContent(
    chrome: XvoxChromeStyle,
    onChromeChange: (XvoxChromeStyle) -> Unit
) {
    val colors = XvoxTheme.colors
    val scrollState = androidx.compose.foundation.rememberScrollState()
    var cropNavImageUri by remember { mutableStateOf<Uri?>(null) }

    val navbarPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) cropNavImageUri = uri
    }

    if (cropNavImageUri != null) {
        XvoxImageCropDialog(
            sourceUri = cropNavImageUri!!,
            isCircle = false,
            aspectRatio = 3.8f,
            onCropped = { cropped ->
                cropNavImageUri = null
                // The cropper writes an app-owned image.  It is assigned only to navigationImageUri
                // and never to the Header preference.
                onChromeChange(chrome.copy(navigationImageUri = cropped.toString()))
            },
            onDismiss = { cropNavImageUri = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .xvoxBoxScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text("Mini Player", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Cover style", color = colors.secondaryText, fontSize = 11.sp)
            SettingsChoiceRow(
                options = listOf("default" to "Standard Box", "full" to "Full Cover Artwork"),
                selected = if (chrome.miniCoverStyle == "full") "full" else "default",
                onSelect = { value -> onChromeChange(chrome.copy(miniCoverStyle = value)) }
            )
        }

        ChromeSlider(
            label = "Mini Player corner radius",
            value = chrome.miniCornerRadius.coerceIn(6f, 32f),
            range = 6f..32f,
            default = 15f,
            valueText = { "${it.roundToInt()} dp" },
            contentDescription = "Mini Player corner radius",
            onChange = { onChromeChange(chrome.copy(miniCornerRadius = it.coerceIn(6f, 32f))) }
        )

        // The control reads in the same direction as its label: 0% is opaque and 100% is clear.
        ChromeSlider(
            label = "Mini Player transparency",
            value = 1f - chrome.miniBgAlpha.coerceIn(0f, 1f),
            range = 0f..1f,
            default = .06f,
            valueText = { "${(it * 100).roundToInt()}%" },
            contentDescription = "Mini Player transparency",
            onChange = { transparency ->
                onChromeChange(chrome.copy(miniBgAlpha = (1f - transparency).coerceIn(0f, 1f)))
            }
        )

        Text("Mini Player position", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        ChromeSlider(
            label = "Left / Right",
            value = chrome.miniPlayerOffsetX.coerceIn(-220f, 220f),
            range = -220f..220f,
            default = 0f,
            valueText = ::positionXText,
            contentDescription = "Mini Player horizontal position",
            onChange = { onChromeChange(chrome.copy(miniPlayerOffsetX = it.coerceIn(-220f, 220f))) }
        )
        ChromeSlider(
            label = "Up / Down",
            value = chrome.miniPlayerOffsetY.coerceIn(-260f, 260f),
            range = -260f..260f,
            default = 0f,
            valueText = ::positionYText,
            contentDescription = "Mini Player vertical position",
            onChange = { onChromeChange(chrome.copy(miniPlayerOffsetY = it.coerceIn(-260f, 260f))) }
        )

        Text("Navigation Bar", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        ChromeSlider(
            label = "Navbar height",
            value = chrome.navigationBarHeight.coerceIn(52f, 88f),
            range = 52f..88f,
            default = 62f,
            valueText = { "${it.roundToInt()} dp" },
            contentDescription = "Navbar height",
            onChange = { onChromeChange(chrome.copy(navigationBarHeight = it.coerceIn(52f, 88f))) }
        )

        ChromeSlider(
            label = "Navbar width",
            value = chrome.navigationBarWidth.coerceIn(190f, 380f),
            range = 190f..380f,
            default = 246f,
            valueText = { "${it.roundToInt()} dp" },
            contentDescription = "Navbar width",
            onChange = { onChromeChange(chrome.copy(navigationBarWidth = it.coerceIn(190f, 380f))) }
        )

        ChromeSlider(
            label = "Navbar transparency",
            value = 1f - chrome.navBgAlpha.coerceIn(0f, 1f),
            range = 0f..1f,
            default = .12f,
            valueText = { "${(it * 100).roundToInt()}%" },
            contentDescription = "Navbar transparency",
            onChange = { transparency ->
                onChromeChange(chrome.copy(navBgAlpha = (1f - transparency).coerceIn(0f, 1f)))
            }
        )

        Text("Navbar position", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        ChromeSlider(
            label = "Left / Right",
            value = chrome.navigationBarOffsetX.coerceIn(-220f, 220f),
            range = -220f..220f,
            default = 0f,
            valueText = ::positionXText,
            contentDescription = "Navbar horizontal position",
            onChange = { onChromeChange(chrome.copy(navigationBarOffsetX = it.coerceIn(-220f, 220f))) }
        )
        ChromeSlider(
            label = "Up / Down",
            value = chrome.navigationBarOffsetY.coerceIn(-260f, 260f),
            range = -260f..260f,
            default = 0f,
            valueText = ::positionYText,
            contentDescription = "Navbar vertical position",
            onChange = { onChromeChange(chrome.copy(navigationBarOffsetY = it.coerceIn(-260f, 260f))) }
        )

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Navbar image", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Independent from the Header and status-bar image.",
                color = colors.secondaryText,
                fontSize = 11.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NavbarImageChoice(
                    title = "Default",
                    active = chrome.navigationImageUri.isBlank(),
                    modifier = Modifier.weight(1f),
                    onClick = { onChromeChange(chrome.copy(navigationImageUri = "")) }
                )
                NavbarImageChoice(
                    title = if (chrome.navigationImageUri.isBlank()) "Custom" else "Custom ✓",
                    active = chrome.navigationImageUri.isNotBlank(),
                    imageUri = chrome.navigationImageUri,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        navbarPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }
        }
    }
}

private fun positionXText(value: Float): String = when {
    value < -0.5f -> "Left ${abs(value).roundToInt()} dp"
    value > 0.5f -> "Right ${value.roundToInt()} dp"
    else -> "Centered"
}

private fun positionYText(value: Float): String = when {
    value < -0.5f -> "Up ${abs(value).roundToInt()} dp"
    value > 0.5f -> "Down ${value.roundToInt()} dp"
    else -> "Default"
}

@Composable
private fun ChromeSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    default: Float,
    valueText: (Float) -> String,
    contentDescription: String,
    onChange: (Float) -> Unit
) {
    val colors = XvoxTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = colors.secondaryText, fontSize = 11.sp)
            Text(valueText(value), color = colors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        XvoxContinuousSlider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            defaultValue = default,
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun NavbarImageChoice(
    title: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    imageUri: String = "",
    onClick: () -> Unit
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
        if (imageUri.isNotBlank()) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(Modifier.matchParentSize().background(colors.background.copy(alpha = if (active) .38f else .62f)))
        }
        Text(
            text = title,
            color = if (imageUri.isNotBlank()) Color.White else if (active) colors.background else colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
