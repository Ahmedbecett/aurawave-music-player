package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiMusicRecognizer
import com.example.data.BuiltInMusicCatalog
import com.example.data.FavoriteEntity
import com.example.data.LocalMediaScanner
import com.example.data.NaghamDatabase
import com.example.data.PlaylistEntity
import com.example.data.RecentHistoryEntity
import com.example.localization.AppLanguage
import com.example.model.EqualizerBand
import com.example.model.EqualizerState
import com.example.model.LyricLine
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.playback.NaghamAudioEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LibraryTab {
    SONGS,
    FAVORITES,
    PLAYLISTS,
    ARTISTS,
    ALBUMS,
    GENRES
}

data class MusicUiState(
    val language: AppLanguage = AppLanguage.ENGLISH, // Default system language is English
    val allSongs: List<Song> = emptyList(),
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val progressMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val isShuffle: Boolean = false,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = 0,
    val equalizerState: EqualizerState = EqualizerState(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val recentSongIds: List<String> = emptyList(),
    val searchQuery: String = "",
    val activeTab: LibraryTab = LibraryTab.SONGS,
    val selectedPlaylist: PlaylistEntity? = null,
    val showLyrics: Boolean = false,
    val isPlayerExpanded: Boolean = false,
    val sleepTimerMinutesLeft: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val parsedLyrics: List<LyricLine> = emptyList(),
    val activeLyricIndex: Int = -1,
    val hasStoragePermission: Boolean = false
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NaghamDatabase.getDatabase(application)
    private val playlistDao = db.playlistDao()
    private val favoriteDao = db.favoriteDao()
    private val recentDao = db.recentDao()

    val audioEngine = NaghamAudioEngine(application)
    val aiRecognizer = AiMusicRecognizer(application)

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    val visualizerBars: StateFlow<List<Float>> = audioEngine.visualizerBars

    private var sleepTimerJob: Job? = null

    init {
        // Setup audio engine listeners
        audioEngine.onPlaybackStateChanged = { isPlaying ->
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }

        audioEngine.onProgressUpdate = { pos, dur ->
            val currentState = _uiState.value
            val lyrics = currentState.parsedLyrics
            val activeIdx = lyrics.indexOfLast { pos >= it.timestampMs }

            _uiState.value = currentState.copy(
                progressMs = pos,
                durationMs = if (dur > 0) dur else currentState.durationMs,
                activeLyricIndex = activeIdx
            )
        }

        audioEngine.onTrackCompleted = {
            handleTrackCompleted()
        }

        // Load built-in tracks and scan device files
        loadMusicCatalog()

        // Observe database
        viewModelScope.launch {
            playlistDao.getAllPlaylists().collect { playlists ->
                _uiState.value = _uiState.value.copy(playlists = playlists)
            }
        }

        viewModelScope.launch {
            favoriteDao.getAllFavoriteIds().collect { favList ->
                val favSet = favList.toSet()
                _uiState.value = _uiState.value.copy(
                    favoriteIds = favSet,
                    allSongs = _uiState.value.allSongs.map { it.copy(isFavorite = favSet.contains(it.id)) }
                )
            }
        }

        viewModelScope.launch {
            recentDao.getRecentSongIds().collect { recentList ->
                _uiState.value = _uiState.value.copy(recentSongIds = recentList)
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.value = _uiState.value.copy(language = language)
    }

    fun setStoragePermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasStoragePermission = granted)
        if (granted) {
            loadMusicCatalog()
        }
    }

    fun loadMusicCatalog() {
        viewModelScope.launch {
            val builtIn = BuiltInMusicCatalog.defaultSongs
            val scanned = LocalMediaScanner.scanDeviceAudio(getApplication())
            val combined = (builtIn + scanned).distinctBy { it.id }

            val favs = _uiState.value.favoriteIds
            val updated = combined.map { it.copy(isFavorite = favs.contains(it.id)) }

            val initialSong = updated.firstOrNull()
            _uiState.value = _uiState.value.copy(
                allSongs = updated,
                currentSong = _uiState.value.currentSong ?: initialSong,
                durationMs = _uiState.value.currentSong?.durationMs ?: (initialSong?.durationMs ?: 0L),
                queue = if (_uiState.value.queue.isEmpty()) updated else _uiState.value.queue,
                parsedLyrics = BuiltInMusicCatalog.parseLyrics(_uiState.value.currentSong?.lyrics ?: initialSong?.lyrics),
                hasStoragePermission = scanned.isNotEmpty() || _uiState.value.hasStoragePermission
            )
        }
    }

    fun playSong(song: Song, newQueue: List<Song>? = null) {
        val queue = newQueue ?: _uiState.value.queue.ifEmpty { _uiState.value.allSongs }
        val index = queue.indexOfFirst { it.id == song.id }.let { if (it >= 0) it else 0 }

        val parsed = BuiltInMusicCatalog.parseLyrics(song.lyrics)

        _uiState.value = _uiState.value.copy(
            currentSong = song,
            queue = queue,
            queueIndex = index,
            progressMs = 0L,
            durationMs = song.durationMs,
            parsedLyrics = parsed,
            activeLyricIndex = -1
        )

        audioEngine.playSong(song)

        // Record in recent history
        viewModelScope.launch {
            recentDao.addRecent(RecentHistoryEntity(songId = song.id, playedAt = System.currentTimeMillis()))
        }
    }

    fun playMatchedAiSong(title: String, artist: String) {
        val all = _uiState.value.allSongs
        val matched = all.find { song ->
            song.title.contains(title, ignoreCase = true) ||
            song.artist.contains(artist, ignoreCase = true) ||
            title.contains(song.title, ignoreCase = true)
        } ?: all.firstOrNull()

        if (matched != null) {
            playSong(matched)
            setPlayerExpanded(true)
        }
    }

    fun togglePlayPause() {
        val state = _uiState.value
        if (state.currentSong == null) {
            state.allSongs.firstOrNull()?.let { playSong(it) }
            return
        }

        if (state.isPlaying) {
            audioEngine.pause()
        } else {
            audioEngine.resume()
        }
    }

    fun playNext() {
        val state = _uiState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        val nextIndex = if (state.isShuffle) {
            (queue.indices - state.queueIndex).randomOrNull() ?: 0
        } else {
            (state.queueIndex + 1) % queue.size
        }

        val nextSong = queue[nextIndex]
        playSong(nextSong, queue)
    }

    fun playPrevious() {
        val state = _uiState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        if (state.progressMs > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (state.queueIndex - 1 < 0) queue.size - 1 else state.queueIndex - 1
        val prevSong = queue[prevIndex]
        playSong(prevSong, queue)
    }

    private fun handleTrackCompleted() {
        val state = _uiState.value
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                state.currentSong?.let { audioEngine.playSong(it, 0L) }
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                if (state.queueIndex < state.queue.size - 1) {
                    playNext()
                } else {
                    audioEngine.pause()
                    seekTo(0L)
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(progressMs = positionMs)
    }

    fun toggleShuffle() {
        val newShuffle = !_uiState.value.isShuffle
        _uiState.value = _uiState.value.copy(isShuffle = newShuffle)
    }

    fun cycleRepeatMode() {
        val newMode = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _uiState.value = _uiState.value.copy(repeatMode = newMode)
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val isFav = _uiState.value.favoriteIds.contains(song.id)
            if (isFav) {
                favoriteDao.removeFavorite(song.id)
            } else {
                favoriteDao.addFavorite(FavoriteEntity(song.id))
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        audioEngine.setSpeed(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun setVolume(volume: Float) {
        audioEngine.setVolume(volume)
        _uiState.value = _uiState.value.copy(volume = volume)
    }

    fun updateEqualizer(updater: (EqualizerState) -> EqualizerState) {
        val newState = updater(_uiState.value.equalizerState)
        audioEngine.applyEqualizer(newState)
        _uiState.value = _uiState.value.copy(equalizerState = newState)
    }

    fun setEqualizerPreset(presetName: String) {
        val gains = EqualizerState.PRESETS[presetName] ?: return
        val currentEq = _uiState.value.equalizerState
        val newBands = currentEq.bands.mapIndexed { idx, band ->
            band.copy(gainDb = gains.getOrElse(idx) { 0 })
        }
        val newState = currentEq.copy(presetName = presetName, bands = newBands)
        audioEngine.applyEqualizer(newState)
        _uiState.value = _uiState.value.copy(equalizerState = newState)
    }

    fun setEqualizerBandGain(bandIndex: Int, gainDb: Int) {
        val currentEq = _uiState.value.equalizerState
        val newBands = currentEq.bands.mapIndexed { idx, band ->
            if (idx == bandIndex) band.copy(gainDb = gainDb) else band
        }
        val newState = currentEq.copy(presetName = "Custom", bands = newBands)
        audioEngine.applyEqualizer(newState)
        _uiState.value = _uiState.value.copy(equalizerState = newState)
    }

    fun setBassBoost(value: Int) {
        val newState = _uiState.value.equalizerState.copy(bassBoost = value.coerceIn(0, 100))
        audioEngine.applyEqualizer(newState)
        _uiState.value = _uiState.value.copy(equalizerState = newState)
    }

    fun setVirtualizer(value: Int) {
        val newState = _uiState.value.equalizerState.copy(virtualizer = value.coerceIn(0, 100))
        audioEngine.applyEqualizer(newState)
        _uiState.value = _uiState.value.copy(equalizerState = newState)
    }

    fun toggleEqualizer(enabled: Boolean) {
        val newState = _uiState.value.equalizerState.copy(isEnabled = enabled)
        audioEngine.applyEqualizer(newState)
        _uiState.value = _uiState.value.copy(equalizerState = newState)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _uiState.value = _uiState.value.copy(sleepTimerMinutesLeft = 0)
            return
        }

        _uiState.value = _uiState.value.copy(sleepTimerMinutesLeft = minutes)
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes
            while (remaining > 0) {
                delay(60000L) // 1 minute
                remaining--
                _uiState.value = _uiState.value.copy(sleepTimerMinutesLeft = remaining)
            }
            audioEngine.pause()
            _uiState.value = _uiState.value.copy(sleepTimerMinutesLeft = 0)
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistDao.insertPlaylist(PlaylistEntity(name = name.trim(), songIds = ""))
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlist)
            if (_uiState.value.selectedPlaylist?.id == playlist.id) {
                _uiState.value = _uiState.value.copy(selectedPlaylist = null)
            }
        }
    }

    fun addSongToPlaylist(playlist: PlaylistEntity, song: Song) {
        viewModelScope.launch {
            val ids = playlist.songIds.split(",").filter { it.isNotBlank() }.toMutableList()
            if (!ids.contains(song.id)) {
                ids.add(song.id)
                val updated = playlist.copy(songIds = ids.joinToString(","))
                playlistDao.updatePlaylist(updated)
                if (_uiState.value.selectedPlaylist?.id == playlist.id) {
                    _uiState.value = _uiState.value.copy(selectedPlaylist = updated)
                }
            }
        }
    }

    fun removeSongFromPlaylist(playlist: PlaylistEntity, songId: String) {
        viewModelScope.launch {
            val ids = playlist.songIds.split(",").filter { it.isNotBlank() && it != songId }
            val updated = playlist.copy(songIds = ids.joinToString(","))
            playlistDao.updatePlaylist(updated)
            if (_uiState.value.selectedPlaylist?.id == playlist.id) {
                _uiState.value = _uiState.value.copy(selectedPlaylist = updated)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setActiveTab(tab: LibraryTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab, selectedPlaylist = null)
    }

    fun selectPlaylist(playlist: PlaylistEntity?) {
        _uiState.value = _uiState.value.copy(selectedPlaylist = playlist)
    }

    fun toggleLyricsView() {
        _uiState.value = _uiState.value.copy(showLyrics = !_uiState.value.showLyrics)
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isPlayerExpanded = expanded)
    }

    fun shuffleAll() {
        val songs = _uiState.value.allSongs
        if (songs.isEmpty()) return
        val shuffled = songs.shuffled()
        _uiState.value = _uiState.value.copy(isShuffle = true)
        playSong(shuffled.first(), shuffled)
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
        sleepTimerJob?.cancel()
    }
}
