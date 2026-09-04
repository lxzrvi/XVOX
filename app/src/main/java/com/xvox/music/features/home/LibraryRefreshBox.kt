package com.xvox.music.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun LibraryRefreshBox(
    refreshing: Boolean,
    result: LibraryRefreshResult?
) {
    val colors =
        XvoxTheme.colors

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    end = 42.dp
                )
    ) {
        Text(
            text = "Refresh library",
            color =
                colors.primaryText,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(18.dp)
        )

        if (
            refreshing ||
            result == null
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        13.dp
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
                            "Checking your music",
                        color =
                            colors.primaryText,
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Text(
                        text =
                            "Scanning device library…",
                        color =
                            colors.secondaryText,
                        fontSize = 11.sp
                    )
                }
            }

            return
        }

        Text(
            text =
                "Total ${result.totalSongs} songs",
            color =
                colors.primaryText,
            fontSize = 15.sp,
            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            Modifier.height(12.dp)
        )

        if (
            result.addedSongs == 0 &&
            result.removedSongs == 0
        ) {
            ResultPill(
                text = "No changes"
            )
        } else {
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                if (
                    result.addedSongs > 0
                ) {
                    ResultPill(
                        text =
                            "${result.addedSongs} added"
                    )
                }

                if (
                    result.removedSongs > 0
                ) {
                    ResultPill(
                        text =
                            "${result.removedSongs} removed"
                    )
                }
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
                        14.dp
                    )
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text = text,
            color =
                colors.secondaryText,
            fontSize = 11.sp,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}
