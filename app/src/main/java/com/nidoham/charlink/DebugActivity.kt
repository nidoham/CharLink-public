package com.nidoham.charlink

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DebugActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ERROR_DETAILS = "extra_error_details"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorDetails = intent.getStringExtra(EXTRA_ERROR_DETAILS) ?: "Unknown Error"

        setContent {
            DebugScreen(errorDetails = errorDetails)
        }
    }
}

@Composable
fun DebugScreen(errorDetails: String) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF0F0)) // Light red background for warning
            .padding(16.dp)
    ) {
        Text(
            text = "Application Crushed",
            fontSize = 24.sp,
            color = Color.Red,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "An unexpected error occurred. Please report this to the developer.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error Stack Trace Box
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Text(
                text = errorDetails,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Button: Copy to Clipboard
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Crash Log", errorDetails)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Copy")
            }

            // Button: Share / Report
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "App Crash Report:\n\n$errorDetails")
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Report Crash via")
                    context.startActivity(shareIntent)
                }
            ) {
                Text("Report")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Button: Restart App
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val packageManager = context.packageManager
                val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                val componentName = intent?.component
                val mainIntent = Intent.makeRestartActivityTask(componentName)
                context.startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }
        ) {
            Text("Restart App")
        }
    }
}