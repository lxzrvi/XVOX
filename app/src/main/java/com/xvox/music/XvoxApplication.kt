package com.xvox.music

import android.app.Application
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.bitmapConfig
import okio.Path.Companion.toOkioPath

class XvoxApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(
                            context,
                            0.25
                        )
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(
                            context.cacheDir
                                .resolve(
                                    "xvox_artwork"
                                )
                                .toOkioPath()
                        )
                        .maxSizeBytes(
                            100L *
                                1024L *
                                1024L
                        )
                        .build()
                }
                .bitmapConfig(
                    Bitmap.Config.RGB_565
                )
                .build()
        }
    }
}
