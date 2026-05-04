package org.koharu.miyo.local.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koharu.miyo.core.util.ext.printStackTraceDebug
import java.io.File
import java.io.FileOutputStream

/**
 * Re-encodes manga page images to save storage space.
 * JPEG → WebP lossy (quality 80) typically saves 30-40% vs original JPEG.
 * Uses Android's built-in WebP encoder (no native code needed).
 */
object ImageReEncoder {

    private const val WEBP_QUALITY = 80

    /**
     * Re-encode a file to WebP. Returns the new file if re-encoding saved significant space,
     * or null if the original is already efficient enough.
     */
    suspend fun reEncodeToWebP(source: File, destDir: File): File? = withContext(Dispatchers.IO) {
        if (source.extension.equals("webp", ignoreCase = true)) return@withContext null

        try {
            val originalSize = source.length()
            if (originalSize < 8 * 1024) return@withContext null // skip small files

            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeFile(source.absolutePath, options)
                ?: return@withContext null

            val outFile = File(destDir, source.nameWithoutExtension + ".webp")
            val success = try {
                FileOutputStream(outFile).use { output ->
                    bitmap.compress(webpCompressFormat(), WEBP_QUALITY, output)
                }
            } finally {
                bitmap.recycle()
            }

            if (!success) {
                outFile.delete()
                return@withContext null
            }

            // Only keep WebP version if it saves >20% space
            if (outFile.length() < originalSize * 0.8) {
                outFile
            } else {
                outFile.delete()
                null
            }
        } catch (e: Exception) {
            e.printStackTraceDebug()
            null
        }
    }

    private fun webpCompressFormat(): Bitmap.CompressFormat {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            webpLossyCompressFormat()
        } else {
            legacyWebpCompressFormat()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun webpLossyCompressFormat(): Bitmap.CompressFormat = Bitmap.CompressFormat.WEBP_LOSSY

    @Suppress("DEPRECATION")
    private fun legacyWebpCompressFormat(): Bitmap.CompressFormat = Bitmap.CompressFormat.WEBP
}
