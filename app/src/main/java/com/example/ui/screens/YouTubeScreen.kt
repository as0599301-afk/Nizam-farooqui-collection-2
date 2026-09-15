package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.SongItem
import com.example.ui.NfcViewModel
import com.example.ui.components.YouTubePlayerView

@Composable
fun YouTubeScreen(viewModel: NfcViewModel) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val error by viewModel.searchError.collectAsState()

    var selectedVideoId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("YouTube")

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier.weight(1f),
                label = { Text("Search song") },
                singleLine = true
            )

            Button(
                onClick = viewModel::executeSearch,
                enabled = query.isNotBlank() && !isSearching
            ) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isSearching) {
            CircularProgressIndicator()
        }

        error?.let { Text(it) }

        selectedVideoId?.let { videoId ->
            Text("Now Playing")
            YouTubePlayerView(
                videoId = videoId,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results) { song ->
                YouTubeResultItem(
                    song = song,
                    onClick = {
                        selectedVideoId = song.uriString.removePrefix("youtube:")
                    }
                )
            }
        }
    }
}

@Composable
private fun YouTubeResultItem(
    song: SongItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(song.title)
        Text(song.artist)
    }
}
