package com.example.anima

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.anima.presentation.navigation.AnimaNavigation
import com.example.anima.ui.theme.AnimaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnimaTheme {
                AnimaNavigation()
            }
        }
    }
}
