package com.example.healthmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.healthmate.ui.theme.HealthMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HealthMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ){
                    HealthMateApp()
                }
            }
        }
    }
}

/**
 * Composable that displays what the UI of the app looks like in light theme in the design tab.
 */
@Preview
@Composable
fun HealthMatePreview() {
    HealthMateTheme(darkTheme = false) {
        HealthMateApp()
    }
}

/**
 * Composable that displays what the UI of the app looks like in dark theme in the design tab.
 */
@Preview
@Composable
fun HealthMateDarkThemePreview() {
    HealthMateTheme(darkTheme = true) {
        HealthMateApp()
    }
}