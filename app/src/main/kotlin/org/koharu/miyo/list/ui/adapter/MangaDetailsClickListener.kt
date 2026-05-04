package org.koharu.miyo.list.ui.adapter

import android.view.View
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.list.ui.model.MangaListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag

interface MangaDetailsClickListener : OnListItemClickListener<MangaListModel> {

	fun onReadClick(manga: Manga, view: View)

	fun onTagClick(manga: Manga, tag: MangaTag, view: View)
}
