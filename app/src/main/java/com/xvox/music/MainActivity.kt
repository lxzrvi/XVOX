package com.xvox.music

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {

    override fun onStart() {
        super.onStart()
        isAppInForeground = true
    }

    override fun onStop() {
        super.onStop()
        isAppInForeground = false
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) com.xvox.music.features.home.allsongs.XvoxMosaicSession.begin()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val orientationPrefs = UserPreferencesRepository(applicationContext)
        setContent {
            // Portrait and Landscape remain explicit locks. Auto deliberately returns ownership
            // to Android so the current system auto-rotate policy and sensor decide the layout.
            LaunchedEffect(orientationPrefs) {
                orientationPrefs.appOrientation.collect { choice ->
                    val requested = when (choice.lowercase()) {
                        "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                    if (requestedOrientation != requested) requestedOrientation = requested
                }
            }
            XvoxTheme {
                XvoxAppRoot()
            }
        }
    }

    companion object {
        @Volatile
        var isAppInForeground: Boolean = false
    }
}
