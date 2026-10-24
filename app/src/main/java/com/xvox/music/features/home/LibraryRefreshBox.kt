package com.xvox.music.features.home

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.overlay.xvoxBoxScroll

@Composable
fun LibraryRefreshBox(
    currentTotal: Int,
    scanning: Boolean,
    result: LibraryRefreshResult?,
    onScan: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = XvoxTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .xvoxBoxScroll(scrollState)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Refresh Library",
            color = colors.primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(16.dp))

        when {
            scanning -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = colors.primaryAccent,
                        strokeWidth = 3.dp
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Scanning Android MediaStore...",
                        color = colors.primaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Verifying completed, readable audio files and duplicate rows",
                        color = colors.secondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            result != null -> {
                val successful = !result.scanFailed
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(colors.primaryAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                if (successful) R.drawable.ic_xvox_check else R.drawable.ic_xvox_info
                            ),
                            contentDescription = if (successful) "Verified scan complete" else "Scan failed",
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = if (successful) "Verified Scan Complete" else "Scan Could Not Finish",
                        color = colors.primaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = if (successful) {
                            "${result.totalSongs} verified playable songs on this device"
                        } else {
                            "The previous ${result.totalSongs} verified songs were kept unchanged"
                        },
                        color = colors.secondaryText,
                        fontSize = 13.sp
                    )

                    if (successful && (result.addedSongs > 0 || result.removedSongs > 0)) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (result.addedSongs > 0) {
                                ResultPill(text = "+${result.addedSongs} new")
                            }
                            if (result.removedSongs > 0) {
                                ResultPill(text = "-${result.removedSongs} removed")
                            }
                        }
                    }

                    if (successful && (result.unavailableRowsSkipped > 0 || result.duplicateRowsSkipped > 0)) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = buildString {
                                if (result.unavailableRowsSkipped > 0) {
                                    append("Ignored ${result.unavailableRowsSkipped} incomplete or unreadable row")
                                    if (result.unavailableRowsSkipped != 1) append('s')
                                }
                                if (result.unavailableRowsSkipped > 0 && result.duplicateRowsSkipped > 0) {
                                    append(" • ")
                                }
                                if (result.duplicateRowsSkipped > 0) {
                                    append("Ignored ${result.duplicateRowsSkipped} duplicate row")
                                    if (result.duplicateRowsSkipped != 1) append('s')
                                }
                            },
                            color = colors.secondaryText,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    RefreshActionButton(
                        title = "Okay",
                        onClick = onCancel,
                        accent = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "$currentTotal verified playable songs currently indexed.",
                        color = colors.primaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Scan Android MediaStore now to verify new downloads, deletions, unreadable files, and duplicate rows.",
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RefreshActionButton(
                            title = "Cancel",
                            onClick = onCancel,
                            modifier = Modifier.weight(1f)
                        )

                        RefreshActionButton(
                            title = "Scan Now",
                            onClick = onScan,
                            accent = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultPill(text: String) {
    val colors = XvoxTheme.colors

    Box(
        modifier = Modifier
            .background(colors.card, RoundedCornerShape(12.dp))
            .padding(horizontal = 11.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colors.secondaryText,
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
    val colors = XvoxTheme.colors

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (accent) colors.primaryAccent else colors.card)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (accent) colors.background else colors.primaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
