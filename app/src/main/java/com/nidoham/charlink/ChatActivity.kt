package com.nidoham.charlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import com.nidoham.charlink.screens.ChatScreen // Make sure to import ChatScreen

class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Enable Edge-to-Edge to draw behind system bars
        enableEdgeToEdge()

        setContent {
            // 2. Define a basic Theme (or use your app's custom Theme wrapper)
            val colorScheme = if (isSystemInDarkTheme()) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                // 3. Render the Chat Screen
                ChatScreen()
            }
        }
    }
}