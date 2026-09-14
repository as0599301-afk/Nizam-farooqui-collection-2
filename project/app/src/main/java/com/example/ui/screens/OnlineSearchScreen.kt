package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SearchCategory
import com.example.data.model.SongItem
import com.example.ui.NfcViewModel
import com.example.ui.components.VoiceDuckingIndicator
import com.example.ui.components.formatDuration
import com.example.ui.theme.NfcBorder
import com.example.ui.theme.NfcCyan
import com.example.ui.theme.NfcGold
import com.example.ui.theme.NfcGreen
import com.example.ui.theme.NfcMidnight
import com.example.ui.theme.NfcSurfaceCard
import com.example.ui.theme.NfcSurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun OnlineSearchScreen(
    viewModel: NfcViewModel,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val category by viewModel.searchCategory.collectAsState()
    val language by viewModel.searchLanguage.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val activeProvider by viewModel.activeMusicProvider.collectAsState()

    val isDucked by viewModel.isDucked.collectAsState()
    val activeVoiceSources by viewModel.activeVoiceSources.collectAsState()
    val duckingSettings by viewModel.duckingSettings.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current

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

        // Search Input Bar
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("Search songs, movies, artists...", color = TextTertiary, fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = NfcGold
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                viewModel.executeSearch()
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = NfcSurfaceDark,
                unfocusedContainerColor = NfcSurfaceDark,
                focusedBorderColor = NfcGold,
                unfocusedBorderColor = NfcBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("online_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips (Song, Movie, Artist)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchCategory.entries.forEach { cat ->
                val selected = cat == category
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.onSearchCategoryChanged(cat) },
                    label = {
                        Text(
                            text = cat.displayName,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NfcGold,
                        selectedLabelColor = Color.Black,
                        containerColor = NfcSurfaceDark,
                        labelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("filter_chip_${cat.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Language Filter Chips (Hindi, Bhojpuri, Marathi, etc.)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            activeProvider.supportedLanguages.forEach { lang ->
                val selected = lang == language
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.onSearchLanguageChanged(lang) },
                    label = {
                        Text(
                            text = lang,
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NfcCyan,
                        selectedLabelColor = Color.Black,
                        containerColor = NfcSurfaceDark,
                        labelColor = TextTertiary
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active Provider Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = null,
                tint = NfcGreen,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Provider: ${activeProvider.displayName}",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Results / Loading / Empty States
        when {
            isSearching -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NfcGold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Searching legal music catalogue...",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            searchError != null -> {
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
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = searchError ?: "",
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            results.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Search by song title, movie soundtrack, or artist",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Hindi • Bhojpuri • Marathi • Punjabi • All Indian Languages",
                            color = NfcCyan,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { it.id }) { song ->
                        SearchResultItem(
                            song = song,
                            onPlay = { viewModel.playSong(song) },
                            onAddToQueue = { viewModel.addToQueue(song) }
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

@Composable
fun SearchResultItem(
    song: SongItem,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NfcSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, NfcBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("search_result_item_${song.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NfcSurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                if (song.artworkUrl != null) {
                    AsyncImage(
                        model = song.artworkUrl,
                        contentDescription = "Artwork",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = NfcGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (song.album.isNotBlank()) {
                    Text(
                        text = "Movie / Album: ${song.album}",
                        color = NfcCyan,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = formatDuration(song.durationMs),
                color = TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            // Add to Queue button
            IconButton(
                onClick = onAddToQueue,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = "Add to Queue",
                    tint = NfcCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Play button
            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = NfcGold,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
