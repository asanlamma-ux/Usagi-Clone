package org.koharu.miyo.reader.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.decoder.SkiaPooledImageRegionDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koharu.miyo.core.util.SynchronizedSieveCache
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class EdgeDetector(private val context: Context) {

	private val mutex = Mutex()
	private val cache = SynchronizedSieveCache<ImageSource, Rect>(CACHE_SIZE)

	suspend fun getBounds(imageSource: ImageSource): Rect? {
		cache[imageSource]?.let { rect ->
			return if (rect.isEmpty) null else rect
		}
		return mutex.withLock {
			withContext(Dispatchers.IO) {
				val decoder = SkiaPooledImageRegionDecoder(Bitmap.Config.RGB_565)
				try {
					val size = runInterruptible {
						decoder.init(context, imageSource)
					}
					val scaleFactor = calculateScaleFactor(size)
					val sampleSize = (1f / scaleFactor).toInt().coerceAtLeast(1)

					val fullBitmap = decoder.decodeRegion(
						Rect(0, 0, size.x, size.y),
						sampleSize,
					)

					try {
						val edges = supervisorScope {
							listOf(
								async { runCatching { detectLeftRightEdge(fullBitmap, isLeft = true) }.getOrDefault(-1) },
								async { runCatching { detectTopBottomEdge(fullBitmap, isTop = true) }.getOrDefault(-1) },
								async { runCatching { detectLeftRightEdge(fullBitmap, isLeft = false) }.getOrDefault(-1) },
								async { runCatching { detectTopBottomEdge(fullBitmap, isTop = false) }.getOrDefault(-1) },
							).awaitAll()
						}
						var hasEdges = false
						for (edge in edges) {
							if (edge > 0) {
								hasEdges = true
							} else if (edge < 0) {
								return@withContext null
							}
						}
						if (!hasEdges) {
							Rect()
						} else {
							val left = (edges[0].takeIf { it > 0 } ?: 0) * sampleSize
							val top = (edges[1].takeIf { it > 0 } ?: 0) * sampleSize
							val right = size.x - (edges[2].takeIf { it > 0 } ?: 0) * sampleSize
							val bottom = size.y - (edges[3].takeIf { it > 0 } ?: 0) * sampleSize

							val clampedLeft = left.coerceIn(0, size.x)
							val clampedRight = right.coerceIn(0, size.x)
							val clampedTop = top.coerceIn(0, size.y)
							val clampedBottom = bottom.coerceIn(0, size.y)

							if (clampedLeft >= clampedRight || clampedTop >= clampedBottom) {
								Rect()
							} else {
								Rect(clampedLeft, clampedTop, clampedRight, clampedBottom)
							}
						}
					} finally {
						fullBitmap.recycle()
					}
				} finally {
					decoder.recycle()
				}
			}
		}
	}

	suspend fun trimEdgeCache() {
		mutex.withLock {
			cache.evictAll()
		}
	}

	private suspend fun detectLeftRightEdge(
		bitmap: Bitmap,
		isLeft: Boolean,
	): Int = withContext(Dispatchers.Default) {
		val width = bitmap.width
		val height = bitmap.height
		val edgeColor = detectEdgeColor(bitmap, width, height, isLeft)
		val requiredPixels = ceil(height * MIN_EDGE_FRACTION).toInt().coerceAtLeast(1)
		val sampleStep = (height / (requiredPixels * 2f)).toInt().coerceAtLeast(1)
		val xStart = if (isLeft) 0 else width - 1
		val xEnd = if (isLeft) width else -1
		val step = if (isLeft) 1 else -1

		var edge = -1
		var x = xStart
		while (x != xEnd) {
			var matchingPixels = 0
			var y = 0
			while (y < height) {
				val actualY = y
				val pixel = bitmap.getPixel(x, actualY)
				if (abs(pixel.red - edgeColor.red) < COLOR_THRESHOLD &&
					abs(pixel.green - edgeColor.green) < COLOR_THRESHOLD &&
					abs(pixel.blue - edgeColor.blue) < COLOR_THRESHOLD
				) {
					matchingPixels++
				}
				y += sampleStep
				if (matchingPixels >= requiredPixels) {
					break
				}
			}
			if (matchingPixels >= requiredPixels) {
				edge = if (isLeft) x else width - 1 - x
				break
			}
			x += step
		}
		edge
	}

	private suspend fun detectTopBottomEdge(
		bitmap: Bitmap,
		isTop: Boolean,
	): Int = withContext(Dispatchers.Default) {
		val width = bitmap.width
		val height = bitmap.height
		val edgeColor = detectEdgeColor(bitmap, width, height, isTop)
		val requiredPixels = ceil(width * MIN_EDGE_FRACTION).toInt().coerceAtLeast(1)
		val sampleStep = (width / (requiredPixels * 2f)).toInt().coerceAtLeast(1)
		val yStart = if (isTop) 0 else height - 1
		val yEnd = if (isTop) height else -1
		val step = if (isTop) 1 else -1

		var edge = -1
		var y = yStart
		while (y != yEnd) {
			var matchingPixels = 0
			var x = 0
			while (x < width) {
				val actualX = x
				val pixel = bitmap.getPixel(actualX, y)
				if (abs(pixel.red - edgeColor.red) < COLOR_THRESHOLD &&
					abs(pixel.green - edgeColor.green) < COLOR_THRESHOLD &&
					abs(pixel.blue - edgeColor.blue) < COLOR_THRESHOLD
				) {
					matchingPixels++
				}
				x += sampleStep
				if (matchingPixels >= requiredPixels) {
					break
				}
			}
			if (matchingPixels >= requiredPixels) {
				edge = if (isTop) y else height - 1 - y
				break
			}
			y += step
		}
		edge
	}

	@ColorInt
	private fun detectEdgeColor(bitmap: Bitmap, width: Int, height: Int, isStart: Boolean): Int {
		val sampleY = if (isStart) 0 else height - 1

		// Check if the very first/last row is uniform enough
		var rSum = 0
		var gSum = 0
		var bSum = 0
		val count = min(10, width)
		for (i in 0 until count) {
			val x = if (isStart) i * (width / count) else width - 1 - i * (width / count)
			val pixel = bitmap.getPixel(x, sampleY)
			rSum += pixel.red
			gSum += pixel.green
			bSum += pixel.blue
		}
		return Color.rgb(rSum / count, gSum / count, bSum / count)
	}

	private fun calculateScaleFactor(size: Point): Float {
		val maxDimension = max(size.x, size.y)
		return min(1f, MAX_SAMPLE_DIMENSION.toFloat() / maxDimension.toFloat())
	}

	companion object {
		fun isColorTheSame(@ColorInt color: Int, @ColorInt other: Int, tolerance: Int): Boolean {
			return abs(color.red - other.red) <= tolerance &&
				abs(color.green - other.green) <= tolerance &&
				abs(color.blue - other.blue) <= tolerance &&
				abs(color.alpha - other.alpha) <= tolerance
		}

		private const val MAX_SAMPLE_DIMENSION = 512
		private const val MIN_EDGE_FRACTION = 0.85f
		private const val COLOR_THRESHOLD = 30
		private const val CACHE_SIZE = 16
	}
}
