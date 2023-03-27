package com.arsoft.arcam.model

import androidx.camera.core.CameraSelector

enum class LensOptions {
    BACK_ONLY,
    FRONT_ONLY,
    BOTH
}

internal fun LensOptions.getLensFacing() =
    when (this) {
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