package com.vibe.ui.compose.components

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

object VibeToast {
    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }

    fun showLong(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

@Composable
fun ShowToast(message: String?, duration: Int = Toast.LENGTH_SHORT) {
    val context = LocalContext.current
    LaunchedEffect(message) {
        message?.let {
            VibeToast.show(context, it, duration)
        }
    }
}
