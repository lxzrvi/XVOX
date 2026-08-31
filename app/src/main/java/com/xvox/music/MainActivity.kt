package com.xvox.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.features.setup.SetupScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        setContent {
            XvoxTheme {
                SetupScreen(
                    onSetupComplete = {
                        // Navigation to Home comes next.
                    }
                )
            }
        }
    }
}
