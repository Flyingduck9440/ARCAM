package com.arsoft.arcam

import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IntRange
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.arsoft.argalleryview.module.main.model.ARGalleryPicker
import com.arsoft.argalleryview.module.main.model.ARGalleryPickerRequest

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


internal sealed class CameraActions {
    object OpenGallery : CameraActions()
    object TakePicture : CameraActions()
    object SwitchCamera : CameraActions()
}

/**
 * Example of usage:
 * @param quality The quality of image default value is 95.
 * @param lensOptions Determine which camera to use front or back Camera
 * default value is [LensOptions.BOTH] using both front and back camera.
 * @param count The maximum of image you can select in the gallery
 * default value is 100.
 * @param onResult Returns list of uris.
 * @param onError Returns errors.
 * @param onBackPressed Returns when user perform back pressed or tap the close button.
 */
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
            .setResolutionSelector(
                ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(
                        Size(720, 1280), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                ).build()
            )
            .build()
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ARGalleryPicker(count),
        onResult = { uris ->
            when {
                uris.isNotEmpty() -> {
                    onResult(uris)
                }
            }
        }
    )
    val cameraPermissionRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { result ->
            if (result.values.all { it }) {
                permissionComplete = true
            } else {
                onError("Permission denied. Cannot access camera.")
            }
        }
    )

    LaunchedEffect(context) {
        when (context.checkRequirePermissions()) {
            true -> {
                permissionComplete = true
                lensFacing = when (lensOptions) {
                    LensOptions.BACK_ONLY -> {
                        CameraSelector.LENS_FACING_BACK
                    }

                    LensOptions.FRONT_ONLY -> {
                        CameraSelector.LENS_FACING_FRONT
                    }

                    LensOptions.BOTH -> {
                        CameraSelector.LENS_FACING_BACK
                    }
                }
            }

            false -> {
                cameraPermissionRequestLauncher.launch(REQUIRE_PERMISSIONS)
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
                        galleryLauncher.launch(ARGalleryPickerRequest())
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
        IconButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp),
            onClick = onBackPressed
        ) {
            Icon(
                modifier = Modifier.size(42.dp),
                painter = painterResource(R.drawable.ic_close_camera),
                contentDescription = null,
                tint = Color.White
            )
        }
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
                .clickable { onGalleryClick() },
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            painter = painterResource(R.drawable.ic_gallery),
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



