package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val songIds: String, // Comma separated song IDs
    val createdAt: Long = System.currentTimeMillis(),
    val iconName: String = "playlist"
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_history")
data class RecentHistoryEntity(
    @PrimaryKey
    val songId: String,
    val playedAt: Long = System.currentTimeMillis()
)
