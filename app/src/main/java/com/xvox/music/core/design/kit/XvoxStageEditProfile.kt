package com.xvox.music.core.design.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.icons.painterResource
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.setup.PfpIcon
import com.xvox.music.features.setup.PfpType
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioAlertFieldGroup
import io.github.neoruaa.glasmio.ui.GlasmioAlertTextField
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import kotlinx.coroutines.launch

/**
 * The profile button and everything behind it.
 *
 * Tapping the button morphs it into a small Glasmio menu (Edit profile / Settings). Editing the
 * name and avatar happens in Glasmio's own alert dialog, which is the component Glasmio's
 * `GlasmioAlertTextField` is built for. The earlier version packed the editor itself into the
 * menu's popup (a focusable window with a text field, buttons and a scroll area inside a morphing
 * surface), which is what was closing the app; the menu now only ever holds plain menu rows.
 */
@Composable
fun XvoxProfileMenu(
    prefs: UserPreferencesRepository,
    backdrop: Backdrop,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val glass = LocalGlassColors.current
    var menuOpen by remember { mutableStateOf(false) }
    var editOpen by remember { mutableStateOf(false) }
    // What to do once the menu has finished collapsing into its button: 1 = edit, 2 = settings.
    var afterClose by remember { mutableStateOf(0) }

    Box(modifier) {
        GlasmioMenu(
            expanded = menuOpen,
            onExpandedChange = { menuOpen = it },
            itemCount = 2,
            menuWidth = 220.dp,
            forceBelowAnchor = true,
            backdrop = backdrop,
            onClosed = {
                when (afterClose) {
                    1 -> editOpen = true
                    2 -> onOpenSettings()
                }
                afterClose = 0
            },
            anchor = { open ->
                GlasmioIconButton(onClick = open, backdrop = backdrop, size = 44.dp) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_user),
                        contentDescription = "Profile",
                        tint = colors.primaryText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        ) { menuBackdrop, close ->
            GlasmioMenuItem(
                title = "Edit profile",
                onClick = {
                    afterClose = 1
                    close()
                },
                backdrop = menuBackdrop,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_xvox_user),
                        contentDescription = null,
                        tint = glass.content,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            GlasmioMenuItem(
                title = "Settings",
                onClick = {
                    afterClose = 2
                    close()
                },
                systemName = "gear",
                backdrop = menuBackdrop
            )
        }
    }

    EditProfileDialog(
        visible = editOpen,
        prefs = prefs,
        backdrop = backdrop,
        onDismiss = { editOpen = false }
    )
}

@Composable
private fun EditProfileDialog(
    visible: Boolean,
    prefs: UserPreferencesRepository,
    backdrop: Backdrop,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val colors = XvoxTheme.colors
    val glass = LocalGlassColors.current
    val saved by prefs.preferences.collectAsState(initial = null)

    // Re-seeded every time the dialog opens, so a cancelled edit never leaks into the next one.
    var name by remember(visible) { mutableStateOf(TextFieldValue(saved?.username.orEmpty())) }
    var avatar by remember(visible) { mutableStateOf(saved?.selectedPfp ?: PfpType.DEFAULT.name) }

    GlasmioAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = "Edit Profile",
        backdrop = backdrop,
        buttons = listOf(
            GlasmioAlertButtonSpec("Cancel", onDismiss, GlasmioAlertButtonRole.Cancel),
            GlasmioAlertButtonSpec(
                label = "Save",
                onClick = {
                    scope.launch {
                        prefs.saveProfile(name.text.trim(), avatar, null)
                        onDismiss()
                    }
                },
                enabled = name.text.isNotBlank()
            )
        )
    ) {
        GlasmioAlertFieldGroup {
            GlasmioAlertTextField(
                value = name,
                onValueChange = { if (it.text.length <= 16) name = it },
                placeholder = "Your name"
            )
        }
        Text(
            text = "Avatar",
            color = glass.secondaryContent,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PfpType.entries.filter { it != PfpType.CUSTOM }.forEach { type ->
                val selected = avatar == type.name
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (selected) colors.primaryAccent.copy(alpha = .22f) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { avatar = type.name },
                    contentAlignment = Alignment.Center
                ) {
                    PfpIcon(
                        type = type,
                        color = if (selected) colors.primaryAccent else glass.content.copy(alpha = .6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
