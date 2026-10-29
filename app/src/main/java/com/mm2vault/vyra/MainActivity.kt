package com.mm2vault.vyra

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.mm2vault.vyra.playback.PlaybackService
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF090A12)
private val Violet = Color(0xFF9A7BFF)
private val Muted = Color(0xFFAAA8BC)
private val Glass = Color(0x20FFFFFF)

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var pendingAudioUri: Uri? = null
    private var pendingAudioTitle: String? = null

    private val isPlayingState = mutableStateOf(false)
    private val trackTitleState = mutableStateOf("No track selected")
    private val trackArtistState = mutableStateOf("Open a local audio file")

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            isPlayingState.value = isPlaying
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            mediaMetadata.title?.toString()?.let { trackTitleState.value = it }
            mediaMetadata.artist?.toString()?.let { trackArtistState.value = it }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaMetadata?.let {
                it.title?.toString()?.let { title -> trackTitleState.value = title }
                it.artist?.toString()?.let { artist -> trackArtistState.value = artist }
            }
        }
    }

    private val audioPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val title = queryDisplayName(uri)
            val controller = mediaController
            if (controller == null) {
                pendingAudioUri = uri
                pendingAudioTitle = title
                trackTitleState.value = title
                trackArtistState.value = "Waiting for player connection"
            } else {
                playLocalAudio(controller, uri, title)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VyraApp(
                isPlaying = isPlayingState.value,
                trackTitle = trackTitleState.value,
                trackArtist = trackArtistState.value,
                onOpenAudio = { audioPicker.launch(arrayOf("audio/*")) },
                onPlayPause = {
                    mediaController?.let { controller ->
                        if (controller.isPlaying) controller.pause()
                        else if (controller.mediaItemCount > 0) controller.play()
                    }
                },
                onPrevious = {
                    mediaController?.let { if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem() }
                },
                onNext = {
                    mediaController?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching { future.get() }.onSuccess { controller ->
                if (controllerFuture !== future || isFinishing || isDestroyed) {
                    MediaController.releaseFuture(future)
                } else {
                    mediaController = controller
                    controller.addListener(playerListener)
                    isPlayingState.value = controller.isPlaying
                    controller.currentMediaItem?.mediaMetadata?.let {
                        it.title?.toString()?.let { title -> trackTitleState.value = title }
                        it.artist?.toString()?.let { artist -> trackArtistState.value = artist }
                    }
                    val queuedUri = pendingAudioUri
                    if (queuedUri != null) {
                        playLocalAudio(controller, queuedUri, pendingAudioTitle ?: "Local audio")
                        pendingAudioUri = null
                        pendingAudioTitle = null
                    }
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        mediaController?.removeListener(playerListener)
        mediaController = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onStop()
    }

    private fun playLocalAudio(controller: MediaController, uri: Uri, title: String) {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist("Local audio")
            .build()
        val item = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()

        trackTitleState.value = title
        trackArtistState.value = "Local audio"
        controller.setMediaItem(item)
        controller.prepare()
        controller.play()
    }

    private fun queryDisplayName(uri: Uri): String {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) cursor.getString(index) else null
                    } else null
                }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Local audio"
    }
}

@Composable
private fun VyraApp(
    isPlaying: Boolean,
    trackTitle: String,
    trackArtist: String,
    onOpenAudio: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Home") }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Ink
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF21183A), Ink, Color(0xFF11101B))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 22.dp)
                ) {
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("VYRA", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                            Text("YOUR SOUND. YOUR SPACE.", color = Muted, fontSize = 9.sp, letterSpacing = 2.sp)
                        }
                        GlassIcon(Icons.Default.NotificationsNone)
                        Spacer(Modifier.width(10.dp))
                        GlassIcon(Icons.Default.AccountCircle)
                    }
                    Spacer(Modifier.height(30.dp))
                    Text("Discover", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("Find the sound that feels like you.", color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        listOf("For you", "Trending", "New").forEachIndexed { index, item ->
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(if (index == 0) Violet else Glass)
                                    .border(1.dp, Color(0x24FFFFFF), CircleShape)
                                    .clickable { }
                                    .padding(horizontal = 17.dp, vertical = 10.dp)
                            ) {
                                Text(item, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Spacer(Modifier.height(25.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Your music", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text("Play audio stored on this device.", color = Muted, fontSize = 11.sp)
                        }
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .background(Violet)
                                .clickable(onClick = onOpenAudio)
                                .padding(horizontal = 15.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderOpen, null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Open audio", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("Made for your mood", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(13.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        items(listOf("Midnight\nDrive", "Soft\nFocus", "After\nHours")) { title ->
                            MoodCard(title)
                        }
                    }
                    Spacer(Modifier.height(25.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Recently played", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("On this device", color = Violet, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    SongRow("Your local tracks", "Choose an audio file to begin", 0)
                    Spacer(Modifier.weight(1f))
                    MiniPlayer(
                        isPlaying, trackTitle, trackArtist,
                        onPlayPause, onPrevious, onNext
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xCC171622))
                            .padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf(Icons.Default.Home to "Home", Icons.Default.Search to "Search", Icons.Default.LibraryMusic to "Library").forEach { (icon, label) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { selectedTab = label }
                            ) {
                                Icon(icon, null, tint = if (selectedTab == label) Violet else Muted)
                                Text(label, color = if (selectedTab == label) Color.White else Muted, fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun GlassIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        Modifier.size(42.dp).clip(CircleShape)
            .background(Glass).border(1.dp, Color(0x30FFFFFF), CircleShape),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = Color.White) }
}

@Composable
private fun MoodCard(title: String) {
    val gradients = listOf(
        listOf(Color(0xFF7452C8), Color(0xFF21183A)),
        listOf(Color(0xFF386D8D), Color(0xFF172A40)),
        listOf(Color(0xFFB05B83), Color(0xFF45213D))
    )
    val index = when {
        title.startsWith("Midnight") -> 0
        title.startsWith("Soft") -> 1
        else -> 2
    }
    Box(
        Modifier.width(148.dp).height(158.dp).clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(gradients[index]))
            .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(22.dp))
            .padding(15.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 23.sp)
    }
}

@Composable
private fun SongRow(title: String, artist: String, index: Int) {
    val covers = listOf(Color(0xFF604B91), Color(0xFF315C6C), Color(0xFF87506E))
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(listOf(covers[index], Color(0xFF171522))))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(artist, color = Muted, fontSize = 11.sp)
        }
        Icon(Icons.Default.MoreHoriz, null, tint = Muted)
    }
}

@Composable
private fun MiniPlayer(
    playing: Boolean,
    title: String,
    artist: String,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp))
            .background(Color(0xCC292438))
            .border(1.dp, Color(0x45FFFFFF), RoundedCornerShape(19.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(43.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Violet, Color(0xFF34244F))))
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(artist, color = Muted, fontSize = 10.sp, maxLines = 1)
        }
        Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.clickable(onClick = onPrevious))
        Spacer(Modifier.width(12.dp))
        Icon(
            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
            null,
            tint = Color.White,
            modifier = Modifier.size(28.dp).clickable { onToggle() }
        )
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.clickable(onClick = onNext))
    }
}
