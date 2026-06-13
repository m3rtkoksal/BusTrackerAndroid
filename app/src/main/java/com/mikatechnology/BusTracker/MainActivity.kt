package com.mikatechnology.BusTracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.mikatechnology.BusTracker.data.smler.SmlerInviteCoordinator
import com.mikatechnology.BusTracker.data.smler.SmlerPendingInviteURL
import com.mikatechnology.BusTracker.services.PushNotificationRouter
import com.mikatechnology.BusTracker.ui.AppRoot
import com.mikatechnology.BusTracker.ui.theme.BusTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleInviteIntent(intent)
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
        handleInviteIntent(intent)
        deliverNotificationOpenIntent(intent)
    }

    private fun handleInviteIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (!SmlerPendingInviteURL.isInviteURL(uri)) return
        SmlerPendingInviteURL.capture(uri)
        lifecycleScope.launch {
            SmlerInviteCoordinator.processIncomingURL(this@MainActivity, uri)
        }
    }

    private fun deliverNotificationOpenIntent(intent: Intent?) {
        PushNotificationRouter.handleIntent(intent)
    }
}
