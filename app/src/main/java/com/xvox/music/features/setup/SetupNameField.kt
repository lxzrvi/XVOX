package com.xvox.music.features.setup

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay
import com.xvox.music.core.design.theme.XvoxPersonalFont

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

        val positions = listOf(
            -8f,
            8f,
            -6f,
            6f,
            -3f,
            3f,
            0f
        )

        positions.forEach { position ->
            shakeX = position
            delay(35)
        }

        delay(1300)
        showLimit = false
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
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

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        OutlinedTextField(
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
            modifier = Modifier
                .width(210.dp)
                .height(50.dp)
                .graphicsLayer {
                    translationX = shakeX
                },
            placeholder = {
                Text(
                    text = "your name",
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.mutedText,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            },
            textStyle = TextStyle(
                color = colors.primaryText,
                fontFamily = XvoxPersonalFont,
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.primaryText,
                unfocusedTextColor = colors.primaryText,
                focusedBorderColor = colors.primaryAccent,
                unfocusedBorderColor = colors.cardBorder,
                focusedContainerColor = colors.card,
                unfocusedContainerColor = colors.card,
                cursorColor = colors.primaryAccent
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            )
        )
    }
}
