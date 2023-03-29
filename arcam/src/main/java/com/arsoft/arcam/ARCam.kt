package com.arsoft.arcam

import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Size
import android.view.Gravity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        }
        Build.VERSION.SDK_INT > Build.VERSION_CODES.P -> {
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


sealed class CameraActions {
    object OpenGallery : CameraActions()
    object TakePicture : CameraActions()
    object SwitchCamera : CameraActions()
}

@Composable
fun ARCam(
    @IntRange(from = 0, to = 100) quality: Int = 95,
    lensOptions: LensOptions = LensOptions.BOTH,
    @IntRange(from = 1, to = 100) count: Int = 100,
    onResult: (uri: List<Uri>) -> Unit,
    onError: (msg: String) -> Unit,
    onBackPressed: () -> Unit
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
    var openGallery by remember { mutableStateOf(false) }
    val pickMediaContract =
        ActivityResultContracts.PickVisualMedia()
            .takeIf { count == 1 }
            ?: ActivityResultContracts.PickMultipleVisualMedia(count)

    val pickMedia = rememberLauncherForActivityResult(
        contract = pickMediaContract,
        onResult = { result ->
            when (result) {
                is Uri -> {

                }
                is ArrayList<*> -> {
                    result.filterIsInstance(Uri::class.java).let {
                        if (it.size > count) {
                            Toast.makeText(
                                context,
                                "Select up to $count items.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    )

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
        if (openGallery) {
            ARGallery()
        } else {
            CameraPreview(
                imageCapture = imageCapture,
                lensOptions = lensOptions,
                lensFacing = lensFacing,
                onActions = { action ->
                    when (action) {
                        CameraActions.OpenGallery -> {
//                        pickMedia.launch(
//                            PickVisualMediaRequest(
//                                ActivityResultContracts.PickVisualMedia.ImageOnly
//                            )
//                        )
                            openGallery = true
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
                },
                onBackPressed = onBackPressed
            )
        }
    }
}

@Composable
private fun CameraPreview(
    imageCapture: ImageCapture,
    lensOptions: LensOptions,
    lensFacing: Int,
    onActions: (CameraActions) -> Unit,
    onBackPressed: () -> Unit
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

    BackHandler {
        cameraProvider.unbindAll()
        previewView.removeAllViews()
        onBackPressed()
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
        horizontalArrangement = Arrangement.spacedBy(52.dp, Alignment.CenterHorizontally),
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun Preview() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
        )
        BottomMenu(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(160.dp)
                .fillMaxWidth()
                .background(Color.Black),
            switchable = true,
            onGalleryClick = { },
            onShutterClick = { },
            onSwitchClick = { }
        )
    }
}



