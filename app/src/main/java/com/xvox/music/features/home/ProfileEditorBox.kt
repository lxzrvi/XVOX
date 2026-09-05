package com.xvox.music.features.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxPersonalFont
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.haptics.LocalXvoxHaptics
import com.xvox.music.data.preferences.UserPreferences
import com.xvox.music.features.setup.PfpIcon
import com.xvox.music.features.setup.PfpType

@Composable
fun ProfileEditorBox(
    profile: UserPreferences,
    onCancel: () -> Unit,
    onSave: (String, String, String?) -> Unit,
) {
    val colors = XvoxTheme.colors
    val haptics = LocalXvoxHaptics.current

    var name by remember(profile.username) { mutableStateOf(profile.username) }
    var selected by remember(profile.selectedPfp) {
        mutableStateOf(runCatching { PfpType.valueOf(profile.selectedPfp) }.getOrDefault(PfpType.DEFAULT))
    }
    var customUri by remember(profile.customPfpUri) {
        mutableStateOf(profile.customPfpUri?.let(Uri::parse))
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customUri = uri
            selected = PfpType.CUSTOM
        }
    }

    val canSave = name.isNotBlank() && (selected != PfpType.CUSTOM || customUri != null)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Edit Profile",
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Avatar selector row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(PfpType.entries.toTypedArray()) { type ->
                val isSelected = selected == type
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(colors.cardElevated)
                        .border(
                            width = if (isSelected) 2.dp else 0.7.dp,
                            color = if (isSelected) colors.primaryAccent else colors.cardBorder,
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptics.tap()
                            if (type == PfpType.CUSTOM) {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                selected = type
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        type == PfpType.DEFAULT -> {
                            Text(
                                text = name.firstOrNull()?.uppercase() ?: "X",
                                color = if (isSelected) colors.primaryAccent else colors.primaryText,
                                fontFamily = XvoxPersonalFont,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        type == PfpType.CUSTOM && customUri != null -> {
                            AsyncImage(
                                model = customUri,
                                contentDescription = "Custom picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(54.dp).clip(CircleShape)
                            )
                        }

                        type == PfpType.CUSTOM -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_xvox_add),
                                contentDescription = "Pick photo",
                                tint = if (isSelected) colors.primaryAccent else colors.primaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        else -> {
                            PfpIcon(
                                type = type,
                                color = if (isSelected) colors.primaryAccent else colors.primaryText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Username",
            color = colors.secondaryText,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        BasicTextField(
            value = name,
            onValueChange = { if (it.length <= 16) name = it },
            singleLine = true,
            textStyle = TextStyle(color = colors.primaryText, fontSize = 14.sp),
            cursorBrush = SolidColor(colors.primaryAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card),
            decorationBox = { field ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    field()
                }
            }
        )

        Spacer(Modifier.height(18.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(colors.cardElevated)
                    .clickable {
                        haptics.tap()
                        onCancel()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Cancel", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(if (canSave) colors.primaryAccent else colors.cardElevated)
                    .clickable(enabled = canSave) {
                        if (canSave) {
                            haptics.success()
                            onSave(name.trim(), selected.name, customUri?.toString())
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Save",
                    color = if (canSave) colors.background else colors.mutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
