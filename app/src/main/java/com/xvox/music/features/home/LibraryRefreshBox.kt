package com.xvox.music.features.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun LibraryRefreshBox(
    currentTotal: Int,
    scanning: Boolean,
    result: LibraryRefreshResult?,
    onScan: () -> Unit,
    onCancel: () -> Unit
) {
    val colors =
        XvoxTheme.colors

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Text(
            text =
                "Refresh library",
            color =
                colors.primaryText,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(
                14.dp
            )
        )

        when {
            scanning -> {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                22.dp
                            ),
                        color =
                            colors.primaryAccent,
                        strokeWidth =
                            2.dp
                    )

                    Column {
                        Text(
                            text =
                                "Scanning device",
                            color =
                                colors.primaryText,
                            fontSize = 13.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Text(
                            text =
                                "Checking your music library…",
                            color =
                                colors.secondaryText,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            result != null -> {
                Text(
                    text =
                        "Total ${result.totalSongs} songs",
                    color =
                        colors.primaryText,
                    fontSize = 14.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )

                if (
                    result.addedSongs == 0 &&
                    result.removedSongs == 0
                ) {
                    ResultPill(
                        text =
                            "No changes"
                    )
                } else {
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {
                        if (
                            result.addedSongs >
                            0
                        ) {
                            ResultPill(
                                text =
                                    "${result.addedSongs} added"
                            )
                        }

                        if (
                            result.removedSongs >
                            0
                        ) {
                            ResultPill(
                                text =
                                    "${result.removedSongs} removed"
                            )
                        }
                    }
                }
            }

            else -> {
                Text(
                    text =
                        "Total $currentTotal songs",
                    color =
                        colors.primaryText,
                    fontSize = 14.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Spacer(
                    Modifier.height(
                        5.dp
                    )
                )

                Text(
                    text =
                        "Scan your phone for changes to the music library.",
                    color =
                        colors.secondaryText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        if (!scanning) {
            Spacer(
                Modifier.height(
                    18.dp
                )
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {
                RefreshActionButton(
                    title =
                        "Cancel",
                    onClick =
                        onCancel,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                RefreshActionButton(
                    title =
                        if (
                            result == null
                        ) {
                            "Scan"
                        } else {
                            "Rescan"
                        },
                    onClick =
                        onScan,
                    accent = true,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }
        }
    }
}

@Composable
private fun ResultPill(
    text: String
) {
    val colors =
        XvoxTheme.colors

    Box(
        modifier =
            Modifier
                .background(
                    colors.card,
                    RoundedCornerShape(
                        12.dp
                    )
                )
                .padding(
                    horizontal = 11.dp,
                    vertical = 7.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text = text,
            color =
                colors.secondaryText,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun RefreshActionButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    val colors =
        XvoxTheme.colors

    Box(
        modifier =
            modifier
                .height(
                    44.dp
                )
                .background(
                    if (accent) {
                        colors.primaryAccent
                    } else {
                        colors.card
                    },
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .clickable(
                    interactionSource =
                        remember {
                            MutableInteractionSource()
                        },
                    indication = null,
                    onClick = onClick
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text = title,
            color =
                if (accent) {
                    colors.background
                } else {
                    colors.primaryText
                },
            fontSize = 13.sp,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}
