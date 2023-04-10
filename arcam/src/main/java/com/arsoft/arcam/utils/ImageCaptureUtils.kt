package com.arsoft.arcam.utils

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
private const val FILE_EXTENSION = ".jpg"

internal fun ImageCapture.capture(
    context: Context,
    lensFacing: Int,
    onImageCapture: (uri: Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val outputDirectory = context.cacheDir
    val tmpFile = createFile(outputDirectory, FILENAME, FILE_EXTENSION)
    val outputFileOptions = getOutputFileOptions(lensFacing, tmpFile)
    val executors = Executors.newSingleThreadExecutor()

    this.takePicture(
        outputFileOptions,
        executors,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.let(onImageCapture)
                executors.shutdown()
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

@Suppress("SameParameterValue")
private fun createFile(
    folder: File,
    format: String,
    extensions: String
) = File(
    folder,
    SimpleDateFormat(format, Locale.US).format(System.currentTimeMillis()) + extensions
)

private fun getOutputFileOptions(
    lensFacing: Int,
    file: File
): ImageCapture.OutputFileOptions {
    val metadata = ImageCapture.Metadata().apply {
        isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
    }

    return ImageCapture.OutputFileOptions.Builder(file)
        .setMetadata(metadata)
        .build()
}