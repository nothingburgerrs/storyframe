package com.fakegram.profile.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** A fake Google "knowledge panel" (the info card shown when you search a person or group). */
@Entity(tableName = "panels")
data class PanelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Text shown in the search bar. */
    val query: String = "",
    val title: String = "New panel",
    val subtitle: String = "",
    val overview: String = "",
    /** Link label appended to the overview, e.g. "Wikipedia". */
    val sourceLabel: String = "Wikipedia",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** The character / group this search is about (null = not attached to anyone). */
    val characterId: Long? = null
)

@Entity(
    tableName = "panel_images",
    foreignKeys = [ForeignKey(
        entity = PanelEntity::class,
        parentColumns = ["id"],
        childColumns = ["panelId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PanelImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val panelId: Long,
    val uri: String,
    val position: Int = 0
)

/** A small info card such as "Age / 22 years / August 31, 2004". */
@Entity(
    tableName = "panel_facts",
    foreignKeys = [ForeignKey(
        entity = PanelEntity::class,
        parentColumns = ["id"],
        childColumns = ["panelId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PanelFactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val panelId: Long,
    val label: String = "",
    val value: String = "",
    val detail: String = "",
    val position: Int = 0
)

/** Row inside an expandable section: a song (kind SONGS) or a group member (kind MEMBERS). */
@Entity(
    tableName = "panel_items",
    foreignKeys = [ForeignKey(
        entity = PanelEntity::class,
        parentColumns = ["id"],
        childColumns = ["panelId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PanelItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val panelId: Long,
    val kind: String = PanelItemKind.SONGS,
    val title: String = "",
    val subtitle: String = "",
    val imageUri: String? = null,
    val position: Int = 0
)

object PanelItemKind {
    const val SONGS = "SONGS"
    const val MEMBERS = "MEMBERS"
}

data class PanelUiState(
    val panel: PanelEntity? = null,
    val images: List<PanelImageEntity> = emptyList(),
    val facts: List<PanelFactEntity> = emptyList(),
    val items: List<PanelItemEntity> = emptyList()
) {
    fun items(kind: String): List<PanelItemEntity> = items.filter { it.kind == kind }.sortedBy { it.position }
}
