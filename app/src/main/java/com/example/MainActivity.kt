package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.NfcScreen
import com.example.ui.NfcViewModel
import com.example.ui.components.NfcMiniPlayer
import com.example.ui.components.NfcTopBar
import com.example.ui.screens.AppSettingsScreen
import com.example.ui.screens.DuckingSettingsScreen
import com.example.ui.screens.LiveMicScreen
import com.example.ui.screens.LocalLibraryScreen
import com.example.ui.screens.MusicPlayerScreen
import com.example.ui.screens.OnlineSearchScreen
import com.example.ui.screens.VoiceEffectsScreen
import com.example.ui.screens.VoiceRecordingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NfcCyan
import com.example.ui.theme.NfcGold
import com.example.ui.theme.NfcMidnight
import com.example.ui.theme.NfcSurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextTertiary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                NfcMainApp()
            }
        }
    }
}

data class NavItem(
    val screen: NfcScreen,
    val icon: ImageVector,
    val label: String
)

@Composable
fun NfcMainApp(
    viewModel: NfcViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isDucked by viewModel.isDucked.collectAsState()
    val duckingSettings by viewModel.duckingSettings.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Handle Android back button
    BackHandler(enabled = currentScreen != NfcScreen.MUSIC_PLAYER) {
        viewModel.navigateTo(NfcScreen.MUSIC_PLAYER)
    }

    val navItems = listOf(
        NavItem(NfcScreen.MUSIC_PLAYER, Icons.Default.PlayCircleFilled, "Player"),
        NavItem(NfcScreen.ONLINE_SEARCH, Icons.Default.Search, "Search"),
        NavItem(NfcScreen.LOCAL_LIBRARY, Icons.Default.LibraryMusic, "Library"),
        NavItem(NfcScreen.LIVE_MIC, Icons.Default.Mic, "Mic"),
        NavItem(NfcScreen.VOICE_EFFECTS, Icons.Default.AutoAwesome, "Effects"),
        NavItem(NfcScreen.VOICE_RECORDINGS, Icons.Default.RecordVoiceOver, "Voices"),
        NavItem(NfcScreen.DUCKING_SETTINGS, Icons.Default.GraphicEq, "Ducking"),
        NavItem(NfcScreen.APP_SETTINGS, Icons.Default.Settings, "Settings")
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(NfcMidnight)
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            NfcTopBar(
                title = currentScreen.title,
                onOpenDuckingSettings = { viewModel.navigateTo(NfcScreen.DUCKING_SETTINGS) },
                onOpenAppSettings = { viewModel.navigateTo(NfcScreen.APP_SETTINGS) }
            )
        },
        bottomBar = {
            Column {
                // Persistent Floating Mini-Player when user navigates away from Music Player
                if (currentScreen != NfcScreen.MUSIC_PLAYER && currentSong != null) {
                    NfcMiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        isDucked = isDucked,
                        duckingPercent = duckingSettings?.duckingLevelPercent ?: 30,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.nextTrack() },
                        onClick = { viewModel.navigateTo(NfcScreen.MUSIC_PLAYER) }
                    )
                }

                // Bottom Navigation Bar with all 8 NFC sections
                NavigationBar(
                    containerColor = NfcSurfaceDark,
                    contentColor = NfcGold,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nfc_bottom_navigation_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        navItems.forEach { item ->
                            val selected = item.screen == currentScreen
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(item.screen) },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NfcGold,
                                    selectedTextColor = NfcGold,
                                    unselectedIconColor = TextTertiary,
                                    unselectedTextColor = TextTertiary,
                                    indicatorColor = Color(0x22FFB703)
                                ),
                                modifier = Modifier.testTag("nav_item_${item.screen.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NfcMidnight
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    NfcScreen.MUSIC_PLAYER -> MusicPlayerScreen(viewModel)
                    NfcScreen.ONLINE_SEARCH -> OnlineSearchScreen(viewModel)
                    NfcScreen.LOCAL_LIBRARY -> LocalLibraryScreen(viewModel)
                    NfcScreen.LIVE_MIC -> LiveMicScreen(viewModel)
                    NfcScreen.VOICE_EFFECTS -> VoiceEffectsScreen(viewModel)
                    NfcScreen.VOICE_RECORDINGS -> VoiceRecordingsScreen(viewModel)
                    NfcScreen.DUCKING_SETTINGS -> DuckingSettingsScreen(viewModel)
                    NfcScreen.APP_SETTINGS -> AppSettingsScreen(viewModel)
                }
            }
        }
    }
}
