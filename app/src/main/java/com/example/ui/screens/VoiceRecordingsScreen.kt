package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.audio.recording.RecordingState
import com.example.data.model.SongItem
import com.example.data.model.VoiceRecording
import com.example.ui.NfcViewModel
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.RenameDialog
import com.example.ui.components.SaveRecordingDialog
import com.example.ui.components.VoiceDuckingIndicator
import com.example.ui.components.formatDuration
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
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VolumeDucked
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecordingsScreen(
    viewModel: NfcViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val isDucked by viewModel.isDucked.collectAsState()
    val activeVoiceSources by viewModel.activeVoiceSources.collectAsState()
    val duckingSettings by viewModel.duckingSettings.collectAsState()

    val recordingState by viewModel.recordingState.collectAsState()
    val recordingDuration by viewModel.recordingDurationMs.collectAsState()
    val recordingError by viewModel.recordingError.collectAsState()

    val savedRecordings by viewModel.savedRecordings.collectAsState()
    val voiceMp3Songs by viewModel.voiceMp3Songs.collectAsState()

    // Voice playback state
    val voiceIsPlaying by viewModel.voiceIsPlaying.collectAsState()
    val activeVoiceId by viewModel.activeVoiceId.collectAsState()
    val currentVoiceTitle by viewModel.currentVoiceTitle.collectAsState()
    val voicePos by viewModel.voicePosition.collectAsState()
    val voiceDur by viewModel.voiceDuration.collectAsState()

    val context = LocalContext.current

    // Dialog states
    var showSaveDialog by remember { mutableStateOf(false) }
    var recordingDurationAtStop by remember { mutableStateOf(0L) }

    var recordingToRename by remember { mutableStateOf<VoiceRecording?>(null) }
    var recordingToDelete by remember { mutableStateOf<VoiceRecording?>(null) }

    var voiceMp3ToRename by remember { mutableStateOf<SongItem?>(null) }
    var voiceMp3ToDelete by remember { mutableStateOf<SongItem?>(null) }
    var isMp3PermanentDelete by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        }
    }

    val voiceMp3Picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            viewModel.importAudioFile(uri, isVoiceMp3 = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NfcMidnight)
            .padding(horizontal = 16.dp)
    ) {
        VoiceDuckingIndicator(
            isDucked = isDucked,
            activeSources = activeVoiceSources,
            duckingPercent = duckingSettings?.duckingLevelPercent ?: 10,
            onClick = {}
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tabs: [Record Voice] and [Imported Voice MP3s]
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = NfcSurfaceDark,
            contentColor = NfcGold
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Voice Recorder", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("tab_voice_recorder")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Imported Voice MP3s (${voiceMp3Songs.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("tab_imported_voice_mp3s")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Persistent Voice Player Bar (if a recording or voice MP3 is currently playing)
        if (activeVoiceId != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NfcSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, VolumeDucked),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = VolumeDucked,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PLAYING VOICE AUDIO: $currentVoiceTitle",
                                color = VolumeDucked,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Background music is continuously ducked during speech",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleVoicePlayPause() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (voiceIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (voiceIsPlaying) "Pause" else "Play",
                                tint = NfcGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.stopVoicePlayback() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (voiceDur > 0) {
                        val progress = (voicePos.toFloat() / voiceDur.toFloat()).coerceIn(0f, 1f)
                        Slider(
                            value = progress,
                            onValueChange = { viewModel.seekVoiceTo((it * voiceDur).toLong()) },
                            colors = SliderDefaults.colors(
                                thumbColor = VolumeDucked,
                                activeTrackColor = VolumeDucked,
                                inactiveTrackColor = NfcMidnight
                            ),
                            modifier = Modifier.fillMaxWidth().height(24.dp)
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // TAB 0: Voice Recorder & Saved Recordings
                RecordingConsole(
                    recordingState = recordingState,
                    durationMs = recordingDuration,
                    error = recordingError,
                    onStart = {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onPause = { viewModel.pauseRecording() },
                    onResume = { viewModel.resumeRecording() },
                    onStop = {
                        recordingDurationAtStop = recordingDuration
                        showSaveDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SAVED RECORDINGS (${savedRecordings.size})",
                    color = NfcCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (savedRecordings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved voice recordings yet.\nTap the Record button above to make your first recording.",
                            color = TextTertiary,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedRecordings, key = { it.id }) { rec ->
                            val isThisPlaying = voiceIsPlaying && activeVoiceId == "rec_${rec.id}"
                            SavedRecordingItemRow(
                                recording = rec,
                                isPlaying = isThisPlaying,
                                onPlay = {
                                    if (isThisPlaying) viewModel.toggleVoicePlayPause()
                                    else viewModel.playSavedRecording(rec)
                                },
                                onRename = { recordingToRename = rec },
                                onDelete = { recordingToDelete = rec },
                                onShare = {
                                    shareAudioFile(context, rec.filePath, rec.title)
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
            1 -> {
                // TAB 1: User's Imported Voice MP3s (Requirement 5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add/Import your phone's voice MP3 files",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = {
                            voiceMp3Picker.launch(arrayOf("audio/*", "application/ogg", "audio/mpeg"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NfcGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("import_voice_mp3_button")
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Voice MP3", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (voiceMp3Songs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Voice MP3s imported yet",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Have an existing voice.mp3 on your phone? Tap 'Add Voice MP3' above to import it. When played, it will automatically duck background music!",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(voiceMp3Songs, key = { it.id }) { song ->
                            val isThisPlaying = voiceIsPlaying && activeVoiceId == "mp3_${song.id}"
                            ImportedVoiceMp3Row(
                                song = song,
                                isPlaying = isThisPlaying,
                                onPlay = {
                                    if (isThisPlaying) viewModel.toggleVoicePlayPause()
                                    else viewModel.playVoiceMp3(song)
                                },
                                onRename = { voiceMp3ToRename = song },
                                onRemoveFromLibrary = {
                                    voiceMp3ToDelete = song
                                    isMp3PermanentDelete = false
                                },
                                onDeletePermanently = {
                                    voiceMp3ToDelete = song
                                    isMp3PermanentDelete = true
                                },
                                onShare = {
                                    try {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "audio/*"
                                            putExtra(Intent.EXTRA_STREAM, Uri.parse(song.uriString))
                                            putExtra(Intent.EXTRA_SUBJECT, song.title)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share ${song.title}"))
                                    } catch (_: Exception) {}
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    // Save Recording Dialog
    if (showSaveDialog) {
        val defaultTitle = "NFC Voice ${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
        SaveRecordingDialog(
            initialTitle = defaultTitle,
            durationMs = recordingDurationAtStop,
            onSave = { title ->
                viewModel.stopAndSaveRecording(title)
                showSaveDialog = false
            },
            onDismiss = {
                viewModel.stopAndSaveRecording(null)
                showSaveDialog = false
            }
        )
    }

    // Recording Rename Dialog
    recordingToRename?.let { rec ->
        RenameDialog(
            currentTitle = rec.title,
            onConfirm = { newTitle ->
                viewModel.renameRecording(rec.id, newTitle)
                recordingToRename = null
            },
            onDismiss = { recordingToRename = null }
        )
    }

    // Recording Delete Dialog
    recordingToDelete?.let { rec ->
        DeleteConfirmDialog(
            itemTitle = rec.title,
            isPermanentDelete = true,
            onConfirm = {
                viewModel.deleteRecording(rec)
                recordingToDelete = null
            },
            onDismiss = { recordingToDelete = null }
        )
    }

    // Voice MP3 Rename Dialog
    voiceMp3ToRename?.let { song ->
        RenameDialog(
            currentTitle = song.title,
            onConfirm = { newTitle ->
                viewModel.renameSong(song.id, newTitle)
                voiceMp3ToRename = null
            },
            onDismiss = { voiceMp3ToRename = null }
        )
    }

    // Voice MP3 Delete / Remove Dialog
    voiceMp3ToDelete?.let { song ->
        DeleteConfirmDialog(
            itemTitle = song.title,
            isPermanentDelete = isMp3PermanentDelete,
            onConfirm = {
                if (isMp3PermanentDelete) {
                    viewModel.deleteSongPermanently(song)
                } else {
                    viewModel.removeSongFromLibrary(song.id)
                }
                voiceMp3ToDelete = null
            },
            onDismiss = { voiceMp3ToDelete = null }
        )
    }
}

@Composable
fun RecordingConsole(
    recordingState: RecordingState,
    durationMs: Long,
    error: String?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recPulseScale"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NfcSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (recordingState == RecordingState.RECORDING) RecordingRed else NfcBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Recording Timer Display
            Text(
                text = formatDuration(durationMs),
                color = if (recordingState == RecordingState.RECORDING) RecordingRed else TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Text(
                text = when (recordingState) {
                    RecordingState.RECORDING -> "● RECORDING LIVE VOICE (ORIGINAL QUALITY)"
                    RecordingState.PAUSED -> "❚❚ RECORDING PAUSED"
                    RecordingState.IDLE -> "READY TO RECORD"
                },
                color = when (recordingState) {
                    RecordingState.RECORDING -> RecordingRed
                    RecordingState.PAUSED -> NfcGold
                    RecordingState.IDLE -> TextSecondary
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = error, color = RecordingRed, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (recordingState) {
                    RecordingState.IDLE -> {
                        FilledIconButton(
                            onClick = onStart,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = RecordingRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("start_recording_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Start Recording",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    RecordingState.RECORDING -> {
                        IconButton(
                            onClick = onPause,
                            modifier = Modifier.size(48.dp).testTag("pause_recording_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = NfcGold,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        FilledIconButton(
                            onClick = onStop,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = RecordingRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(64.dp)
                                .scale(pulseScale)
                                .testTag("stop_recording_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Recording",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    RecordingState.PAUSED -> {
                        IconButton(
                            onClick = onResume,
                            modifier = Modifier.size(48.dp).testTag("resume_recording_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = MicLive,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        FilledIconButton(
                            onClick = onStop,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = RecordingRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("stop_recording_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Recording",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedRecordingItemRow(
    recording: VoiceRecording,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val dateFormatted = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(recording.dateRecorded))
    val sizeKb = recording.fileSizeBytes / 1024

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = NfcSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPlaying) VolumeDucked else NfcBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("saved_recording_row_${recording.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPlaying) VolumeDucked.copy(alpha = 0.2f) else NfcSurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = if (isPlaying) VolumeDucked else NfcGold,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.title,
                    color = if (isPlaying) VolumeDucked else TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatDuration(recording.durationMs)} • $dateFormatted • ${sizeKb}KB",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) VolumeDucked else NfcGold,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(NfcSurfaceDark)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = NfcGold) },
                        onClick = {
                            onRename()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Audio", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Share, null, tint = TextSecondary) },
                        onClick = {
                            onShare()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = RecordingRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = RecordingRed) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ImportedVoiceMp3Row(
    song: SongItem,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
    onDeletePermanently: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = NfcSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPlaying) VolumeDucked else NfcBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("voice_mp3_row_${song.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPlaying) VolumeDucked.copy(alpha = 0.2f) else NfcSurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = if (isPlaying) VolumeDucked else NfcCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = if (isPlaying) VolumeDucked else TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Imported Voice • ${formatDuration(song.durationMs)}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) VolumeDucked else NfcGold,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(NfcSurfaceDark)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = NfcGold) },
                        onClick = {
                            onRename()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Share, null, tint = TextSecondary) },
                        onClick = {
                            onShare()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove from NFC (Keep file)", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = TextSecondary) },
                        onClick = {
                            onRemoveFromLibrary()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Permanently", color = RecordingRed) },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = RecordingRed) },
                        onClick = {
                            onDeletePermanently()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

fun shareAudioFile(context: android.content.Context, filePath: String, title: String) {
    try {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share $title"))
    } catch (e: Exception) {
        // Fallback standard share
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(filePath)))
                putExtra(Intent.EXTRA_SUBJECT, title)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share $title"))
        } catch (_: Exception) {}
    }
}
