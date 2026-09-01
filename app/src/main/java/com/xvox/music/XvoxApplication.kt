package com.xvox.music

import android.app.Application
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.bitmapConfig

class XvoxApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val loader =
            ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(
                            this,
                            0.25
                        )
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(
                            cacheDir.resolve(
                                "xvox_artwork"
                            )
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

        SingletonImageLoader.set(
            loader
        )
    }
}
