package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DuckingSettings
import com.example.ui.NfcViewModel
import com.example.ui.components.VoiceDuckingIndicator
import com.example.ui.theme.NfcBorder
import com.example.ui.theme.NfcCyan
import com.example.ui.theme.NfcGold
import com.example.ui.theme.NfcMidnight
import com.example.ui.theme.NfcSurfaceCard
import com.example.ui.theme.NfcSurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VolumeDucked

@Composable
fun DuckingSettingsScreen(
    viewModel: NfcViewModel,
    modifier: Modifier = Modifier
) {
    val duckingSettings by viewModel.duckingSettings.collectAsState()
    val isDucked by viewModel.isDucked.collectAsState()
    val activeVoiceSources by viewModel.activeVoiceSources.collectAsState()
    val effectiveMusicVolume by viewModel.effectiveMusicVolume.collectAsState()
    val currentDuckingFactor by viewModel.currentDuckingFactor.collectAsState()

    val settings = duckingSettings ?: DuckingSettings()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NfcMidnight)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        VoiceDuckingIndicator(
            isDucked = isDucked,
            activeSources = activeVoiceSources,
            duckingPercent = settings.duckingLevelPercent,
            onClick = {}
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Real-time Audio Mixer Status Banner
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NfcSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, NfcBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isDucked) VolumeDucked else NfcGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Audio Mixer Engine Status",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = if (isDucked) "DUCKING ACTIVE" else "NORMAL PLAYBACK",
                        color = if (isDucked) VolumeDucked else NfcCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Effective Music Output Volume: ${(effectiveMusicVolume * 100).toInt()}% (Ducking Factor: ${(currentDuckingFactor * 100).toInt()}%)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Ducking ON / OFF Switch
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NfcSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, NfcBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Automatic Audio Ducking",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Smoothly lowers background music when Live Mic, Voice MP3, or Voice Recording is active",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Switch(
                    checked = settings.isDuckingEnabled,
                    onCheckedChange = { viewModel.updateDuckingEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = NfcGold,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = NfcSurfaceCard
                    ),
                    modifier = Modifier.testTag("ducking_enable_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Ducking Level (0% to 100%)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NfcSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, NfcBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ducked Music Volume Level",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${settings.duckingLevelPercent}%",
                        color = NfcGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "The target volume of background music when voice is speaking (Default: 30%)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Slider(
                    value = settings.duckingLevelPercent.toFloat(),
                    onValueChange = { viewModel.updateDuckingLevel(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = NfcGold,
                        activeTrackColor = NfcGold,
                        inactiveTrackColor = NfcSurfaceCard
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("ducking_level_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Fade Down Speed (ms)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NfcSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, NfcBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Fade Down Speed (Attack)",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${settings.fadeDownSpeedMs} ms",
                        color = NfcCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "How quickly music fades down when voice starts speaking (50ms - 2000ms)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Slider(
                    value = settings.fadeDownSpeedMs.toFloat(),
                    onValueChange = { viewModel.updateFadeDownSpeed(it.toLong()) },
                    valueRange = 50f..2000f,
                    colors = SliderDefaults.colors(
                        thumbColor = NfcCyan,
                        activeTrackColor = NfcCyan,
                        inactiveTrackColor = NfcSurfaceCard
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("fade_down_speed_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Fade Up Speed (ms)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NfcSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, NfcBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Fade Up Speed (Release)",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${settings.fadeUpSpeedMs} ms",
                        color = NfcCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "How smoothly music returns to full volume after all voice sources stop (50ms - 2500ms)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Slider(
                    value = settings.fadeUpSpeedMs.toFloat(),
                    onValueChange = { viewModel.updateFadeUpSpeed(it.toLong()) },
                    valueRange = 50f..2500f,
                    colors = SliderDefaults.colors(
                        thumbColor = NfcCyan,
                        activeTrackColor = NfcCyan,
                        inactiveTrackColor = NfcSurfaceCard
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("fade_up_speed_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Background Music Base Volume (0% - 100%)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NfcSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, NfcBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, null, tint = NfcGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Background Music Master Volume",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "${settings.musicVolumePercent}%",
                        color = NfcGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Slider(
                    value = settings.musicVolumePercent.toFloat(),
                    onValueChange = { viewModel.updateMusicVolume(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = NfcGold,
                        activeTrackColor = NfcGold,
                        inactiveTrackColor = NfcSurfaceCard
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("music_master_volume_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 6. Voice / Mic Master Volume (0% - 100%)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NfcSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, NfcBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, null, tint = NfcCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Voice / Mic Master Volume",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "${settings.voiceMicVolumePercent}%",
                        color = NfcCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Slider(
                    value = settings.voiceMicVolumePercent.toFloat(),
                    onValueChange = { viewModel.setMicVolume(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = NfcCyan,
                        activeTrackColor = NfcCyan,
                        inactiveTrackColor = NfcSurfaceCard
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("voice_master_volume_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7. Preview / Test Ducking Button
        Button(
            onClick = { viewModel.triggerDuckingTestPreview() },
            colors = ButtonDefaults.buttonColors(
                containerColor = VolumeDucked,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("preview_ducking_test_button")
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Preview / Test Ducking (3.5s Simulation)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}
