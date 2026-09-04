package com.xvox.music.core.ui.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun XvoxB(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors =
        XvoxTheme.colors

    val scope =
        rememberCoroutineScope()

    var visible by
        remember {
            mutableStateOf(false)
        }

    var closing by
        remember {
            mutableStateOf(false)
        }

    fun close() {
        if (closing) {
            return
        }

        closing = true
        visible = false

        scope.launch {
            delay(150L)
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    BackHandler {
        close()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = 0.28f
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            tryAwaitRelease()
                        }
                    )
                }
                .imePadding(),
        contentAlignment =
            Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter =
                fadeIn(
                    tween(180)
                ),
            exit =
                fadeOut(
                    tween(150)
                )
        ) {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal =
                                18.dp,
                            vertical =
                                12.dp
                        )
            ) {
                val maximumHeight =
                    (
                        maxHeight -
                            24.dp
                        )
                        .coerceAtLeast(
                            120.dp
                        )

                val shape =
                    RoundedCornerShape(
                        24.dp
                    )

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                max =
                                    maximumHeight
                            )
                            .background(
                                colors.cardElevated,
                                shape
                            )
                            .pointerInput(
                                Unit
                            ) {
                                detectTapGestures(
                                    onPress = {
                                        tryAwaitRelease()
                                    }
                                )
                            }
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    max =
                                        maximumHeight
                                )
                                .verticalScroll(
                                    rememberScrollState()
                                )
                                .padding(
                                    start =
                                        16.dp,
                                    top =
                                        18.dp,
                                    end =
                                        16.dp,
                                    bottom =
                                        16.dp
                                )
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        end =
                                            44.dp
                                    )
                        ) {
                            content()
                        }
                    }

                    Box(
                        modifier =
                            Modifier
                                .align(
                                    Alignment.TopEnd
                                )
                                .padding(
                                    top = 14.dp,
                                    end = 14.dp
                                )
                                .size(
                                    36.dp
                                )
                                .background(
                                    colors.card,
                                    CircleShape
                                )
                                .clickable(
                                    interactionSource =
                                        remember {
                                            MutableInteractionSource()
                                        },
                                    indication =
                                        null,
                                    onClick =
                                        ::close
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    R.drawable
                                        .ic_xvox_close
                                ),
                            contentDescription =
                                "Close",
                            tint =
                                colors.primaryText,
                            modifier =
                                Modifier.size(
                                    17.dp
                                )
                        )
                    }
                }
            }
        }
    }
}
