package com.xvox.music.features.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.UserPreferences
import com.xvox.music.features.setup.PfpCarousel
import com.xvox.music.features.setup.PfpType

@Composable
fun ProfileEditorBox(
    profile: UserPreferences,
    onCancel: () -> Unit,
    onSave: (
        String,
        String,
        String?,
    ) -> Unit,
) {
    val colors =
        XvoxTheme.colors

    var name by remember(
        profile.username,
    ) {
        mutableStateOf(
            profile.username,
        )
    }

    var selected by remember(
        profile.selectedPfp,
    ) {
        mutableStateOf(
            runCatching {
                PfpType.valueOf(
                    profile.selectedPfp,
                )
            }.getOrDefault(
                PfpType.DEFAULT,
            ),
        )
    }

    var customUri by remember(
        profile.customPfpUri,
    ) {
        mutableStateOf(
            profile.customPfpUri
                ?.let(Uri::parse),
        )
    }

    val photoPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .PickVisualMedia(),
        ) { uri ->

            if (uri != null) {
                customUri = uri
                selected =
                    PfpType.CUSTOM
            }
        }

    // Fix: if Add (CUSTOM) selected but no photo, Okay should be disabled. Require photo for CUSTOM.
    val canSave = name.isNotBlank() && (selected != PfpType.CUSTOM || customUri != null)

    Column(
        modifier =
            Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Profile",
            color =
                colors.primaryText,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold,
            modifier =
                Modifier.padding(
                    end = 48.dp,
                ),
        )

        Spacer(
            Modifier.height(
                14.dp,
            ),
        )

        PfpCarousel(
            selected = selected,
            username = name,
            customPfpUri =
            customUri,
            onSelected = {
                selected = it
                // If user selects CUSTOM without photo, keep but cannot save until photo picked
                // If user leaves CUSTOM without photo, keep selected as CUSTOM but canSave false
            },
            onAddClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts
                            .PickVisualMedia
                            .ImageOnly,
                    ),
                )
            },
        )

        Spacer(
            Modifier.height(
                12.dp,
            ),
        )

        Text(
            text = "Rename",
            color =
                colors.secondaryText,
            fontSize = 11.sp,
        )

        Spacer(
            Modifier.height(
                6.dp,
            ),
        )

        BasicTextField(
            value = name,
            onValueChange = {
                if (
                    it.length <= 12
                ) {
                    name = it
                }
            },
            singleLine = true,
            textStyle =
                TextStyle(
                    color =
                        colors.primaryText,
                    fontSize = 14.sp,
                ),
            cursorBrush =
                SolidColor(
                    colors.primaryText,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        colors.card,
                        RoundedCornerShape(
                            14.dp,
                        ),
                    ),
            decorationBox = { field ->

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(
                                horizontal =
                                    14.dp,
                            ),
                    contentAlignment =
                        Alignment
                            .CenterStart,
                ) {
                    field()
                }
            },
        )

        Spacer(
            Modifier.height(
                14.dp,
            ),
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp,
                ),
        ) {
            ProfileEditorAction(
                title = "Cancel",
                enabled = true,
                onClick =
                onCancel,
                modifier =
                    Modifier.weight(1f),
            )

            ProfileEditorAction(
                title = "Okay",
                enabled =
                canSave,
                onClick = {
                    if (canSave) {
                        onSave(
                            name.trim(),
                            selected.name,
                            customUri
                                ?.toString(),
                        )
                    }
                },
                modifier =
                    Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProfileEditorAction(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors =
        XvoxTheme.colors

    Box(
        modifier =
            modifier
                .height(44.dp)
                .background(
                    colors.card,
                    RoundedCornerShape(
                        14.dp,
                    ),
                ).clickable(
                    enabled = enabled,
                    interactionSource =
                        remember {
                            MutableInteractionSource()
                        },
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment =
            Alignment.Center,
    ) {
        Text(
            text = title,
            color =
                if (enabled) {
                    colors.primaryText
                } else {
                    colors.mutedText
                },
            fontSize = 13.sp,
            fontWeight =
                FontWeight.SemiBold,
        )
    }
}
