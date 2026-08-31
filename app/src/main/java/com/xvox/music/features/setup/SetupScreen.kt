package com.xvox.music.features.setup

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    fun audioGranted(): Boolean {
        val permission =
            if (Build.VERSION.SDK_INT >= 33)
                Manifest.permission.READ_MEDIA_AUDIO
            else
                Manifest.permission.READ_EXTERNAL_STORAGE

        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun notificationGranted(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
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

    val audioLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            viewModel.updatePermissions(
                audioGranted(),
                notificationGranted()
            )

            if (
                Build.VERSION.SDK_INT >= 33 &&
                !notificationGranted()
            ) {
                notificationLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }

    val imageLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let(viewModel::setCustomPfp)
        }

    LaunchedEffect(Unit) {
        viewModel.updatePermissions(
            audioGranted(),
            notificationGranted()
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.weight(0.35f))

            Text(
                text = "XVOX",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Let's get to know you",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Personalize your xvox",
                style = MaterialTheme.typography.headlineMedium,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(26.dp))

            PfpCarousel(
                selected = state.selectedPfp,
                onSelected = { type ->
                    if (type == PfpType.CUSTOM) {
                        imageLauncher.launch("image/*")
                    } else {
                        viewModel.selectPfp(type)
                    }
                }
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "choose your pfp and enter",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "your name",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                )
            )

            Spacer(Modifier.height(22.dp))

            PermissionCard(
                audioGranted = state.audioGranted,
                notificationGranted = state.notificationGranted,
                onRequestPermissions = {
                    val permission =
                        if (Build.VERSION.SDK_INT >= 33)
                            Manifest.permission.READ_MEDIA_AUDIO
                        else
                            Manifest.permission.READ_EXTERNAL_STORAGE

                    if (!audioGranted()) {
                        audioLauncher.launch(permission)
                    } else if (
                        Build.VERSION.SDK_INT >= 33 &&
                        !notificationGranted()
                    ) {
                        notificationLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                }
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedButton(
                    onClick = {
                        (context as? Activity)?.finish()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Exit")
                }

                Button(
                    onClick = onSetupComplete,
                    enabled = state.setupComplete,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Start")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PfpCarousel(
    selected: PfpType,
    onSelected: (PfpType) -> Unit
) {
    val items = PfpType.entries

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(items) { item ->

            val isSelected = item == selected

            Box(
                modifier = Modifier
                    .size(if (isSelected) 88.dp else 62.dp)
                    .graphicsLayer {
                        alpha = if (isSelected) 1f else 0.6f
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary
                                ),
                                CircleShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable {
                        onSelected(item)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pfpSymbol(item),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

private fun pfpSymbol(type: PfpType): String =
    when (type) {
        PfpType.HEART -> "♥"
        PfpType.STAR -> "★"
        PfpType.CIRCLE -> "●"
        PfpType.DIAMOND -> "◆"
        PfpType.HEXAGON -> "⬢"
        PfpType.CUSTOM -> "+"
    }

@Composable
private fun PermissionCard(
    audioGranted: Boolean,
    notificationGranted: Boolean,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRequestPermissions),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "permissions",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(18.dp))

            PermissionRow(
                title = "Audio and storage access",
                granted = audioGranted
            )

            Spacer(Modifier.height(16.dp))

            PermissionRow(
                title = "Notifications permission",
                granted = notificationGranted
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = if (granted) "✓" else "X",
            color = if (granted) {
                androidx.compose.ui.graphics.Color(0xFF57D38C)
            } else {
                MaterialTheme.colorScheme.error
            },
            style = MaterialTheme.typography.titleMedium
        )
    }
}
