package com.example.focuslab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.focuslab.focus.FocusRoute
import com.example.focuslab.ui.theme.FocusLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusLabTheme {
                FocusRoute()
            }
        }
    }
}
