package com.xvox.music.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.overlay.xvoxBoxScroll

/** Local timer input state reported live to XvoxMainShell as the user edits the sheet. */
data class XvoxTimerDraft(
    val minutes: Int,
    val seconds: Int = 0,
    val pauseMusic: Boolean = true,
    val closeApp: Boolean = false
)

/**
 * Content only: the parent supplies the fixed Cancel / [Off] / Okay footer. The selected draft is
 * reported immediately so the host can update the real timer live; Cancel and Okay only close.
 */
@Composable
fun XvoxTimerBoxContent(
    currentMinutes: Int?,
    draft: XvoxTimerDraft?,
    onDraftChange: (XvoxTimerDraft?) -> Unit
) {
    val colors = XvoxTheme.colors
    val scrollState = rememberScrollState()
    val presets = remember { listOf(5, 10, 15, 30, 45, 60) }
    val draftIsCustom = draft != null && (draft.seconds > 0 || draft.minutes !in presets)
    var showCustom by remember { mutableStateOf(draftIsCustom) }
    var minText by remember { mutableStateOf(if (draftIsCustom) draft?.minutes?.toString().orEmpty() else "") }
    var secText by remember { mutableStateOf(if (draftIsCustom && (draft?.seconds ?: 0) > 0) draft?.seconds?.toString().orEmpty() else "") }
    var pauseMusic by remember { mutableStateOf(draft?.pauseMusic ?: true) }
    var closeApp by remember { mutableStateOf(draft?.closeApp ?: false) }

    fun publishCustom(minutesText: String = minText, secondsText: String = secText) {
        onDraftChange(
            XvoxTimerDraft(
                minutes = minutesText.toIntOrNull()?.coerceIn(0, 999) ?: 0,
                seconds = (secondsText.toIntOrNull() ?: 0).coerceIn(0, 59),
                pauseMusic = pauseMusic,
                closeApp = closeApp
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .xvoxBoxScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TimerGroupBox(title = "Sleep timer") {
            Text(
                text = currentMinutes?.let { "$it min active" } ?: "Choose when playback should stop",
                color = if (currentMinutes != null) colors.primaryAccent else colors.secondaryText,
                fontSize = 12.sp,
                fontWeight = if (currentMinutes != null) FontWeight.SemiBold else FontWeight.Normal
            )
        }

        TimerGroupBox(title = "Quick time") {
            presets.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { minutes ->
                        val selected = draft?.minutes == minutes && draft.seconds == 0
                        val optionShape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(optionShape)
                                .background(if (selected) colors.primaryAccent else colors.card.copy(alpha = .78f))
                                .border(.7.dp, if (selected) colors.primaryAccent else colors.cardBorder.copy(alpha = .65f), optionShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showCustom = false
                                    onDraftChange(XvoxTimerDraft(minutes = minutes))
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$minutes min",
                                color = if (selected) colors.background else colors.primaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        TimerGroupBox(title = "Custom time") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Selecting Custom is itself the active choice; retain the original live
                        // draft behavior and let the fixed footer commit it without a second step.
                        if (!showCustom) {
                            showCustom = true
                            if (!draftIsCustom) {
                                minText = ""
                                secText = ""
                                pauseMusic = true
                                closeApp = false
                                publishCustom()
                            }
                        }
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Set a custom time",
                    color = colors.primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(if (showCustom) R.drawable.ic_xvox_collapse else R.drawable.ic_xvox_caret_right),
                    contentDescription = null,
                    tint = colors.secondaryText,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (showCustom) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimerNumberField(
                        value = minText,
                        hint = "Minutes",
                        maxLength = 3,
                        modifier = Modifier.weight(1f),
                        onValueChange = {
                            minText = it
                            publishCustom(minutesText = it)
                        }
                    )
                    TimerNumberField(
                        value = secText,
                        hint = "Seconds",
                        maxLength = 2,
                        modifier = Modifier.weight(1f),
                        onValueChange = {
                            secText = it
                            publishCustom(secondsText = it)
                        }
                    )
                }

                TimerActionChoice(
                    label = "Pause music",
                    selected = pauseMusic,
                    onClick = {
                        pauseMusic = true
                        closeApp = false
                        publishCustom()
                    }
                )
                TimerActionChoice(
                    label = "Close full app",
                    selected = closeApp,
                    onClick = {
                        closeApp = true
                        pauseMusic = false
                        publishCustom()
                    }
                )
            }
        }
    }
}

@Composable
private fun TimerGroupBox(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardElevated.copy(alpha = .70f))
            .border(.8.dp, colors.cardBorder.copy(alpha = .76f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(title, color = colors.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun TimerNumberField(
    value: String,
    hint: String,
    maxLength: Int,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    val colors = XvoxTheme.colors
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength && it.all(Char::isDigit)) onValueChange(it) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(color = colors.primaryText, fontSize = 14.sp),
        cursorBrush = SolidColor(colors.primaryAccent),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.card.copy(alpha = .78f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(hint, color = colors.mutedText, fontSize = 14.sp)
            inner()
        }
    )
}

@Composable
private fun TimerActionChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.accentSoft else colors.card.copy(alpha = .52f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, color = colors.primaryText, fontSize = 13.sp)
    }
}
