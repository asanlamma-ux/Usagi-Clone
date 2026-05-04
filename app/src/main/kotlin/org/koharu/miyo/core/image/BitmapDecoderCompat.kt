package org.koharu.miyo.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.ImageDecoder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import com.davemorrissey.labs.subscaleview.decoder.ImageDecodeException
import okio.IOException
import okio.buffer
import okio.source
import org.aomedia.avif.android.AvifDecoder
import org.aomedia.avif.android.AvifDecoder.Info
import org.jetbrains.annotations.Blocking
import org.koharu.miyo.core.util.MimeTypes
import org.koharu.miyo.core.util.ext.MimeType
import org.koharu.miyo.core.util.ext.printStackTraceDebug
import org.koharu.miyo.core.util.ext.readByteBuffer
import org.koharu.miyo.core.util.ext.toByteBuffer
import org.koharu.miyo.core.util.ext.toMimeTypeOrNull
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer

object BitmapDecoderCompat {

	private const val FORMAT_AVIF = "avif"

	@Blocking
	fun decode(file: File): Bitmap = when (val format = probeMimeType(file)?.subtype) {
		FORMAT_AVIF -> file.source().buffer().use { decodeAvif(it.readByteBuffer()) }
		else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			ImageDecoder.decodeBitmap(ImageDecoder.createSource(file))
		} else {
			checkBitmapNotNull(BitmapFactory.decodeFile(file.absolutePath), format)
		}
	}

	@Blocking
	fun decode(stream: InputStream, type: MimeType?, isMutable: Boolean = false): Bitmap {
		val format = type?.subtype
		if (format == FORMAT_AVIF) {
			return decodeAvif(stream.toByteBuffer())
		}

		// Attempt native JPEG decode for performance (2-6x faster)
		if (format == "jpeg" || format == "jpg" || type == null) {
			val bytes = stream.readBytes()
			val nativeResult = tryNativeJpegDecode(bytes)
			if (nativeResult != null) {
				return nativeResult
			}
			// Fall back to standard decoders with the bytes we already read
			val byteBuffer = ByteBuffer.wrap(bytes)
			return if (AvifDecoder.isAvifImage(byteBuffer)) {
				decodeAvif(byteBuffer)
			} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				ImageDecoder.decodeBitmap(ImageDecoder.createSource(byteBuffer), DecoderConfigListener(isMutable))
			} else {
				val opts = BitmapFactory.Options().apply { inMutable = isMutable }
				checkBitmapNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts), format)
			}
		}

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
			val opts = BitmapFactory.Options()
			opts.inMutable = isMutable
			return checkBitmapNotNull(BitmapFactory.decodeStream(stream, null, opts), format)
		}
		val byteBuffer = stream.toByteBuffer()
		return if (AvifDecoder.isAvifImage(byteBuffer)) {
			decodeAvif(byteBuffer)
		} else {
			ImageDecoder.decodeBitmap(ImageDecoder.createSource(byteBuffer), DecoderConfigListener(isMutable))
		}
	}

	private fun tryNativeJpegDecode(data: ByteArray): Bitmap? {
		if (!NativeJpegDecoder.isAvailable) return null
		return runCatchingCancellable {
			NativeJpegDecoder.decodeJpegWithFallback(data)
		}.getOrNull()
	}

	@Blocking
	fun createRegionDecoder(inoutStream: InputStream): BitmapRegionDecoder? = try {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			BitmapRegionDecoder.newInstance(inoutStream)
		} else {
			@Suppress("DEPRECATION")
			BitmapRegionDecoder.newInstance(inoutStream, false)
		}
	} catch (e: IOException) {
		e.printStackTraceDebug()
		null
	}

	@Blocking
	fun probeMimeType(file: File): MimeType? {
		return MimeTypes.probeMimeType(file) ?: detectBitmapType(file)
	}

	@Blocking
	private fun detectBitmapType(file: File): MimeType? = runCatchingCancellable {
		val options = BitmapFactory.Options().apply {
			inJustDecodeBounds = true
		}
		BitmapFactory.decodeFile(file.path, options)?.recycle()
		options.outMimeType?.toMimeTypeOrNull()
	}.getOrNull()

	private fun checkBitmapNotNull(bitmap: Bitmap?, format: String?): Bitmap =
		bitmap ?: throw ImageDecodeException(null, format)

	private fun decodeAvif(bytes: ByteBuffer): Bitmap {
		val info = Info()
		if (!AvifDecoder.getInfo(bytes, bytes.remaining(), info)) {
			throw ImageDecodeException(
				null,
				FORMAT_AVIF,
				"Requested to decode byte buffer which cannot be handled by AvifDecoder",
			)
		}
		val config = if (info.depth == 8 || info.alphaPresent) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
		val bitmap = createBitmap(info.width, info.height, config)
		if (!AvifDecoder.decode(bytes, bytes.remaining(), bitmap)) {
			bitmap.recycle()
			throw ImageDecodeException(null, FORMAT_AVIF)
		}
		return bitmap
	}

	@RequiresApi(Build.VERSION_CODES.P)
	private class DecoderConfigListener(
		private val isMutable: Boolean,
	) : ImageDecoder.OnHeaderDecodedListener {

		override fun onHeaderDecoded(
			decoder: ImageDecoder,
			info: ImageDecoder.ImageInfo,
			source: ImageDecoder.Source
		) {
			decoder.isMutableRequired = isMutable
		}
	}
}