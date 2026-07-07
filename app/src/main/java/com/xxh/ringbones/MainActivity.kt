package com.xxh.ringbones

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.xxh.ringbones.core.navigation.AppNavGraph
import com.xxh.ringbones.core.theme.MusixTheme
import dagger.hilt.android.AndroidEntryPoint

/** Aspect ratio for Picture-in-Picture window (16:9). */
private val PIP_ASPECT_RATIO = Rational(16, 9)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Whether the activity is currently in Picture-in-Picture mode. */
    var isInPiPMode by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Modern edge-to-edge with explicit SystemBarStyle — avoids deprecated
        // SYSTEM_UI_FLAG_* APIs that may not render correctly on all devices.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ),
        )

        setContent {
            val windowSize = currentWindowAdaptiveInfo().windowSizeClass

            // React to PiP mode changes for Compose recomposition
            DisposableEffect(this) {
                onDispose { }
            }

            MusixTheme {
                Musix2025App(
                    windowSize = windowSize,
                    isInPiPMode = isInPiPMode,
                    onEnterPiP = ::enterPiP,
                )
            }
        }
    }

    /**
     * Enters Picture-in-Picture mode with a 16:9 aspect ratio.
     * Wraps the system [android.app.Activity.enterPictureInPictureMode] API
     * with pre-configured PiP params.
     */
    private fun enterPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(PIP_ASPECT_RATIO)
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPiPMode = isInPictureInPictureMode

        if (isInPictureInPictureMode) {
            // Hide UI chrome in PiP — the system draws its own controls
        } else {
            // Restore full UI when returning from PiP
        }
    }

    override fun onUserLeaveHint() {
        // Enter PiP automatically when user leaves the app while content is playing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            // Only auto-enter PiP on Android 14+ when not already in PiP
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !isInPiPMode) {
                // Do nothing — user is leaving; PiP is triggered from PlayerScreen
            }
        }
        super.onUserLeaveHint()
    }
}

@Composable
fun Musix2025App(
    windowSize: WindowSizeClass,
    isInPiPMode: Boolean = false,
    onEnterPiP: () -> Unit = {},
) {
    val navController = rememberNavController()

    when (windowSize.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            AppNavGraph(
                navController = navController,
                isInPiPMode = isInPiPMode,
                onEnterPiP = onEnterPiP,
            )
        }
        WindowWidthSizeClass.EXPANDED -> {
            Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 5.dp) {
                Row {
                    SootheNavigationRail()
                    AppNavGraph(
                        navController = navController,
                        isInPiPMode = isInPiPMode,
                        onEnterPiP = onEnterPiP,
                    )
                }
            }
        }
    }
}

@Composable
private fun SootheNavigationRail(modifier: Modifier = Modifier) {
    NavigationRail(
        modifier = modifier.padding(8.dp, 0.dp, 8.dp, 0.dp),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NavigationRailItem(
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text(stringResource(R.string.bottom_navigation_home)) },
                selected = true,
                onClick = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            NavigationRailItem(
                icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                label = { Text(stringResource(R.string.bottom_navigation_profile)) },
                selected = false,
                onClick = {}
            )
        }
    }
}
