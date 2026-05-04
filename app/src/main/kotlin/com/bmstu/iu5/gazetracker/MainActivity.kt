package com.bmstu.iu5.gazetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bmstu.iu5.gazetracker.ui.CameraScreen
import com.bmstu.iu5.gazetracker.ui.theme.GazeTrackerTheme

/**
 * Точка входа приложения.
 *
 * Архитектурно мы держим [MainActivity] максимально пустой: вся UI-логика
 * живёт в Composable-функциях из [com.bmstu.iu5.gazetracker.ui]. Это
 * стандартный подход для современных Compose-приложений.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GazeTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black,
                ) {
                    CameraScreen()
                }
            }
        }
    }
}
