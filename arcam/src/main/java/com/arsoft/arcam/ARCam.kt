package com.arsoft.arcam

import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.annotation.IntRange
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arsoft.arcam.exts.checkRequirePermissions
import com.arsoft.arcam.exts.getCameraProvider
import com.arsoft.arcam.model.LensOptions
import com.arsoft.arcam.utils.capture

val REQUIRE_PERMISSIONS =
    Build.VERSION.SDK_INT.let { sdk ->
        when {
            sdk >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            }
            sdk > Build.VERSION_CODES.P -> {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
            else -> {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

sealed class CameraActions {
    object OpenGallery : CameraActions()
    object TakePicture : CameraActions()
    object SwitchCamera : CameraActions()
}

@Composable
fun ARCam(
    @IntRange(from = 0, to = 100) quality: Int = 95,
    lensOptions: LensOptions = LensOptions.BOTH,
    count: Int = Int.MAX_VALUE,
    onResult: (uri: List<Uri>) -> Unit,
    onError: (msg: String) -> Unit
) {
    val context = LocalContext.current
    var permissionComplete by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val imageCapture: ImageCapture = remember {
        ImageCapture.Builder()
            .setJpegQuality(quality)
            .setTargetResolution(Size(720, 1280))
            .build()
    }

    LaunchedEffect(context) {
        when (context.checkRequirePermissions()) {
            true -> {
                permissionComplete = true
            }
            false -> {
                onError("Need permission")
            }
        }
    }

    if (permissionComplete) {
        CameraPreview(
            imageCapture = imageCapture,
            lensOptions = lensOptions,
            lensFacing = lensFacing,
            onActions = { action ->
                when (action) {
                    CameraActions.OpenGallery -> {

                    }
                    CameraActions.SwitchCamera -> {
                        lensFacing = CameraSelector.LENS_FACING_FRONT
                            .takeIf { lensFacing == CameraSelector.LENS_FACING_BACK }
                            ?: CameraSelector.LENS_FACING_BACK
                    }
                    CameraActions.TakePicture -> {
                        imageCapture.capture(
                            context,
                            lensFacing,
                            onImageCapture = {
                                onResult(listOf(it))
                            },
                            onError = {
                                onError(it.message ?: "Unknown Error")
                            }
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun CameraPreview(
    imageCapture: ImageCapture,
    lensOptions: LensOptions,
    lensFacing: Int,
    onActions: (CameraActions) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preview = Preview.Builder().build()
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()
    val previewView = remember { PreviewView(context) }
    val cameraProvider = remember { context.getCameraProvider() }

    LaunchedEffect(lensFacing) {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        BottomMenu(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(160.dp)
                .fillMaxWidth()
                .background(Color.Black),
            switchable = lensOptions == LensOptions.BOTH,
            onGalleryClick = { onActions(CameraActions.OpenGallery) },
            onShutterClick = { onActions(CameraActions.TakePicture) },
            onSwitchClick = { onActions(CameraActions.SwitchCamera) }
        )
    }
}

@Composable
private fun BottomMenu(
    modifier: Modifier,
    switchable: Boolean = true,
    onGalleryClick: () -> Unit,
    onShutterClick: () -> Unit,
    onSwitchClick: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable { onGalleryClick() },
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            painter = painterResource(R.drawable.ic_image_placeholder),
            contentDescription = "Gallery",
            colorFilter = ColorFilter.tint(Color.White)
        )
        Image(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .border(2.dp, Color.LightGray.copy(alpha = 0.32f), CircleShape)
                .clickable { onShutterClick() },
            painter = painterResource(R.drawable.ic_camera_shutter),
            contentDescription = "Shutter",
            colorFilter = ColorFilter.tint(Color.White)
        )
        Image(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable(enabled = switchable) { onSwitchClick() },
            painter = painterResource(R.drawable.ic_camera_flip),
            contentDescription = "SwitchCam",
            colorFilter = ColorFilter.tint(Color.White)
        )
    }
}



