package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PlaylistEntity
import com.example.model.Song
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.viewmodel.LibraryTab
import com.example.viewmodel.MusicUiState

@Composable
fun LibraryScreen(
    uiState: MusicUiState,
    onTabSelected: (LibraryTab) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onAddSongToPlaylist: (PlaylistEntity, Song) -> Unit,
    onRemoveSongFromPlaylist: (PlaylistEntity, String) -> Unit,
    onSelectPlaylist: (PlaylistEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songForPlaylistDialog by remember { mutableStateOf<Song?>(null) }
    var filterArtist by remember { mutableStateOf<String?>(null) }
    var filterAlbum by remember { mutableStateOf<String?>(null) }
    var filterGenre by remember { mutableStateOf<String?>(null) }

    // Dialogs
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name -> onCreatePlaylist(name) }
        )
    }

    songForPlaylistDialog?.let { targetSong ->
        AddToPlaylistDialog(
            song = targetSong,
            playlists = uiState.playlists,
            onSelectPlaylist = { pl -> onAddSongToPlaylist(pl, targetSong) },
            onCreateNewRequested = { showCreatePlaylistDialog = true },
            onDismiss = { songForPlaylistDialog = null }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            // Header / Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("بحث عن أغنية، فنان، ألبوم...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = NeonCyan
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "مسح",
                                    tint = Color(0xFF9DA5BF)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_field")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playlist Detail View Back Bar (if selected)
            if (uiState.selectedPlaylist != null || filterArtist != null || filterAlbum != null || filterGenre != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onSelectPlaylist(null)
                            filterArtist = null
                            filterAlbum = null
                            filterGenre = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = NeonCyan
                        )
                    }
                    val title = uiState.selectedPlaylist?.name
                        ?: filterArtist?.let { "فنان: $it" }
                        ?: filterAlbum?.let { "ألبوم: $it" }
                        ?: filterGenre?.let { "تصنيف: $it" }
                        ?: ""
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (uiState.selectedPlaylist != null) {
                        IconButton(
                            onClick = { onDeletePlaylist(uiState.selectedPlaylist) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف القائمة",
                                tint = Color(0xFFF72585)
                            )
                        }
                    }
                }
            } else {
                // Category Tabs
                val tabs = LibraryTab.values()
                val selectedTabIndex = tabs.indexOf(uiState.activeTab).coerceAtLeast(0)

                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = NeonCyan,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = NeonCyan
                            )
                        }
                    },
                    divider = {}
                ) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = uiState.activeTab == tab,
                            onClick = { onTabSelected(tab) },
                            text = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (uiState.activeTab == tab) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (uiState.activeTab == tab) NeonCyan else Color(0xFF9DA5BF)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Content Area based on tab or active filter
            val allSongs = uiState.allSongs
            val query = uiState.searchQuery.trim().lowercase()

            val displayedSongs = when {
                uiState.selectedPlaylist != null -> {
                    val ids = uiState.selectedPlaylist.songIds.split(",").toSet()
                    allSongs.filter { ids.contains(it.id) }
                }
                filterArtist != null -> allSongs.filter { it.artist == filterArtist }
                filterAlbum != null -> allSongs.filter { it.album == filterAlbum }
                filterGenre != null -> allSongs.filter { it.genre == filterGenre }
                uiState.activeTab == LibraryTab.FAVORITES -> allSongs.filter { uiState.favoriteIds.contains(it.id) }
                else -> allSongs
            }.filter { song ->
                if (query.isBlank()) true
                else song.title.lowercase().contains(query) ||
                        song.artist.lowercase().contains(query) ||
                        song.album.lowercase().contains(query) ||
                        song.genre.lowercase().contains(query)
            }

            // Tab-specific views (Playlists, Artists, Albums, Genres) when no sub-filter is selected
            if (uiState.selectedPlaylist == null && filterArtist == null && filterAlbum == null && filterGenre == null) {
                when (uiState.activeTab) {
                    LibraryTab.PLAYLISTS -> {
                        PlaylistsList(
                            playlists = uiState.playlists,
                            allSongs = allSongs,
                            onSelectPlaylist = onSelectPlaylist,
                            onCreateNew = { showCreatePlaylistDialog = true }
                        )
                    }
                    LibraryTab.ARTISTS -> {
                        ArtistsGrid(
                            songs = allSongs,
                            onSelectArtist = { filterArtist = it }
                        )
                    }
                    LibraryTab.ALBUMS -> {
                        AlbumsGrid(
                            songs = allSongs,
                            onSelectAlbum = { filterAlbum = it }
                        )
                    }
                    LibraryTab.GENRES -> {
                        GenresGrid(
                            songs = allSongs,
                            onSelectGenre = { filterGenre = it }
                        )
                    }
                    else -> {
                        // Songs or Favorites
                        SongsListView(
                            songs = displayedSongs,
                            currentSong = uiState.currentSong,
                            isPlaying = uiState.isPlaying,
                            favoriteIds = uiState.favoriteIds,
                            onSongClick = { song -> onPlaySong(song, displayedSongs) },
                            onToggleFavorite = onToggleFavorite,
                            onAddToPlaylist = { song -> songForPlaylistDialog = song }
                        )
                    }
                }
            } else {
                // Display filtered song list for playlist / artist / album / genre
                SongsListView(
                    songs = displayedSongs,
                    currentSong = uiState.currentSong,
                    isPlaying = uiState.isPlaying,
                    favoriteIds = uiState.favoriteIds,
                    onSongClick = { song -> onPlaySong(song, displayedSongs) },
                    onToggleFavorite = onToggleFavorite,
                    onAddToPlaylist = { song -> songForPlaylistDialog = song },
                    onRemoveFromPlaylist = if (uiState.selectedPlaylist != null) { songId ->
                        onRemoveSongFromPlaylist(uiState.selectedPlaylist, songId)
                    } else null
                )
            }
        }

        // Floating Action Button for Playlists tab
        if (uiState.activeTab == LibraryTab.PLAYLISTS && uiState.selectedPlaylist == null) {
            FloatingActionButton(
                onClick = { showCreatePlaylistDialog = true },
                containerColor = NeonCyan,
                contentColor = Color(0xFF0C0D14),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 110.dp, end = 20.dp)
                    .testTag("fab_create_playlist")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "قائمة تشغيل جديدة")
            }
        }
    }
}

