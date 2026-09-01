package com.xvox.music.features.setup

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxItalicFont
import com.xvox.music.core.design.theme.XvoxLogoFont
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = viewModel()
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val state by viewModel.state.collectAsState()
    val colors = XvoxTheme.colors

    val imeVisible =
        WindowInsets.ime.getBottom(
            density
        ) > 0

    val carouselKeyboardOffset by
        animateDpAsState(
            targetValue =
                if (imeVisible) {
                    (-92).dp
                } else {
                    0.dp
                },
            animationSpec = tween(
                durationMillis = 260,
                easing =
                    FastOutSlowInEasing
            ),
            label =
                "carouselKeyboardOffset"
        )

    fun audioGranted(): Boolean {
        val permission =
            if (Build.VERSION.SDK_INT >= 33) {
                Manifest.permission
                    .READ_MEDIA_AUDIO
            } else {
                Manifest.permission
                    .READ_EXTERNAL_STORAGE
            }

        return ContextCompat
            .checkSelfPermission(
                context,
                permission
            ) ==
            PackageManager
                .PERMISSION_GRANTED
    }

    fun notificationGranted(): Boolean {
        if (Build.VERSION.SDK_INT < 33) {
            return true
        }

        return ContextCompat
            .checkSelfPermission(
                context,
                Manifest.permission
                    .POST_NOTIFICATIONS
            ) ==
            PackageManager
                .PERMISSION_GRANTED
    }

    val audioLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestPermission()
        ) {
            viewModel.updatePermissions(
                audioGranted(),
                notificationGranted()
            )
        }

    val notificationLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestPermission()
        ) {
            viewModel.updatePermissions(
                audioGranted(),
                notificationGranted()
            )
        }

    val photoPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .PickVisualMedia()
        ) { uri ->
            uri?.let(
                viewModel::setCustomPfp
            )
        }

    LaunchedEffect(Unit) {
        viewModel.updatePermissions(
            audioGranted(),
            notificationGranted()
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                colors.background
            )
            .windowInsetsPadding(
                WindowInsets.statusBars
                    .union(
                        WindowInsets
                            .navigationBars
                    )
            )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    bottom = 10.dp
                )
        ) {
            val screenHeight =
                maxHeight

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(
                        y =
                            screenHeight *
                                0.13f
                    ),
                horizontalAlignment =
                    Alignment
                        .CenterHorizontally
            ) {
                Text(
                    text = "XVOX",
                    modifier =
                        Modifier.offset(
                            y = (-32).dp
                        ),
                    color =
                        colors.primaryText,
                    fontFamily =
                        XvoxLogoFont,
                    fontSize = 37.sp,
                    textAlign =
                        TextAlign.Center
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            26.dp
                        )
                )

                Text(
                    text =
                        "Let's get to know you",
                    color =
                        colors.secondaryText,
                    fontSize = 14.sp,
                    textAlign =
                        TextAlign.Center
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )

                Text(
                    text =
                        "Personalize your xvox",
                    color =
                        colors.primaryText,
                    fontFamily =
                        XvoxItalicFont,
                    fontSize = 23.sp,
                    textAlign =
                        TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(
                        y =
                            screenHeight *
                                0.36f +
                                carouselKeyboardOffset
                    ),
                horizontalAlignment =
                    Alignment
                        .CenterHorizontally
            ) {
                PfpCarousel(
                    selected =
                        state.selectedPfp,
                    username =
                        state.name,
                    customPfpUri =
                        state.customPfpUri,
                    onSelected =
                        viewModel::selectPfp,
                    onAddClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts
                                    .PickVisualMedia
                                    .ImageOnly
                            )
                        )
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                Text(
                    text =
                        "choose your pfp and enter",
                    color =
                        colors.mutedText,
                    fontSize = 12.sp,
                    textAlign =
                        TextAlign.Center
                )

                SetupNameField(
                    value =
                        state.name,
                    onValueChange =
                        viewModel::setName
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(
                        Alignment
                            .BottomCenter
                    )
            ) {
                PermissionCard(
                    audioGranted =
                        state.audioGranted,
                    notificationGranted =
                        state.notificationGranted,
                    onAudioClick = {
                        val permission =
                            if (
                                Build.VERSION
                                    .SDK_INT >= 33
                            ) {
                                Manifest.permission
                                    .READ_MEDIA_AUDIO
                            } else {
                                Manifest.permission
                                    .READ_EXTERNAL_STORAGE
                            }

                        if (
                            !audioGranted()
                        ) {
                            audioLauncher.launch(
                                permission
                            )
                        }
                    },
                    onNotificationClick = {
                        if (
                            Build.VERSION
                                .SDK_INT >= 33 &&
                            !notificationGranted()
                        ) {
                            notificationLauncher
                                .launch(
                                    Manifest.permission
                                        .POST_NOTIFICATIONS
                                )
                        }
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        ),
                    verticalAlignment =
                        Alignment
                            .CenterVertically
                ) {
                    SetupCloseButton(
                        onClick = {
                            (
                                context
                                    as? Activity
                                )?.finish()
                        }
                    )

                    Button(
                        onClick =
                            onSetupComplete,
                        enabled =
                            state.setupComplete,
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(
                                    48.dp
                                ),
                        shape =
                            RoundedCornerShape(
                                18.dp
                            ),
                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        colors.primaryAccent,
                                    contentColor =
                                        colors.background,
                                    disabledContainerColor =
                                        colors.accentSoft,
                                    disabledContentColor =
                                        colors.mutedText
                                )
                    ) {
                        Text(
                            text = "Start",
                            fontWeight =
                                FontWeight
                                    .SemiBold
                        )
                    }
                }
            }
        }
    }
}
