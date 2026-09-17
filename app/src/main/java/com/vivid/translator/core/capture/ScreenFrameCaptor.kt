package com.vivid.translator.core.capture

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection

class ScreenFrameCaptor(
    private val mediaProjection: MediaProjection,
    private val frameWidth: Int,
    private val frameHeight: Int,
    private val screenDensity: Int
) {
    private var frameReader: ImageReader? = null
    private var mirrorDisplay: VirtualDisplay? = null

    fun start() {
        val activeReader = ImageReader.newInstance(frameWidth, frameHeight, PixelFormat.RGBA_8888, 2)
        frameReader = activeReader
        mirrorDisplay = mediaProjection.createVirtualDisplay(
            "VividCapture",
            frameWidth,
            frameHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            activeReader.surface,
            null,
            null
        )
    }

    fun captureFrame(): Bitmap? {
        val reader = frameReader ?: return null
        val acquiredImage = reader.acquireLatestImage() ?: return null
        acquiredImage.use { image ->
            val imagePlane = image.planes[0]
            val planeBuffer = imagePlane.buffer
            val pixelStride = imagePlane.pixelStride
            val rowStride = imagePlane.rowStride
            val rowPadding = rowStride - pixelStride * frameWidth
            val paddedWidth = frameWidth + rowPadding / pixelStride
            val paddedBitmap = Bitmap.createBitmap(paddedWidth, frameHeight, Bitmap.Config.ARGB_8888)
            paddedBitmap.copyPixelsFromBuffer(planeBuffer)
            return Bitmap.createBitmap(paddedBitmap, 0, 0, frameWidth, frameHeight)
        }
    }

    fun release() {
        mirrorDisplay?.release()
        mirrorDisplay = null
        frameReader?.close()
        frameReader = null
        mediaProjection.stop()
    }
}
