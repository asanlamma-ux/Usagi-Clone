package org.koharu.miyo.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.koharu.miyo.core.util.ext.printStackTraceDebug
import java.io.ByteArrayOutputStream

/**
 * JNI bridge for native JPEG decoding via libjpeg-turbo.
 * Provides 2-6x faster decode compared to Android BitmapFactory.
 */
object NativeJpegDecoder {

    private var nativeAvailable: Boolean? = null

    val isAvailable: Boolean
        get() {
            if (nativeAvailable == null) {
                nativeAvailable = runCatching {
                    System.loadLibrary("miyo-native")
                    true
                }.getOrElse { e ->
                    e.printStackTraceDebug()
                    false
                }
            }
            return nativeAvailable == true
        }

    /**
     * Decode JPEG bytes directly into a pre-allocated Bitmap.
     * Falls back to BitmapFactory on failure.
     *
     * @param jpegData raw JPEG byte array
     * @param outBitmap pre-allocated mutable bitmap (use Bitmap.Config.RGB_565 for manga)
     * @return true if native decode succeeded
     */
    fun decodeJpeg(jpegData: ByteArray, outBitmap: Bitmap): Boolean {
        if (!isAvailable) return false
        return try {
            val width = intArrayOf(0)
            val height = intArrayOf(0)
            val result = nativeDecodeJpeg(jpegData, outBitmap, width, height)
            result == 0
        } catch (e: Exception) {
            e.printStackTraceDebug()
            false
        }
    }

    /**
     * Quickly probe JPEG dimensions without full decode.
     * @return Pair(width, height) or null if failed
     */
    fun probeDimensions(jpegData: ByteArray): Pair<Int, Int>? {
        if (!isAvailable) return null
        return try {
            val width = intArrayOf(0)
            val height = intArrayOf(0)
            val result = nativeProbeJpeg(jpegData, width, height)
            if (result == 0) Pair(width[0], height[0]) else null
        } catch (e: Exception) {
            e.printStackTraceDebug()
            null
        }
    }

    /**
     * Decode with fallback to Android BitmapFactory.
     * @return decoded Bitmap, never null
     */
    fun decodeJpegWithFallback(jpegData: ByteArray, sampleSize: Int = 1): Bitmap {
        if (isAvailable) {
            // First probe dimensions, then create bitmap and decode
            val dims = probeDimensions(jpegData)
            if (dims != null) {
                val w = dims.first / sampleSize
                val h = dims.second / sampleSize
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
                if (decodeJpeg(jpegData, bitmap)) {
                    return bitmap
                }
                bitmap.recycle()
            }
        }
        // Fallback to Android decoder
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
            inSampleSize = sampleSize
        }
        return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size, options)
            ?: throw RuntimeException("Failed to decode JPEG")
    }

    // JNI native methods
    @JvmStatic
    private external fun nativeDecodeJpeg(
        data: ByteArray,
        bitmap: Bitmap,
        outWidth: IntArray,
        outHeight: IntArray,
    ): Int

    @JvmStatic
    private external fun nativeProbeJpeg(
        data: ByteArray,
        outWidth: IntArray,
        outHeight: IntArray,
    ): Int
}