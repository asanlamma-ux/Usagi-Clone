package org.koharu.miyo.core.parser

import org.koharu.miyo.core.cache.MemoryContentCache
import org.koharu.miyo.core.model.TestMangaSource
import org.koitharu.kotatsu.parsers.MangaLoaderContext

@Suppress("unused")
class TestMangaRepository(
	private val loaderContext: MangaLoaderContext,
	cache: MemoryContentCache
) : EmptyMangaRepository(TestMangaSource)
