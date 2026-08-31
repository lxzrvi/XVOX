package com.xvox.music.features.setup

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val colors = XvoxTheme.colors

    fun audioGranted(): Boolean {
        val permission =
            if (Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun notificationGranted(): Boolean {
        if (Build.VERSION.SDK_INT < 33) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    val audioLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            viewModel.updatePermissions(
                audioGranted(),
                notificationGranted()
            )
        }

    val notificationLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            viewModel.updatePermissions(
                audioGranted(),
                notificationGranted()
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
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 18.dp,
                    vertical = 10.dp
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "XVOX",
                    color = colors.primaryText,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center
            ) {
                Text(
                    text = "Let's get to know you",
                    color = colors.secondaryText,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = "Personalize your xvox",
                    color = colors.primaryText,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))

                PfpCarousel(
                    selected = state.selectedPfp,
                    username = state.name,
                    onSelected = viewModel::selectPfp
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "choose your pfp and enter",
                    color = colors.mutedText,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(4.dp))

                SetupNameField(
                    value = state.name,
                    onValueChange = viewModel::setName
                )
            }

            PermissionCard(
                audioGranted = state.audioGranted,
                notificationGranted =
                    state.notificationGranted,
                onAudioClick = {
                    val permission =
                        if (Build.VERSION.SDK_INT >= 33) {
                            Manifest.permission.READ_MEDIA_AUDIO
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }

                    audioLauncher.launch(permission)
                },
                onNotificationClick = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        notificationLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                }
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        (context as? Activity)?.finish()
                    },
                    modifier = Modifier
                        .height(44.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        colors.cardBorder
                    ),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor =
                                colors.primaryText
                        )
                ) {
                    Text("Exit")
                }

                Spacer(Modifier.padding(horizontal = 5.dp))

                Button(
                    onClick = onSetupComplete,
                    enabled = state.setupComplete,
                    modifier = Modifier
                        .height(44.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
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
                    Text("Start")
                }
            }
        }
    }
}
