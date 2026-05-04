package org.koharu.miyo.reader.ui

import org.koharu.miyo.reader.ui.pager.ReaderPage

data class ReaderContent(
	val pages: List<ReaderPage>,
	val state: ReaderState?
)