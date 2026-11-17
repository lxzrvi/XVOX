package com.xvox.music.core.design.kit

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.UserPreferencesRepository
import com.xvox.music.features.setup.PfpIcon
import com.xvox.music.features.setup.PfpType
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.glass.render.backdrops.LayerBackdrop
import io.github.neoruaa.glasmio.ui.GlasmioTextField
import kotlinx.coroutines.launch

/**
 * Edit Profile as a small centred box of clear Glasmio glass: it refracts and samples the live
 * page behind it, keeps text-only actions (Cancel / Save) and animates in with the sheet spring.
 */
@Composable
fun XvoxEditProfileGlassBox(
    prefs: UserPreferencesRepository,
    backdrop: LayerBackdrop,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val colors = XvoxTheme.colors
    val glass = LocalGlassColors.current
    val saved by prefs.preferences.collectAsState(initial = null)

    var name by remember { mutableStateOf(TextFieldValue(saved?.username.orEmpty())) }
    var avatar by remember { mutableStateOf(saved?.selectedPfp ?: PfpType.DEFAULT.name) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val reveal by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = spring(dampingRatio = .82f, stiffness = Spring.StiffnessMediumLow),
        label = "profileBoxReveal"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .16f * reveal))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            backdrop = backdrop,
            modifier = Modifier
                .fillMaxWidth(.82f)
                .widthIn(max = 320.dp)
                .graphicsLayer {
                    alpha = reveal
                    val s = .92f + .08f * reveal
                    scaleX = s
                    scaleY = s
                }
                // The box itself must not swallow the outside-tap dismissal.
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Edit Profile",
                    color = glass.content,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))

                GlasmioTextField(
                    value = name.text,
                    onValueChange = { name = TextFieldValue(it) },
                    placeholder = "Your name",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))
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
                                    if (selected) colors.primaryAccent.copy(alpha = .22f)
                                    else Color.Transparent
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

                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(.5.dp)
                        .background(glass.separator)
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Cancel",
                        color = glass.secondaryContent,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Box(
                        Modifier
                            .widthIn(min = .5.dp)
                            .height(44.dp)
                            .background(glass.separator)
                    )
                    Text(
                        text = "Save",
                        color = if (name.text.isBlank()) glass.tertiaryContent else colors.primaryAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = name.text.isNotBlank()) {
                                scope.launch {
                                    prefs.saveProfile(name.text, avatar, null)
                                    onDismiss()
                                }
                            }
                            .padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
