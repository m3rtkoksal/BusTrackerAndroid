package com.mikatechnology.BusTracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mikatechnology.BusTracker.services.PushNotificationRouter
import com.mikatechnology.BusTracker.ui.AppRoot
import com.mikatechnology.BusTracker.ui.theme.BusTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deliverNotificationOpenIntent(intent)
        enableEdgeToEdge()
        setContent {
            BusTrackerTheme {
                AppRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deliverNotificationOpenIntent(intent)
    }

    private fun deliverNotificationOpenIntent(intent: Intent?) {
        PushNotificationRouter.handleIntent(intent)
    }
}
