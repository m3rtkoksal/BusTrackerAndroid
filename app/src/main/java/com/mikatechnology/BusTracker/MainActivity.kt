package com.mikatechnology.BusTracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mikatechnology.BusTracker.ui.AppRoot
import com.mikatechnology.BusTracker.ui.theme.BusTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BusTrackerTheme {
                AppRoot()
            }
        }
    }
}
