package org.koharu.miyo.local.data.index

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.koharu.miyo.core.db.entity.MangaEntity

@Entity(
	tableName = "local_index",
	foreignKeys = [
		ForeignKey(
			entity = MangaEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE,
		),
	],
)
class LocalMangaIndexEntity(
	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "manga_id") val mangaId: Long,
	@ColumnInfo(name = "path") val path: String,
)
