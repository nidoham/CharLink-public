package com.nidoham.charlink

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CharLinkTheme {
                SplashScreen(
                    onTimeout = {
                        checkAuthAndNavigate()
                    }
                )
            }
        }
    }

    private fun checkAuthAndNavigate() {
        // Firebase Auth ইন্সট্যান্স এবং বর্তমান ইউজার চেক
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // ইউজার যদি null না হয় (অর্থাৎ লগ-ইন করা আছে), তাহলে MainActivity, নতুবা OnboardActivity
        val nextActivity = if (currentUser != null) {
            MainActivity::class.java
        } else {
            OnboardActivity::class.java
        }

        try {
            val intent = Intent(this, nextActivity)
            startActivity(intent)

            // স্লাইড অ্যানিমেশন বন্ধ করে শুধু ফেড ইন/আউট দেওয়া হলো
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
}

@Composable
fun CharLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFE94560),
            background = Color(0xFF1A1A2E),
            surface = Color(0xFF16213E)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFE94560),
            background = Color(0xFFFAFAFA),
            surface = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun SplashScreen(onTimeout: () -> Unit = {}) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Fade In (ধীরে দৃশ্যমান হওয়া)
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )

        // কিছুক্ষণ লোগো ধরে রাখা
        delay(1500)

        // Fade Out (ধীরে মিলিয়ে যাওয়া)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 700)
        )

        // নেভিগেশন কল করা
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher), // আপনার লোগো এখানে থাকবে
            contentDescription = "CharLink App Icon",
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .graphicsLayer {
                    this.alpha = alpha.value
                }
        )
    }
}

@Preview(showBackground = true, name = "Splash Screen")
@Composable
fun PreviewSplashScreen() {
    CharLinkTheme {
        SplashScreen()
    }
}