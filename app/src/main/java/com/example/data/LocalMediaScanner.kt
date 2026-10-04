package com.example.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.model.Song
import org.json.JSONArray
import org.json.JSONObject

object LocalMediaScanner {

    private const val PREFS_NAME = "aurawave_imported_songs_prefs"
    private const val KEY_IMPORTED_SONGS = "imported_songs_json"

    fun scanDeviceAudio(context: Context): List<Song> {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA
        )

        // Broad selection to find all audio files on user device (>= 2 seconds)
        val selection = "${MediaStore.Audio.Media.DURATION} >= 2000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Audio Track $id"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Device Audio"
                    val duration = cursor.getLong(durCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val year = if (yearCol != -1) cursor.getInt(yearCol) else 2026

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val albumArtUri = "content://media/external/audio/albumart/$albumId"

                    songs.add(
                        Song(
                            id = "local_$id",
                            title = title,
                            artist = if (artist.contains("<unknown>", ignoreCase = true)) "Unknown Artist" else artist,
                            album = if (album.contains("<unknown>", ignoreCase = true)) "Device Audio" else album,
                            durationMs = duration,
                            dataUri = contentUri.toString(),
                            albumArtUri = albumArtUri,
                            genre = "Device Audio",
                            year = if (year > 1900) year else 2026,
                            isBundled = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Also merge with imported files selected via File Picker
        val imported = loadImportedSongs(context)
        val combined = (songs + imported).distinctBy { it.dataUri }
        return combined
    }

    fun parseSongFromUri(context: Context, uri: Uri): Song? {
        var title = ""
        var artist = "Unknown Artist"
        var album = "Imported Audio"
        var duration = 0L
        var year = 2026
        var genre = "Audio"

        // 1. Get filename from ContentResolver
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        val displayName = cursor.getString(nameIdx)
                        if (!displayName.isNullOrBlank()) {
                            title = displayName.substringBeforeLast(".")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (title.isBlank()) {
            title = uri.lastPathSegment?.substringBeforeLast(".") ?: "My Song"
        }

        // 2. Extract ID3 tags using MediaMetadataRetriever
        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, uri)

            val metaTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val metaArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val metaAlbum = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val metaDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val metaGenre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            val metaYear = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)

            if (!metaTitle.isNullOrBlank()) title = metaTitle
            if (!metaArtist.isNullOrBlank()) artist = metaArtist
            if (!metaAlbum.isNullOrBlank()) album = metaAlbum
            if (!metaDuration.isNullOrBlank()) duration = metaDuration.toLongOrNull() ?: 0L
            if (!metaGenre.isNullOrBlank()) genre = metaGenre
            if (!metaYear.isNullOrBlank()) year = metaYear.toIntOrNull() ?: 2026

            mmr.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Song(
            id = "imported_${uri.hashCode()}_${System.currentTimeMillis()}",
            title = title,
            artist = artist,
            album = album,
            durationMs = duration,
            dataUri = uri.toString(),
            genre = genre,
            year = year,
            isBundled = false
        )
    }

    fun saveImportedSongs(context: Context, newSongs: List<Song>) {
        try {
            val existing = loadImportedSongs(context).toMutableList()
            existing.addAll(newSongs)
            val unique = existing.distinctBy { it.dataUri }

            val jsonArray = JSONArray()
            for (song in unique) {
                val obj = JSONObject().apply {
                    put("id", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("album", song.album)
                    put("durationMs", song.durationMs)
                    put("dataUri", song.dataUri)
                    put("genre", song.genre)
                    put("year", song.year)
                }
                jsonArray.put(obj)
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_IMPORTED_SONGS, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadImportedSongs(context: Context): List<Song> {
        val result = mutableListOf<Song>()
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_IMPORTED_SONGS, null) ?: return emptyList()
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    Song(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        album = obj.getString("album"),
                        durationMs = obj.getLong("durationMs"),
                        dataUri = obj.getString("dataUri"),
                        genre = obj.getString("genre"),
                        year = obj.getInt("year"),
                        isBundled = false
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
