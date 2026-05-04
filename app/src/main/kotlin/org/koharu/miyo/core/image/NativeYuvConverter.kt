package org.koharu.miyo.core.image

import android.graphics.Bitmap
import java.nio.ByteBuffer

/**
 * NEON-accelerated YUV to RGB conversion for camera-sourced or video-sourced manga content.
 * Uses ARM NEON intrinsics on supported devices, falls back to scalar on others.
 */
object NativeYuvConverter {

    private var nativeAvailable: Boolean? = null

    val isAvailable: Boolean
        get() {
            if (nativeAvailable == null) {
                nativeAvailable = runCatching {
                    System.loadLibrary("miyo-native")
                    true
                }.getOrDefault(false)
            }
            return nativeAvailable == true
        }

    /**
     * Convert NV21 YUV planes to RGBA Bitmap.
     */
    fun nv21ToRgba(
        yPlane: ByteBuffer,
        uvPlane: ByteBuffer,
        width: Int,
        height: Int,
        stride: Int,
        outBitmap: Bitmap,
    ): Boolean {
        if (!isAvailable) return false
        return try {
            nativeNv21ToRgba(yPlane, uvPlane, width, height, stride, outBitmap)
            true
        } catch (e: Exception) {
            false
        }
    }

    @JvmStatic
    private external fun nativeNv21ToRgba(
        yBuffer: ByteBuffer,
        uvBuffer: ByteBuffer,
        width: Int,
        height: Int,
        stride: Int,
        rgbaBitmap: Bitmap,
    )
}
