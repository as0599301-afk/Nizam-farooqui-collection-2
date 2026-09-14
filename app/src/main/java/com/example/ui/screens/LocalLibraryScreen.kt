package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SongItem
import com.example.ui.NfcViewModel
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.RenameDialog
import com.example.ui.components.VoiceDuckingIndicator
import com.example.ui.components.formatDuration
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

enum class LibrarySortOrder(val label: String) {
    DATE_ADDED("Recently Added"),
    TITLE("Song Title (A-Z)"),
    ARTIST("Artist Name"),
    DURATION("Duration")
}

@Composable
fun LocalLibraryScreen(
    viewModel: NfcViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.localMusicSongs.collectAsState()
    val isDucked by viewModel.isDucked.collectAsState()
    val activeVoiceSources by viewModel.activeVoiceSources.collectAsState()
    val duckingSettings by viewModel.duckingSettings.collectAsState()

    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(LibrarySortOrder.DATE_ADDED) }
    var showSortMenu by remember { mutableStateOf(false) }

    var songToRename by remember { mutableStateOf<SongItem?>(null) }
    var songToDelete by remember { mutableStateOf<SongItem?>(null) }
    var isPermanentDelete by remember { mutableStateOf(false) }

    // File picker launcher for importing MP3s
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            viewModel.importAudioFile(uri, isVoiceMp3 = false)
        }
    }

    val filteredSongs = songs
        .filter {
            if (searchQuery.isBlank()) true
            else it.title.contains(searchQuery, ignoreCase = true) ||
                 it.artist.contains(searchQuery, ignoreCase = true)
        }
        .sortedWith { a, b ->
            when (sortOrder) {
                LibrarySortOrder.DATE_ADDED -> b.dateAdded.compareTo(a.dateAdded)
                LibrarySortOrder.TITLE -> a.title.compareTo(b.title, ignoreCase = true)
                LibrarySortOrder.ARTIST -> a.artist.compareTo(b.artist, ignoreCase = true)
                LibrarySortOrder.DURATION -> b.durationMs.compareTo(a.durationMs)
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
            duckingPercent = duckingSettings?.duckingLevelPercent ?: 30,
            onClick = {}
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Import Button Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    filePickerLauncher.launch(arrayOf("audio/*", "application/ogg", "audio/mpeg"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = NfcGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("import_mp3_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import MP3 from Phone", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.testTag("sort_library_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort Library",
                        tint = NfcCyan
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(NfcSurfaceDark)
                ) {
                    LibrarySortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    order.label,
                                    color = if (order == sortOrder) NfcGold else TextPrimary,
                                    fontWeight = if (order == sortOrder) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                sortOrder = order
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Filter TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter ${songs.size} local songs...", color = TextTertiary, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = NfcSurfaceDark,
                unfocusedContainerColor = NfcSurfaceDark,
                focusedBorderColor = NfcCyan,
                unfocusedBorderColor = NfcBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("library_filter_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredSongs.isEmpty()) {
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
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (songs.isEmpty()) "No local MP3 files imported yet." else "No songs matching \"$searchQuery\"",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap 'Import MP3 from Phone' above to browse and select music files from your device storage.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredSongs, key = { it.id }) { song ->
                    LocalSongItemRow(
                        song = song,
                        onPlay = { viewModel.playSong(song) },
                        onAddToQueue = { viewModel.addToQueue(song) },
                        onRename = { songToRename = song },
                        onRemoveFromLibrary = {
                            songToDelete = song
                            isPermanentDelete = false
                        },
                        onDeletePermanently = {
                            songToDelete = song
                            isPermanentDelete = true
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

    // Rename Dialog
    songToRename?.let { song ->
        RenameDialog(
            currentTitle = song.title,
            onConfirm = { newTitle ->
                viewModel.renameSong(song.id, newTitle)
                songToRename = null
            },
            onDismiss = { songToRename = null }
        )
    }

    // Delete / Remove Dialog
    songToDelete?.let { song ->
        DeleteConfirmDialog(
            itemTitle = song.title,
            isPermanentDelete = isPermanentDelete,
            onConfirm = {
                if (isPermanentDelete) {
                    viewModel.deleteSongPermanently(song)
                } else {
                    viewModel.removeSongFromLibrary(song.id)
                }
                songToDelete = null
            },
            onDismiss = { songToDelete = null }
        )
    }
}

@Composable
fun LocalSongItemRow(
    song: SongItem,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
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
        border = androidx.compose.foundation.BorderStroke(1.dp, NfcBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("local_song_row_${song.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NfcSurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = NfcGold,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${formatDuration(song.durationMs)}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = NfcGold,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(34.dp).testTag("song_menu_button_${song.id}")
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
                        text = { Text("Add to Queue", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, tint = NfcCyan) },
                        onClick = {
                            onAddToQueue()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = NfcGold) },
                        onClick = {
                            onRename()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Track", color = TextPrimary) },
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
