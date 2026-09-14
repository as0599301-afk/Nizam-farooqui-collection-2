package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NfcScreen
import com.example.ui.NfcViewModel
import com.example.ui.components.VoiceDuckingIndicator
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

@Composable
fun LiveMicScreen(
    viewModel: NfcViewModel,
    modifier: Modifier = Modifier
) {
    val isMicActive by viewModel.isMicActive.collectAsState()
    val audioMeterLevel by viewModel.audioMeterLevel.collectAsState()
    val currentEffect by viewModel.currentVoiceEffect.collectAsState()
    val micVolume by viewModel.micVolume.collectAsState()
    val micError by viewModel.micError.collectAsState()

    val isDucked by viewModel.isDucked.collectAsState()
    val activeVoiceSources by viewModel.activeVoiceSources.collectAsState()
    val duckingSettings by viewModel.duckingSettings.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleMic()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NfcMidnight)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VoiceDuckingIndicator(
            isDucked = isDucked,
            activeSources = activeVoiceSources,
            duckingPercent = duckingSettings?.duckingLevelPercent ?: 30,
            onClick = { viewModel.navigateTo(NfcScreen.DUCKING_SETTINGS) }
        )

        Spacer(modifier = Modifier.weight(0.3f))

        // Center Live Mic Button with Pulsating Outer Glow when Active
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )

        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isMicActive) {
                // Pulsating ring
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MicLive.copy(alpha = 0.15f))
                        .border(2.dp, MicLive.copy(alpha = 0.4f), CircleShape)
                )
            }

            FilledIconButton(
                onClick = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isMicActive) MicLive else NfcSurfaceCard,
                    contentColor = if (isMicActive) Color.Black else TextPrimary
                ),
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .border(
                        3.dp,
                        if (isMicActive) MicLive else NfcBorder,
                        CircleShape
                    )
                    .testTag("live_mic_toggle_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isMicActive) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Microphone Toggle",
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isMicActive) "LIVE ON" else "TAP TO START",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isMicActive) "Microphone is LIVE" else "Microphone is Muted",
            color = if (isMicActive) MicLive else TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (isMicActive) {
                "Low latency real audio passing through • Music is ducked"
            } else {
                "Tap button to transmit live voice with background music"
            },
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (micError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = micError ?: "",
                color = RecordingRed,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(0.3f))

        // Live Audio VU Level Meter
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = NfcSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, NfcBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isMicActive) MicLive else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Audio Input Meter",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "${(audioMeterLevel * 100).toInt()}%",
                        color = if (audioMeterLevel > 0.8f) RecordingRed else if (isMicActive) MicLive else TextTertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { if (isMicActive) audioMeterLevel else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (audioMeterLevel > 0.8f) RecordingRed else if (audioMeterLevel > 0.5f) NfcGold else MicLive,
                    trackColor = NfcSurfaceCard
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mic Volume Slider
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = NfcSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, NfcBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = NfcCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Microphone Gain / Volume",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "$micVolume%",
                        color = NfcCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Slider(
                    value = micVolume.toFloat(),
                    onValueChange = { viewModel.setMicVolume(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = NfcCyan,
                        activeTrackColor = NfcCyan,
                        inactiveTrackColor = NfcSurfaceCard
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("live_mic_volume_slider")
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.3f))

        // Current Voice Effect Banner & Shortcut to Effects Screen
        Surface(
            color = NfcSurfaceDark,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NfcBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NfcGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Active Effect: ${currentEffect.displayName}",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = currentEffect.description,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                TextButton(
                    onClick = { viewModel.navigateTo(NfcScreen.VOICE_EFFECTS) },
                    modifier = Modifier.testTag("nav_to_voice_effects_button")
                ) {
                    Text("Change →", color = NfcGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
