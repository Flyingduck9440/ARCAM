package com.arsoft.acam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arsoft.acam.ui.theme.ACAMTheme
import com.arsoft.arcam.ARCam

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ACAMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    Camera()
                }
            }
        }
    }
}

@Composable
fun Camera() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        ARCam(onResult = {}, onError = {})
    }
}