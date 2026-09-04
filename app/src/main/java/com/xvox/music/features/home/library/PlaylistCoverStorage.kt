package com.xvox.music.features.home.library

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistCoverStorage(
    context: Context
) {
    private val appContext =
        context.applicationContext

    suspend fun persist(
        playlistId: String,
        source: Uri
    ): String? =
        withContext(
            Dispatchers.IO
        ) {
            runCatching {
                val directory =
                    File(
                        appContext.filesDir,
                        "playlist_covers"
                    )

                directory.mkdirs()

                val target =
                    File(
                        directory,
                        "playlist_$playlistId.img"
                    )

                val temporary =
                    File(
                        directory,
                        "playlist_$playlistId.tmp"
                    )

                appContext
                    .contentResolver
                    .openInputStream(
                        source
                    )
                    ?.use {
                        input ->

                        temporary
                            .outputStream()
                            .use {
                                output ->

                                input.copyTo(
                                    output
                                )
                            }
                    }
                    ?: return@runCatching null

                if (target.exists()) {
                    target.delete()
                }

                if (
                    !temporary.renameTo(
                        target
                    )
                ) {
                    temporary
                        .copyTo(
                            target,
                            overwrite = true
                        )

                    temporary.delete()
                }

                Uri.fromFile(target)
                    .toString()
            }.getOrNull()
        }

    suspend fun delete(
        playlistId: String
    ) {
        withContext(
            Dispatchers.IO
        ) {
            val directory =
                File(
                    appContext.filesDir,
                    "playlist_covers"
                )

            File(
                directory,
                "playlist_$playlistId.img"
            ).delete()

            File(
                directory,
                "playlist_$playlistId.tmp"
            ).delete()
        }
    }
}