@Composable
fun SongsListView(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onRemoveFromPlaylist: ((String) -> Unit)? = null
) {
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "لا توجد أغانٍ متوفرة في هذا القسم",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9DA5BF)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_songs_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            val isCurrent = currentSong?.id == song.id
            val isFav = favoriteIds.contains(song.id)

            SongListItem(
                song = song,
                isCurrent = isCurrent,
                isPlaying = isPlaying && isCurrent,
                isFavorite = isFav,
                onClick = { onSongClick(song) },
                onToggleFavorite = { onToggleFavorite(song) },
                onAddToPlaylist = { onAddToPlaylist(song) },
                onRemoveFromPlaylist = if (onRemoveFromPlaylist != null) { { onRemoveFromPlaylist(song.id) } } else null
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SongListItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("song_item_${song.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFF1E243B) else DarkCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF222638)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    song.albumArtRes != null -> {
                        Image(
                            painter = painterResource(id = song.albumArtRes),
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    !song.albumArtUri.isNullOrBlank() -> {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x80000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = if (isCurrent) NeonCyan else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color(0xFF9DA5BF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "المفضلة",
                    tint = if (isFavorite) NeonPink else Color(0xFF6B7280),
                    modifier = Modifier.size(20.dp)
                )
            }

            // More Options
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "خيارات إضافية",
                        tint = Color(0xFF9DA5BF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(DarkCard)
                ) {
                    DropdownMenuItem(
                        text = { Text("تشغيل الآن", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonCyan)
                        },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("إضافة إلى قائمة تشغيل", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = NeonPurple)
                        },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                    if (onRemoveFromPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("إزالة من القائمة", color = Color(0xFFF72585)) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF72585))
                            },
                            onClick = {
                                menuExpanded = false
                                onRemoveFromPlaylist()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistsList(
    playlists: List<PlaylistEntity>,
    allSongs: List<Song>,
    onSelectPlaylist: (PlaylistEntity) -> Unit,
    onCreateNew: () -> Unit
) {
    if (playlists.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "لا توجد قوائم تشغيل حتى الآن",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9DA5BF)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "اضغط على الزر أدناه لإنشاء قائمة تشغيل مخصصة",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)
    ) {
        items(playlists) { playlist ->
            val count = playlist.songIds.split(",").filter { it.isNotBlank() }.size

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectPlaylist(playlist) },
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF222638)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$count مقطع موسيقي",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9DA5BF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistsGrid(
    songs: List<Song>,
    onSelectArtist: (String) -> Unit
) {
    val artists = songs.groupBy { it.artist }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)
    ) {
        items(artists.keys.toList()) { artistName ->
            val count = artists[artistName]?.size ?: 0
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectArtist(artistName) },
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222638)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "$count أغانٍ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9DA5BF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumsGrid(
    songs: List<Song>,
    onSelectAlbum: (String) -> Unit
) {
    val albums = songs.groupBy { it.album }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)
    ) {
        items(albums.keys.toList()) { albumName ->
            val albumSongs = albums[albumName] ?: emptyList()
            val firstSong = albumSongs.firstOrNull()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectAlbum(albumName) },
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF222638)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            firstSong?.albumArtRes != null -> {
                                Image(
                                    painter = painterResource(id = firstSong.albumArtRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            !firstSong?.albumArtUri.isNullOrBlank() -> {
                                AsyncImage(
                                    model = firstSong?.albumArtUri,
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = albumName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "${firstSong?.artist} • ${albumSongs.size} مقاطع",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9DA5BF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GenresGrid(
    songs: List<Song>,
    onSelectGenre: (String) -> Unit
) {
    val genres = songs.groupBy { it.genre }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)
    ) {
        items(genres.keys.toList()) { genreName ->
            val count = genres[genreName]?.size ?: 0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectGenre(genreName) },
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222638)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = genreName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "$count أغانٍ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9DA5BF)
                        )
                    }
                }
            }
        }
    }
}
