package com.xvox.music.features.playlist

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.model.Song
import com.xvox.music.data.preferences.XvoxPlaylist
import com.xvox.music.features.home.HomeGeometry

@Composable
fun XvoxPlaylistsSection(
    playlists: List<XvoxPlaylist>,
    songsFor: (XvoxPlaylist) -> List<Song>,
    onCreate: () -> Unit,
    onOpen: (XvoxPlaylist) -> Unit,
    onOptions: (XvoxPlaylist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors =
        XvoxTheme.colors

    Column(
        modifier =
            modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 2.dp,
                    ),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Text(
                text = "Playlists",
                color =
                    colors.primaryText,
                fontSize = 16.sp,
                fontWeight =
                    FontWeight.SemiBold,
                modifier =
                    Modifier.weight(1f),
            )

            Box(
                modifier =
                    Modifier
                        .size(38.dp)
                        .background(
                            colors.card,
                            CircleShape,
                        ).clickable(
                            interactionSource =
                                remember {
                                    MutableInteractionSource()
                                },
                            indication = null,
                            onClick = onCreate,
                        ),
                contentAlignment =
                    Alignment.Center,
            ) {
                Icon(
                    painter =
                        painterResource(
                            R.drawable
                                .ic_xvox_plus,
                        ),
                    contentDescription =
                        "Create playlist",
                    tint =
                        colors.primaryText,
                    modifier =
                        Modifier.size(
                            19.dp,
                        ),
                )
            }
        }

        // Gap below header equal to All Songs label niche gap (sectionGap)
        Spacer(modifier = Modifier.height(HomeGeometry.sectionGap))

        if (playlists.isEmpty()) {
            Text(
                text =
                    "No playlists created",
                color =
                    colors.mutedText,
                fontSize = 12.sp,
                modifier =
                    Modifier.padding(
                        horizontal =
                            12.dp,
                        vertical =
                            28.dp,
                    ),
            )
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal =
                                6.dp,
                            vertical =
                                6.dp,
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        6.dp,
                    ),
            ) {
                playlists
                    .chunked(2)
                    .forEach { rowPlaylists ->

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    6.dp,
                                ),
                        ) {
                            rowPlaylists
                                .forEach { playlist ->

                                    XvoxPlaylistCard(
                                        playlist =
                                        playlist,
                                        songs =
                                            songsFor(
                                                playlist,
                                            ),
                                        onClick = {
                                            onOpen(
                                                playlist,
                                            )
                                        },
                                        onLongClick = {
                                            onOptions(
                                                playlist,
                                            )
                                        },
                                        modifier =
                                            Modifier
                                                .weight(
                                                    1f,
                                                ),
                                    )
                                }

                            if (
                                rowPlaylists.size ==
                                1
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .weight(
                                                1f,
                                            ),
                                )
                            }
                        }
                    }
            }
        }
    }
}
