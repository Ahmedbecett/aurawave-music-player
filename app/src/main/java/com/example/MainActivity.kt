package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.AutoAwesome
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.localization.AppLanguage
import com.example.localization.Localization
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AiSongFinderScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.viewmodel.LibraryTab
import com.example.viewmodel.MusicViewModel

enum class NavDestination(val testTag: String) {
    HOME("nav_home"),
    LIBRARY("nav_library"),
    AI_FINDER("nav_ai_finder"),
    EQUALIZER("nav_equalizer"),
    ABOUT("nav_about")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val layoutDirection = if (uiState.language == AppLanguage.ARABIC) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            MyApplicationTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    AuraWaveApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AuraWaveApp(viewModel: MusicViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val visualizerBars by viewModel.visualizerBars.collectAsState()
    val strings = Localization.get(uiState.language)

    var currentNav by remember { mutableStateOf(NavDestination.HOME) }

    // Multi-permission request for Audio storage and Microphone
    val permissionsToRequest = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        add(Manifest.permission.RECORD_AUDIO)
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioGranted = results.entries.any {
            (it.key == Manifest.permission.READ_MEDIA_AUDIO || it.key == Manifest.permission.READ_EXTERNAL_STORAGE) && it.value
        }
        if (audioGranted) {
            viewModel.setStoragePermission(true)
            Toast.makeText(context, strings.permissionGranted, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    // Audio File Picker for importing custom music files directly from device storage
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importAudioFiles(uris)
            Toast.makeText(context, "${uris.size} tracks added to AuraWave!", Toast.LENGTH_SHORT).show()
        }
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

                    // Bottom Navigation Bar with 5 destinations
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
                            val (title, filledIcon, outlinedIcon) = when (destination) {
                                NavDestination.HOME -> Triple(strings.navHome, Icons.Filled.Home, Icons.Outlined.Home)
                                NavDestination.LIBRARY -> Triple(strings.navLibrary, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
                                NavDestination.AI_FINDER -> Triple(strings.navAiFinder, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
                                NavDestination.EQUALIZER -> Triple(strings.navEqualizer, Icons.Filled.Equalizer, Icons.Outlined.Equalizer)
                                NavDestination.ABOUT -> Triple(strings.navAbout, Icons.Filled.Info, Icons.Outlined.Info)
                            }

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentNav = destination },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) filledIcon else outlinedIcon,
                                        contentDescription = title
                                    )
                                },
                                label = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 10.sp
                                        ),
                                        maxLines = 1
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
                            onNavigateAiFinder = { currentNav = NavDestination.AI_FINDER },
                            onRequestStoragePermission = {
                                permissionLauncher.launch(permissionsToRequest)
                            },
                            onImportFiles = {
                                audioPickerLauncher.launch(arrayOf("audio/*", "application/ogg"))
                            },
                            onScanFiles = {
                                viewModel.loadMusicCatalog()
                            },
                            onSetLanguage = { lang -> viewModel.setLanguage(lang) },
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
                            onSelectPlaylist = { pl -> viewModel.selectPlaylist(pl) },
                            onImportFiles = {
                                audioPickerLauncher.launch(arrayOf("audio/*", "application/ogg"))
                            },
                            onScanFiles = {
                                viewModel.loadMusicCatalog()
                            }
                        )
                    }

                    NavDestination.AI_FINDER -> {
                        AiSongFinderScreen(
                            currentLanguage = uiState.language,
                            aiRecognizer = viewModel.aiRecognizer,
                            visualizerBars = visualizerBars,
                            onPlayMatchedSong = { title, artist ->
                                viewModel.playMatchedAiSong(title, artist)
                            }
                        )
                    }

                    NavDestination.EQUALIZER -> {
                        EqualizerScreen(
                            equalizerState = uiState.equalizerState,
                            visualizerBars = visualizerBars,
                            isPlaying = uiState.isPlaying,
                            currentLanguage = uiState.language,
                            onToggleEnabled = { enabled -> viewModel.toggleEqualizer(enabled) },
                            onSelectPreset = { preset -> viewModel.setEqualizerPreset(preset) },
                            onBandGainChanged = { idx, gain -> viewModel.setEqualizerBandGain(idx, gain) },
                            onBassBoostChanged = { boost -> viewModel.setBassBoost(boost) },
                            onVirtualizerChanged = { virt -> viewModel.setVirtualizer(virt) }
                        )
                    }

                    NavDestination.ABOUT -> {
                        AboutScreen(
                            currentLanguage = uiState.language,
                            onSetLanguage = { lang -> viewModel.setLanguage(lang) }
                        )
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
