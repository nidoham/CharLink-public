package com.nidoham.charlink

import android.app.Application
import android.content.Intent
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

class ApplicationManager : Application() {

    override fun onCreate() {
        super.onCreate()

        // Set the global exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(thread, throwable)
        }
    }

    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        // 1. Log the error locally (optional but good for Logcat)
        Log.e("AppCrash", "Critical error: ${throwable.message}", throwable)

        // 2. Prepare the Intent for the DebugActivity
        val intent = Intent(this, DebugActivity::class.java).apply {
            // specific flags are needed to start a new activity from a crashed context
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // Pass the stack trace
            putExtra(DebugActivity.EXTRA_ERROR_DETAILS, throwable.stackTraceToString())
        }

        // 3. Start the Debug Activity
        startActivity(intent)

        // 4. Kill the current process to ensure the OS doesn't show the standard "App has stopped" dialog
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }
}