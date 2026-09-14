package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SongItem
import com.example.data.model.VoiceSourceType
import com.example.ui.theme.MicLive
import com.example.ui.theme.NfcBorder
import com.example.ui.theme.NfcCyan
import com.example.ui.theme.NfcGold
import com.example.ui.theme.NfcMidnight
import com.example.ui.theme.NfcSurfaceCard
import com.example.ui.theme.NfcSurfaceDark
import com.example.ui.theme.RecordingRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VolumeDucked

@Composable
fun NfcTopBar(
    title: String,
    onOpenDuckingSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NfcMidnight,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // NFC Monogram Badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(NfcGold, Color(0xFFFB8500))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NFC",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Nizam Farooqui Collection",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = title,
                        color = NfcCyan,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenDuckingSettings,
                    modifier = Modifier.testTag("top_ducking_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Ducking Settings",
                        tint = NfcGold
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceDuckingIndicator(
    isDucked: Boolean,
    activeSources: Set<VoiceSourceType>,
    duckingPercent: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = isDucked) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        val sourceText = when {
            activeSources.contains(VoiceSourceType.LIVE_MIC) && activeSources.contains(VoiceSourceType.VOICE_MP3) ->
                "Live Mic + Voice MP3 Active"
            activeSources.contains(VoiceSourceType.LIVE_MIC) ->
                "Live Microphone Active"
            activeSources.contains(VoiceSourceType.VOICE_MP3) ->
                "Voice MP3 Playing"
            activeSources.contains(VoiceSourceType.SAVED_RECORDING) ->
                "Voice Recording Playing"
            activeSources.contains(VoiceSourceType.PREVIEW_TEST) ->
                "Ducking Test Simulation Active"
            else -> "Voice Active"
        }

        Surface(
            color = VolumeDucked.copy(alpha = 0.15f * alpha),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VolumeDucked.copy(alpha = 0.6f)),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeDown,
                    contentDescription = "Audio Ducked",
                    tint = VolumeDucked,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AUDIO DUCKED: $sourceText",
                        color = VolumeDucked,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Background music volume smoothly reduced to $duckingPercent%",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "$duckingPercent%",
                    color = VolumeDucked,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun NfcMiniPlayer(
    song: SongItem?,
    isPlaying: Boolean,
    isDucked: Boolean,
    duckingPercent: Int,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NfcSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDucked) VolumeDucked.copy(alpha = 0.8f) else NfcBorder
        ),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onClick() }
            .testTag("mini_player_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or Music Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NfcSurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                if (song.artworkUrl != null) {
                    AsyncImage(
                        model = song.artworkUrl,
                        contentDescription = "Album Artwork",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isDucked) VolumeDucked else NfcGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.artist,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isDucked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• Ducked ($duckingPercent%)",
                            color = VolumeDucked,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.testTag("mini_player_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = NfcGold
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.testTag("mini_player_next")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
fun RenameDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("rename_input_field")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NfcGold),
                modifier = Modifier.testTag("confirm_rename_button")
            ) {
                Text("Save", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = NfcSurfaceDark
    )
}

@Composable
fun DeleteConfirmDialog(
    itemTitle: String,
    isPermanentDelete: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPermanentDelete) "Delete File Permanently?" else "Remove from NFC Library?",
                color = if (isPermanentDelete) RecordingRed else TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = if (isPermanentDelete) {
                    "Are you sure you want to permanently delete \"$itemTitle\" from your device storage? This cannot be undone."
                } else {
                    "Remove \"$itemTitle\" from NFC app library? The original file on your phone will NOT be deleted."
                },
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPermanentDelete) RecordingRed else NfcGold
                ),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(
                    text = if (isPermanentDelete) "Delete Permanently" else "Remove from Library",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = NfcSurfaceDark
    )
}

@Composable
fun SaveRecordingDialog(
    initialTitle: String,
    durationMs: Long,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    val durationSec = durationMs / 1000L
    val minutes = durationSec / 60
    val seconds = durationSec % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Save Voice Recording", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Duration: $timeFormatted • Original Voice Quality",
                    color = NfcCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Recording Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("save_recording_title_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.ifBlank { initialTitle }) },
                colors = ButtonDefaults.buttonColors(containerColor = NfcGold),
                modifier = Modifier.testTag("save_recording_confirm_button")
            ) {
                Text("Save Recording", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Discard", color = RecordingRed)
            }
        },
        containerColor = NfcSurfaceDark
    )
}

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
