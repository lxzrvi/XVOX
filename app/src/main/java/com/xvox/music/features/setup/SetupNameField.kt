package com.xvox.music.features.setup

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxPersonalFont
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay

@Composable
fun SetupNameField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val colors = XvoxTheme.colors
    val focusManager = LocalFocusManager.current
    val view = LocalView.current

    var showLimit by remember {
        mutableStateOf(false)
    }

    var shakeTrigger by remember {
        mutableIntStateOf(0)
    }

    var shakeX by remember {
        mutableStateOf(0f)
    }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger == 0) {
            return@LaunchedEffect
        }

        listOf(
            -7f,
            7f,
            -5f,
            5f,
            -2f,
            2f,
            0f
        ).forEach {
            shakeX = it
            delay(28)
        }

        delay(1200)
        showLimit = false
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = showLimit,
            enter = fadeIn() + slideInVertically {
                it / 2
            },
            exit = fadeOut() + slideOutVertically {
                it / 2
            }
        ) {
            Text(
                text = "enough is 12 character :(",
                color = colors.secondaryText,
                fontSize = 11.sp
            )
        }

        Box(
            modifier = Modifier
                .width(220.dp)
                .height(48.dp)
                .graphicsLayer {
                    translationX = shakeX
                },
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = value,
                onValueChange = { newValue ->
                    if (newValue.length <= 12) {
                        onValueChange(newValue)
                    } else {
                        showLimit = true
                        shakeTrigger += 1

                        view.performHapticFeedback(
                            HapticFeedbackConstants.REJECT
                        )
                    }
                },
                modifier = Modifier.width(220.dp),
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.primaryText,
                    fontFamily = XvoxPersonalFont,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(
                    colors.primaryAccent
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.width(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = "your name",
                                color = colors.mutedText,
                                fontFamily = XvoxPersonalFont,
                                fontSize = 22.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        innerTextField()
                    }
                }
            )
        }
    }
}
