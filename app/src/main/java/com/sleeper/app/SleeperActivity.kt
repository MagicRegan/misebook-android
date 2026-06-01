package com.sleeper.app

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sleeper.app.ui.SleeperScreen
import com.sleeper.app.viewmodel.SleeperViewModel
import kotlinx.coroutines.launch

/**
 * Main (and only) Activity for Sleeper.
 *
 * Responsibilities:
 *  - Immersive fullscreen with true black background
 *  - Keep screen awake
 *  - Intercept hardware volume buttons and forward to the ViewModel
 *  - Trigger haptic feedback based on state-machine events
 */
class SleeperActivity : ComponentActivity() {

    private val vm: SleeperViewModel by viewModels()
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set black background immediately to prevent any flash
        window.setBackgroundDrawableResource(android.R.color.black)

        setupImmersiveMode()
        setupKeepScreenOn()
        initVibrator()

        setContent {
            val uiState by vm.uiState.collectAsState()

            SleeperScreen(
                uiState = uiState,
                onTransformUpdate = { pan, zoom, rotation ->
                    vm.updateCardTransform(pan, zoom, rotation)
                },
                onDismiss = { vm.onDismiss() },
                onEmergencyReset = { vm.onEmergencyReset() }
            )
        }

        // Observe haptic events
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.hapticEvent.collect { event ->
                    if (event != SleeperViewModel.HapticEvent.NONE) {
                        performHaptic(event)
                        vm.consumeHapticEvent()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupImmersiveMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupImmersiveMode()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                vm.onVolumeUp()
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                vm.onVolumeDown()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> true
            else -> super.onKeyUp(keyCode, event)
        }
    }

    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(
            WindowInsetsCompat.Type.statusBars() or
            WindowInsetsCompat.Type.navigationBars() or
            WindowInsetsCompat.Type.systemBars()
        )
        @Suppress("DEPRECATION")
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
    }

    private fun setupKeepScreenOn() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            mgr.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun performHaptic(event: SleeperViewModel.HapticEvent) {
        val v = vibrator ?: return
        val effect = when (event) {
            SleeperViewModel.HapticEvent.SUIT_MODE ->
                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            SleeperViewModel.HapticEvent.CONFIRM ->
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            SleeperViewModel.HapticEvent.RESET ->
                VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
            SleeperViewModel.HapticEvent.NONE -> return
        }
        v.vibrate(effect)
    }
}
