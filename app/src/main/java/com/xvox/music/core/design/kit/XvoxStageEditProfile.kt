package com.xvox.music.core.design.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.setup.PfpIcon
import com.xvox.music.features.setup.PfpType
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.render.backdrops.LayerBackdrop
import io.github.neoruaa.glasmio.ui.GlasmioAlertFieldGroup
import io.github.neoruaa.glasmio.ui.GlasmioAlertTextField
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import io.github.neoruaa.glasmio.ui.GlasmioTextSelectionHost
import kotlinx.coroutines.launch

/** Width and height of the profile popover; the menu needs both up front to size its morph. */
val XvoxProfilePanelWidth: Dp = 300.dp
val XvoxProfilePanelHeight: Dp = 340.dp

/**
 * Content of the profile popover: Edit Profile (name + avatar) with a Settings shortcut.
 *
 * It lives inside a `GlasmioMenu` anchored on the profile button, so the button itself morphs into
 * this panel and the panel collapses back into the very same button - there is no separate dialog
 * to open or dismiss, which is what used to flash over the closing button.
 *
 * A popup is its own window, so it mounts its own text host: the name field gets Glasmio's glass
 * caret loupe, selection handles and liquid Edit Menu.
 */
@Composable
fun XvoxProfilePanel(
    prefs: UserPreferencesRepository,
    menuBackdrop: LayerBackdrop,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val colors = XvoxTheme.colors
    val glass = LocalGlassColors.current
    val saved by prefs.preferences.collectAsState(initial = null)

    var name by remember(saved?.username) { mutableStateOf(TextFieldValue(saved?.username.orEmpty())) }
    var avatar by remember(saved?.selectedPfp) { mutableStateOf(saved?.selectedPfp ?: PfpType.DEFAULT.name) }

    // The menu hands its content unbounded height. A scroll container (or the host's own
    // fillMaxSize) measured against that throws and closed the app, so the host gets an explicit,
    // bounded size and the column inside is a plain, non-scrolling Column that fits it.
    GlasmioTextSelectionHost(
        modifier = Modifier
            .fillMaxWidth()
            .height(XvoxProfilePanelHeight - 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = "Edit Profile",
                color = glass.content,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            GlasmioAlertFieldGroup {
                GlasmioAlertTextField(
                    value = name,
                    onValueChange = { if (it.text.length <= 16) name = it },
                    placeholder = "Your name"
                )
            }

            Text("Avatar", color = glass.secondaryContent, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PfpType.entries.filter { it != PfpType.CUSTOM }.forEach { type ->
                    val selected = avatar == type.name
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) colors.primaryAccent.copy(alpha = .22f) else Color.Transparent
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { avatar = type.name },
                        contentAlignment = Alignment.Center
                    ) {
                        PfpIcon(
                            type = type,
                            color = if (selected) colors.primaryAccent else glass.tertiaryContent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlasmioButton(
                    onClick = onClose,
                    backdrop = menuBackdrop,
                    size = GlasmioButtonSize.Medium,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel", color = glass.content, fontSize = 15.sp) }
                GlasmioButton(
                    onClick = {
                        scope.launch {
                            prefs.saveProfile(name.text.trim(), avatar, null)
                            onClose()
                        }
                    },
                    backdrop = menuBackdrop,
                    enabled = name.text.isNotBlank(),
                    emphasis = GlassEmphasis.Prominent,
                    size = GlasmioButtonSize.Medium,
                    modifier = Modifier.weight(1f)
                ) { Text("Save", color = glass.content, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            }

            Spacer(Modifier.height(8.dp))
            GlasmioMenuItem(
                title = "Settings",
                onClick = onOpenSettings,
                systemName = "gear",
                backdrop = menuBackdrop
            )
        }
    }
}
