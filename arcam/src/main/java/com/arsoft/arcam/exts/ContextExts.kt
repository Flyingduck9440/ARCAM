package com.arsoft.arcam.exts

import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.arsoft.arcam.REQUIRE_PERMISSIONS

internal fun Context.checkRequirePermissions() =
    REQUIRE_PERMISSIONS.all { permission ->
        ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

internal fun Context.getCameraProvider() =
    ProcessCameraProvider.getInstance(this).also { cameraProvider ->
        cameraProvider.addListener(
            { cameraProvider.get() },
            ContextCompat.getMainExecutor(this)
        )
    }.get()