package com.arsoft.acam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arsoft.acam.ui.theme.ACAMTheme
import com.arsoft.arcam.ARCam
import androidx.lifecycle.viewmodel.compose.viewModel

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
fun Camera(
    viewModel: MainViewModel = viewModel()
) {
    val showCamera by remember { derivedStateOf { viewModel.states.isCameraOpen } }
    if (showCamera) {
        Box(modifier = Modifier.fillMaxSize()) {
            ARCam(
                count = 2,
                onResult = {
                    viewModel.onEvent(MainEvents.CloseCamera)
                },
                onError = {
                    viewModel.onEvent(MainEvents.CloseCamera)
                },
                onBackPressed = {
                    viewModel.onEvent(MainEvents.CloseCamera)
                }
            )
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Button(
                modifier = Modifier.align(Alignment.Center),
                onClick = { viewModel.onEvent(MainEvents.OpenCamera) }
            ) {
                Text(text = "OPEN CAMERA")
            }
        }
    }
}