package com.viralclip.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.viralclip.app.ui.navigation.ViralClipNavHost
import com.viralclip.app.ui.theme.ViralClipTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ViralClipTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ViralClipNavHost(
                        initialVideoUri = handleIncomingIntent(intent)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?): String? {
        return when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("video/") == true) {
                    intent.getParcelableExtra<Intent>(Intent.EXTRA_STREAM)?.data?.toString()
                        ?: intent.data?.toString()
                } else null
            }
            else -> intent?.data?.toString()
        }
    }
}
