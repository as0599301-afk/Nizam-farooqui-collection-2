package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.NfcViewModel

@Composable
fun YouTubeScreen(viewModel: NfcViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("YouTube")
        Spacer(modifier = Modifier.height(12.dp))
        Text("YouTube Music Search")
    }
}
