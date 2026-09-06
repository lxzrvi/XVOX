package com.xvox.music.features.home

import android.app.Activity
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import com.xvox.music.core.model.Song

object XvoxSongActions {

    fun share(context: Context, song: Song) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, song.contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share song").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun shareMultiple(context: Context, songs: List<Song>) {
        if (songs.isEmpty()) return
        if (songs.size == 1) {
            share(context, songs.first())
            return
        }
        val uris = ArrayList<Uri>(songs.map { it.contentUri })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share ${songs.size} songs").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun canWriteSettings(context: Context): Boolean = Settings.System.canWrite(context)

    fun openWriteSettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun setRingtone(context: Context, song: Song): Boolean {
        if (!canWriteSettings(context)) return false
        return runCatching {
            val values = ContentValues().apply { put(MediaStore.Audio.Media.IS_RINGTONE, true) }
            runCatching { context.contentResolver.update(song.contentUri, values, null, null) }
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, song.contentUri)
            true
        }.getOrDefault(false)
    }

    fun deleteRequest(activity: Activity, song: Song): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(activity.contentResolver, listOf(song.contentUri))
                .intentSender.let { sender ->
                    Intent().apply { putExtra("xvox_delete_sender", sender) }
                }
        } else {
            Intent()
        }
    }

    fun deleteMultipleRequest(activity: Activity, songs: List<Song>): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(activity.contentResolver, songs.map { it.contentUri })
                .intentSender.let { sender ->
                    Intent().apply { putExtra("xvox_delete_sender", sender) }
                }
        } else {
            Intent()
        }
    }

    fun deletePendingIntent(context: Context, song: Song): PendingIntent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(context.contentResolver, listOf(song.contentUri))
        } else {
            null
        }

    fun deleteLegacy(context: Context, song: Song): Boolean =
        runCatching { context.contentResolver.delete(song.contentUri, null, null) > 0 }.getOrDefault(false)
}
