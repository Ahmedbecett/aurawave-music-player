package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SyncedLyricsView
import com.example.ui.components.VisualizerView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.viewmodel.MusicUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    uiState: MusicUiState,
    visualizerBars: List<Float>,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetVolume: (Float) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onToggleLyrics: () -> Unit,
    onNavigateEqualizer: () -> Unit,
    onCollapse: () -> Unit,
    onSelectQueueSong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong = uiState.currentSong ?: return

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableFloatStateOf(0f) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }

    val displayProgress = if (isUserSeeking) seekPositionMs.toLong() else uiState.progressMs
    val totalDuration = if (uiState.durationMs > 0) uiState.durationMs else currentSong.durationMs

    // Rotating vinyl animation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotate")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "rotate"
    )

    // Sleep timer dialog
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentMinutesLeft = uiState.sleepTimerMinutesLeft,
            onSetTimer = { onSetSleepTimer(it) },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    // Queue Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة الانتظار (${uiState.queue.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Text(
                        text = if (uiState.isShuffle) "عشوائي مفعّل" else "تشغيل متسلسل",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                    itemsIndexed(uiState.queue) { index, song ->
                        val isSelected = song.id == currentSong.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF1E243B) else Color.Transparent)
                                .clickable {
                                    onSelectQueueSong(song)
                                    showQueueSheet = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) NeonCyan else Color(0xFF6B7280),
                                modifier = Modifier.width(28.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) NeonCyan else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9DA5BF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isSelected && uiState.isPlaying) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF151928),
                        Color(0xFF0F111D),
                        DarkBackground
                    )
                )
            )
            .testTag("full_player_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.testTag("player_collapse_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "تصغير المشغل",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "قيد التشغيل الآن",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9DA5BF)
                    )
                    Text(
                        text = currentSong.album,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(onClick = onNavigateEqualizer) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "معادل الصوت",
                            tint = NeonPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        if (uiState.sleepTimerMinutesLeft > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = NeonCyan) {
                                        Text("${uiState.sleepTimerMinutesLeft}د", color = Color(0xFF0C0D14))
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = "مؤقت النوم",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "مؤقت النوم",
                                tint = Color(0xFF9DA5BF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cover Art vs Lyrics Switcher Tabs
            TabRow(
                selectedTabIndex = if (uiState.showLyrics) 1 else 0,
                containerColor = Color(0xFF1B1E30),
                contentColor = NeonCyan,
                modifier = Modifier
                    .width(220.dp)
                    .clip(RoundedCornerShape(20.dp)),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[if (uiState.showLyrics) 1 else 0]),
                        color = NeonCyan
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = !uiState.showLyrics,
                    onClick = { if (uiState.showLyrics) onToggleLyrics() },
                    text = {
                        Text(
                            "غلاف الألبوم",
                            fontSize = 12.sp,
                            fontWeight = if (!uiState.showLyrics) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = uiState.showLyrics,
                    onClick = { if (!uiState.showLyrics) onToggleLyrics() },
                    text = {
                        Text(
                            "الكلمات",
                            fontSize = 12.sp,
                            fontWeight = if (uiState.showLyrics) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Center Area: Vinyl/Artwork OR Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.showLyrics) {
                    SyncedLyricsView(
                        lyrics = uiState.parsedLyrics,
                        activeLyricIndex = uiState.activeLyricIndex,
                        onSeekTo = onSeekTo,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Glowing Vinyl Album Art
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(16.dp)
                            .shadow(24.dp, shape = CircleShape, spotColor = NeonCyan)
                            .clip(CircleShape)
                            .background(Color(0xFF0F121C))
                            .border(6.dp, Brush.sweepGradient(listOf(NeonCyan, NeonPurple, ElectricBlue, NeonCyan)), CircleShape)
                            .rotate(if (uiState.isPlaying) rotationAngle else 0f),
                        contentAlignment = Alignment.Center
                    ) {
                        // Artwork Image
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.72f)
                                .clip(CircleShape)
                                .background(Color(0xFF222638)),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                currentSong.albumArtRes != null -> {
                                    Image(
                                        painter = painterResource(id = currentSong.albumArtRes),
                                        contentDescription = "غلاف الألبوم",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                !currentSong.albumArtUri.isNullOrBlank() -> {
                                    AsyncImage(
                                        model = currentSong.albumArtUri,
                                        contentDescription = "غلاف الألبوم",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                        }

                        // Center Vinyl Pin Hole
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0C0D14))
                                .border(2.dp, NeonCyan, CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Song Title, Artist & Favorite Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currentSong.artist} • ${currentSong.genre}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9DA5BF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onToggleFavorite(currentSong) },
                    modifier = Modifier.testTag("player_favorite_btn")
                ) {
                    Icon(
                        imageVector = if (currentSong.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "المفضلة",
                        tint = if (currentSong.isFavorite) NeonPink else Color(0xFF9DA5BF),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Spectrum Visualizer Bar
            VisualizerView(
                bars = visualizerBars,
                isPlaying = uiState.isPlaying,
                barHeight = 28.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Seekbar (Slider)
            Slider(
                value = displayProgress.toFloat(),
                onValueChange = {
                    isUserSeeking = true
                    seekPositionMs = it
                },
                onValueChangeFinished = {
                    onSeekTo(seekPositionMs.toLong())
                    isUserSeeking = false
                },
                valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_seekbar")
            )

            // Timestamps: Elapsed & Remaining
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val curSec = displayProgress / 1000
                Text(
                    text = "%d:%02d".format(curSec / 60, curSec % 60),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9DA5BF)
                )

                val durSec = totalDuration / 1000
                Text(
                    text = "%d:%02d".format(durSec / 60, durSec % 60),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9DA5BF)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Controls: Shuffle, Prev, Play/Pause, Next, Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.testTag("player_shuffle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "خلط",
                        tint = if (uiState.isShuffle) NeonCyan else Color(0xFF6B7280),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous Button
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.testTag("player_prev_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "السابق",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause Large Glow Button
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(16.dp, shape = CircleShape, spotColor = NeonCyan)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(NeonCyan, Color(0xFF00BFA5))
                            )
                        )
                        .testTag("player_play_pause_btn")
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "إيقاف مؤقت" else "تشغيل",
                        tint = Color(0xFF0C0D14),
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Next Button
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.testTag("player_next_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "التالي",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat Mode Button
                IconButton(
                    onClick = onCycleRepeatMode,
                    modifier = Modifier.testTag("player_repeat_btn")
                ) {
                    val (icon, tint) = when (uiState.repeatMode) {
                        RepeatMode.OFF -> Pair(Icons.Default.Repeat, Color(0xFF6B7280))
                        RepeatMode.ALL -> Pair(Icons.Default.Repeat, NeonCyan)
                        RepeatMode.ONE -> Pair(Icons.Default.RepeatOne, NeonPurple)
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "تكرار",
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Secondary Controls: Speed, Volume Toggle, Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback speed
                Box {
                    Text(
                        text = "${uiState.playbackSpeed}x",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (uiState.playbackSpeed != 1.0f) NeonCyan else Color(0xFF9DA5BF),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2236))
                            .clickable {
                                val nextSpeed = when (uiState.playbackSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    2.0f -> 0.75f
                                    else -> 1.0f
                                }
                                onSetPlaybackSpeed(nextSpeed)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Volume slider toggle
                IconButton(
                    onClick = { showVolumeSlider = !showVolumeSlider }
                ) {
                    Icon(
                        imageVector = if (uiState.volume > 0.5f) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        contentDescription = "مستوى الصوت",
                        tint = if (showVolumeSlider) NeonCyan else Color(0xFF9DA5BF),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Queue button
                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.testTag("player_queue_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "قائمة الانتظار",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Volume Slider Popup row
            AnimatedVisibility(visible = showVolumeSlider) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeDown,
                        contentDescription = null,
                        tint = Color(0xFF9DA5BF),
                        modifier = Modifier.size(18.dp)
                    )
                    Slider(
                        value = uiState.volume,
                        onValueChange = onSetVolume,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
