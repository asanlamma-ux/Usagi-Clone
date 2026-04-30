package org.draken.usagi.download.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class AdaptiveTokenBucket(
	private val maxTokensPerSecond: Double = 8.0,
) {
	private val tokens = ConcurrentHashMap<String, BucketState>()
	private val mutex = Mutex()

	suspend fun acquire(key: String): Long {
		val state = tokens.getOrPut(key) { BucketState(maxTokensPerSecond) }
		return mutex.withLock {
			val now = nowMillis()
			state.refill(now)
			if (state.currentTokens >= 1.0) {
				state.currentTokens -= 1.0
				0L
			} else {
				val waitMs = ((1.0 - state.currentTokens) / state.refillRate * 1000.0).toLong() + 1L
				state.currentTokens = 0.0
				waitMs
			}
		}
	}

	suspend fun recordSlowResponse(key: String) {
		mutex.withLock {
			val state = tokens.getOrPut(key) { BucketState(maxTokensPerSecond) }
			state.recordSlowResponse()
		}
	}

	suspend fun recordRateLimit(key: String) {
		mutex.withLock {
			val state = tokens.getOrPut(key) { BucketState(maxTokensPerSecond) }
			state.record429()
		}
	}

	fun reset(key: String) {
		tokens.remove(key)
	}

	private fun nowMillis() = System.nanoTime() / 1_000_000L

	private class BucketState(
		initialRate: Double,
	) {
		var currentTokens = initialRate
		var refillRate = initialRate
		private var lastRefillMs = nowMillis()
		private var consecutiveFailures = 0

		fun refill(nowMs: Long) {
			val elapsed = (nowMs - lastRefillMs).coerceAtLeast(0L)
			val added = elapsed / 1000.0 * refillRate
			currentTokens = (currentTokens + added).coerceAtMost(refillRate * BURST_FACTOR)
			lastRefillMs = nowMs
		}

		fun recordSlowResponse() {
			consecutiveFailures++
			if (consecutiveFailures >= SLOW_THRESHOLD) {
				refillRate = (refillRate * 0.7).coerceAtLeast(MIN_RATE)
			}
		}

		fun record429() {
			refillRate = (refillRate * 0.5).coerceAtLeast(MIN_RATE)
			consecutiveFailures = SLOW_THRESHOLD
		}

		private fun nowMillis() = System.nanoTime() / 1_000_000L

		companion object {
			private const val BURST_FACTOR = 2.0
			private const val MIN_RATE = 0.5
			private const val SLOW_THRESHOLD = 3
		}
	}
}