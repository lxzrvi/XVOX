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

    /**
     * Prefer a 120 Hz presentation pipeline on capable Android 13+ displays. This is a request,
     * not a hardware bypass: panels limited to 60/90 Hz, battery saver, thermal policy, and the
     * user's display setting remain authoritative. Re-applying at focus/resume prevents an OEM
     * compositor from silently restoring a lower mode after a dialog or a background return.
     */
    private fun requestHighRefreshPresentation() {
        val display = window.decorView.display ?: return
        val highRefreshModes = display.supportedModes.filter { it.refreshRate >= 119f }
        val bestMode = highRefreshModes
            .minByOrNull { kotlin.math.abs(it.refreshRate - 120f) }
            ?: display.supportedModes.maxByOrNull { it.refreshRate }
            ?: return
        // A preferred Display.Mode is an advisory WindowManager request. The compositor can still
        // choose a lower rate under battery/thermal/user policy, which is correct: an app must
        // never claim to override a physical or system-imposed limit. The mode ID intentionally
        // takes precedence over preferredRefreshRate per the platform contract.
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = bestMode.modeId
            preferredRefreshRate = 0f
        }
    }

    override fun onStart() {
        super.onStart()
        requestHighRefreshPresentation()
        isAppInForeground = true
    }

    override fun onResume() {
        super.onResume()
        requestHighRefreshPresentation()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) requestHighRefreshPresentation()
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
        requestHighRefreshPresentation()
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
