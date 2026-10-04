package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.viewmodel.LibraryTab
import com.example.viewmodel.MusicViewModel

enum class NavDestination(val title: String, val testTag: String) {
    HOME("الرئيسية", "nav_home"),
    LIBRARY("المكتبة", "nav_library"),
    EQUALIZER("المعادل", "nav_equalizer"),
    ABOUT("حول التطبيق", "nav_about")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                // Ensure RTL layout for Arabic primary support
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    NaghamApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun NaghamApp(viewModel: MusicViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val visualizerBars by viewModel.visualizerBars.collectAsState()

    var currentNav by remember { mutableStateOf(NavDestination.HOME) }

    // Audio storage permission launcher
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadMusicCatalog()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permission)
    }

    // Handle system back navigation
    BackHandler(enabled = uiState.isPlayerExpanded || uiState.selectedPlaylist != null) {
        if (uiState.isPlayerExpanded) {
            viewModel.setPlayerExpanded(false)
        } else if (uiState.selectedPlaylist != null) {
            viewModel.selectPlaylist(null)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DarkBackground,
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Mini Player (docked above navigation bar)
                    if (uiState.currentSong != null && !uiState.isPlayerExpanded) {
                        MiniPlayer(
                            currentSong = uiState.currentSong,
                            isPlaying = uiState.isPlaying,
                            progressMs = uiState.progressMs,
                            durationMs = uiState.durationMs,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.playNext() },
                            onFavoriteToggle = {
                                uiState.currentSong?.let { viewModel.toggleFavorite(it) }
                            },
                            onExpand = { viewModel.setPlayerExpanded(true) }
                        )
                    }

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = DarkSurface,
                        contentColor = NeonCyan,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bottom_nav_bar")
                    ) {
                        NavDestination.values().forEach { destination ->
                            val isSelected = currentNav == destination
                            val (filledIcon, outlinedIcon) = when (destination) {
                                NavDestination.HOME -> Pair(Icons.Filled.Home, Icons.Outlined.Home)
                                NavDestination.LIBRARY -> Pair(Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
                                NavDestination.EQUALIZER -> Pair(Icons.Filled.Equalizer, Icons.Outlined.Equalizer)
                                NavDestination.ABOUT -> Pair(Icons.Filled.Info, Icons.Outlined.Info)
                            }

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentNav = destination },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) filledIcon else outlinedIcon,
                                        contentDescription = destination.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF0C0D14),
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = Color(0xFF9DA5BF),
                                    unselectedTextColor = Color(0xFF9DA5BF)
                                ),
                                modifier = Modifier.testTag(destination.testTag)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .statusBarsPadding()
            ) {
                when (currentNav) {
                    NavDestination.HOME -> {
                        HomeScreen(
                            uiState = uiState,
                            visualizerBars = visualizerBars,
                            onPlaySong = { song -> viewModel.playSong(song) },
                            onShuffleAll = { viewModel.shuffleAll() },
                            onNavigateLibraryTab = { tab ->
                                viewModel.setActiveTab(tab)
                                currentNav = NavDestination.LIBRARY
                            },
                            onNavigateEqualizer = { currentNav = NavDestination.EQUALIZER },
                            onScanLocalAudio = {
                                permissionLauncher.launch(permission)
                                viewModel.loadMusicCatalog()
                            },
                            onOpenPlayer = { viewModel.setPlayerExpanded(true) }
                        )
                    }

                    NavDestination.LIBRARY -> {
                        LibraryScreen(
                            uiState = uiState,
                            onTabSelected = { tab -> viewModel.setActiveTab(tab) },
                            onSearchQueryChanged = { query -> viewModel.setSearchQuery(query) },
                            onPlaySong = { song, queue -> viewModel.playSong(song, queue) },
                            onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                            onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                            onDeletePlaylist = { pl -> viewModel.deletePlaylist(pl) },
                            onAddSongToPlaylist = { pl, song -> viewModel.addSongToPlaylist(pl, song) },
                            onRemoveSongFromPlaylist = { pl, id -> viewModel.removeSongFromPlaylist(pl, id) },
                            onSelectPlaylist = { pl -> viewModel.selectPlaylist(pl) }
                        )
                    }

                    NavDestination.EQUALIZER -> {
                        EqualizerScreen(
                            equalizerState = uiState.equalizerState,
                            visualizerBars = visualizerBars,
                            isPlaying = uiState.isPlaying,
                            onToggleEnabled = { enabled -> viewModel.toggleEqualizer(enabled) },
                            onSelectPreset = { preset -> viewModel.setEqualizerPreset(preset) },
                            onBandGainChanged = { idx, gain -> viewModel.setEqualizerBandGain(idx, gain) },
                            onBassBoostChanged = { boost -> viewModel.setBassBoost(boost) },
                            onVirtualizerChanged = { virt -> viewModel.setVirtualizer(virt) }
                        )
                    }

                    NavDestination.ABOUT -> {
                        AboutScreen()
                    }
                }
            }
        }

        // Full Screen Player Modal with smooth vertical slide
        AnimatedVisibility(
            visible = uiState.isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerScreen(
                uiState = uiState,
                visualizerBars = visualizerBars,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onSeekTo = { pos -> viewModel.seekTo(pos) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeatMode = { viewModel.cycleRepeatMode() },
                onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                onSetPlaybackSpeed = { speed -> viewModel.setPlaybackSpeed(speed) },
                onSetVolume = { vol -> viewModel.setVolume(vol) },
                onSetSleepTimer = { minutes -> viewModel.setSleepTimer(minutes) },
                onToggleLyrics = { viewModel.toggleLyricsView() },
                onNavigateEqualizer = {
                    viewModel.setPlayerExpanded(false)
                    currentNav = NavDestination.EQUALIZER
                },
                onCollapse = { viewModel.setPlayerExpanded(false) },
                onSelectQueueSong = { song -> viewModel.playSong(song) },
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            )
        }
    }
}
