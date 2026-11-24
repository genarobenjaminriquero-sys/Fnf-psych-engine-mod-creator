package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.AudioSynthEngine
import com.example.model.AudioSourceMode
import com.example.model.SongProject
import com.example.ui.ModStudioViewModel
import com.example.ui.components.PillToggle
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.components.WaveformCanvas
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LaneDownColor
import com.example.ui.theme.LaneLeftColor
import com.example.ui.theme.LaneRightColor
import com.example.ui.theme.LaneUpColor
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun MusicTab(
    viewModel: ModStudioViewModel,
    song: SongProject?
) {
    if (song == null) return

    val isPlaying by viewModel.playback.isPlaying.collectAsStateWithLifecycle()
    val positionMs by viewModel.playback.positionMs.collectAsStateWithLifecycle()

    var selectedBar by remember(song.id) { mutableIntStateOf(0) }
    var selectedTrack by remember(song.id) { mutableIntStateOf(0) } // 0=Drums, 1=Bass, 2=Lead, 3=BF, 4=Rival
    val tapTimes = remember { mutableStateListOf<Long>() }

    val instPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importAudioFile(it, isVoices = false) }
    }
    val voicesPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importAudioFile(it, isVoices = true) }
    }

    val totalDuration = AudioSynthEngine.songTotalDurationMs(song).coerceAtLeast(1L)
    val playProgress = (positionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    val activeStepGlobal = if (isPlaying) {
        (positionMs / AudioSynthEngine.stepDurationMs(song.bpm)).toInt()
    } else -1

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Source Mode Switcher
        item {
            StudioCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "Fuente de Audio",
                        subtitle = "Crea la canción aquí con instrumentos o sube tus audios del celular"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PillToggle(
                            label = "Compositor Nativo",
                            selected = song.audioSourceMode == AudioSourceMode.SYNTH_COMPOSER,
                            accent = StudioCyan,
                            onClick = {
                                viewModel.stopPlayback()
                                viewModel.updateActiveSong { it.copy(audioSourceMode = AudioSourceMode.SYNTH_COMPOSER) }
                            },
                            modifier = Modifier.weight(1f).testTag("mode_composer_btn")
                        )
                        PillToggle(
                            label = "Audio del Celular",
                            selected = song.audioSourceMode == AudioSourceMode.IMPORTED_AUDIO,
                            accent = StudioMint,
                            onClick = {
                                viewModel.stopPlayback()
                                viewModel.updateActiveSong { it.copy(audioSourceMode = AudioSourceMode.IMPORTED_AUDIO) }
                            },
                            modifier = Modifier.weight(1f).testTag("mode_import_btn")
                        )
                    }
                }
            }
        }

        // Transport & BPM Card
        item {
            StudioCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Tempo (BPM): ${song.bpm.toInt()}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                text = "Scroll Speed: ${String.format("%.1f", song.speed)} • ${totalDuration / 1000}s",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    if (tapTimes.isNotEmpty() && now - tapTimes.last() > 2500L) {
                                        tapTimes.clear()
                                    }
                                    tapTimes.add(now)
                                    if (tapTimes.size >= 3) {
                                        val intervals = tapTimes.zipWithNext { a, b -> b - a }
                                        val avgMs = intervals.average()
                                        if (avgMs > 200.0) {
                                            val detected = (60_000.0 / avgMs).toFloat().coerceIn(60f, 240f)
                                            viewModel.updateActiveSong { it.copy(bpm = detected) }
                                        }
                                        if (tapTimes.size > 8) tapTimes.removeAt(0)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = StudioSurfaceElevated,
                                    contentColor = StudioCyan
                                )
                            ) {
                                Icon(Icons.Filled.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tap BPM")
                            }

                            Button(
                                onClick = { viewModel.togglePlaySong(0L) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) LaneRightColor else StudioCyan,
                                    contentColor = ObsidianBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("music_play_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Detener" else "Reproducir"
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPlaying) "Stop" else "Oír", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Slider(
                        value = song.bpm,
                        onValueChange = { newBpm ->
                            viewModel.updateActiveSong { it.copy(bpm = newBpm.toInt().toFloat()) }
                        },
                        valueRange = 60f..220f,
                        colors = SliderDefaults.colors(
                            thumbColor = StudioCyan,
                            activeTrackColor = StudioCyan,
                            inactiveTrackColor = StudioSurfaceHigh
                        )
                    )
                }
            }
        }

        if (song.audioSourceMode == AudioSourceMode.IMPORTED_AUDIO) {
            // Phone Audio Importer UI
            item {
                StudioCard {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(
                            title = "Archivos de Audio del Celular",
                            subtitle = "Acepta .ogg, .mp3, .wav o .m4a (se convierten automáticamente a OGG sin errores)"
                        )

                        WaveformCanvas(
                            peaks = song.waveformPeaks,
                            progress = playProgress,
                            accentColor = StudioMint
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { instPicker.launch(arrayOf("audio/*")) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioSurfaceElevated,
                                    contentColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                                    .testTag("pick_inst_button")
                            ) {
                                Icon(Icons.Filled.AudioFile, contentDescription = null, tint = StudioMint, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(song.importedInstName?.take(14) ?: "Subir Inst", maxLines = 1)
                            }

                            Button(
                                onClick = { voicesPicker.launch(arrayOf("audio/*")) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioSurfaceElevated,
                                    contentColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                                    .testTag("pick_voices_button")
                            ) {
                                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(song.importedVoicesName?.take(14) ?: "Subir Voices", maxLines = 1)
                            }
                        }

                        if (!song.importedInstPath.isNullOrBlank()) {
                            Text(
                                text = "Inst listo: ${song.importedInstName} • Voices: ${song.importedVoicesName ?: "Opcional (desactivado)"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = StudioMint
                            )
                        }
                    }
                }
            }
        } else {
            // Native Instrument Composer Studio
            item {
                val trackNames = listOf("Batería", "Bajo", "Lead", "Voz BF", "Voz Rival")
                val trackColors = listOf(WarningAmber, StudioIndigo, StudioCyan, LaneDownColor, LaneLeftColor)
                val comp = song.composerData
                val safeBar = selectedBar.coerceIn(0, (comp.barsCount - 1).coerceAtLeast(0))

                StudioCard {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(
                            title = "Secuenciador de Instrumentos",
                            subtitle = "Toca los cuadros para componer la instrumental y voces FNF",
                            trailing = {
                                FilledTonalButton(
                                    onClick = { viewModel.syncChartFromComposerVocals() },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = StudioCyan.copy(alpha = 0.16f),
                                        contentColor = StudioCyan
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("sync_chart_from_vocals")
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pasar a Chart")
                                }
                            }
                        )

                        // Bar Count & Current Bar Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Compás:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            for (b in 0 until comp.barsCount) {
                                val isCur = b == safeBar
                                val isPlayingBar = activeStepGlobal in (b * 16 until (b + 1) * 16)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isPlayingBar -> StudioMint.copy(alpha = 0.25f)
                                                isCur -> StudioCyan.copy(alpha = 0.20f)
                                                else -> StudioSurfaceElevated
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (isCur) StudioCyan else HairlineBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedBar = b }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${b + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isCur) StudioCyan else TextPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            PillToggle(
                                label = "+4 Compases",
                                selected = false,
                                onClick = { viewModel.setComposerBars(comp.barsCount + 4) }
                            )
                        }

                        // Instrument Track Tabs with Mute toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            trackNames.forEachIndexed { idx, label ->
                                val isSel = selectedTrack == idx
                                val muted = comp.trackMutes.getOrElse(idx) { false }
                                val col = trackColors[idx]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) col.copy(alpha = 0.18f) else StudioSurfaceElevated)
                                        .border(1.dp, if (isSel) col else HairlineBorder, RoundedCornerShape(10.dp))
                                        .clickable { selectedTrack = idx }
                                        .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (muted) TextMuted else if (isSel) col else TextPrimary
                                    )
                                    IconButton(
                                        onClick = { viewModel.toggleTrackMute(idx) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                            contentDescription = "Silenciar $label",
                                            tint = if (muted) TextMuted else col,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Step Grid for Selected Track & Bar
                        val baseStep = safeBar * 16
                        if (selectedTrack == 0) {
                            val drumNames = listOf("Kick", "Snare", "HiHat", "Clap")
                            val drumColors = listOf(LaneRightColor, WarningAmber, StudioCyan, LaneUpColor)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                drumNames.forEachIndexed { lane, dName ->
                                    val activeSteps = comp.drumGrid[lane].orEmpty().toSet()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = dName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = drumColors[lane],
                                            modifier = Modifier.width(50.dp)
                                        )
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            for (s in 0 until 16) {
                                                val globalStep = baseStep + s
                                                val on = globalStep in activeSteps
                                                val isBeat = s % 4 == 0
                                                val isPlayhead = globalStep == activeStepGlobal
                                                StepCell(
                                                    active = on,
                                                    isPlayhead = isPlayhead,
                                                    isBeatStart = isBeat,
                                                    activeColor = drumColors[lane],
                                                    onClick = { viewModel.toggleDrumStep(lane, globalStep) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Melodic / Vocal Piano Grid (8 clean minor pentatonic/funk notes)
                            val pitchLabels = listOf("C+", "Bb", "Ab", "G", "F", "Eb", "D", "C")
                            val baseOctave = when (selectedTrack) {
                                1 -> 36 // Bass C2
                                4 -> 48 // Rival C3
                                else -> 60 // Lead / BF C4
                            }
                            val pitchMidis = listOf(12, 10, 8, 7, 5, 3, 2, 0).map { baseOctave + it }
                            val noteMap = when (selectedTrack) {
                                1 -> comp.bassNotes
                                2 -> comp.leadNotes
                                3 -> comp.bfVocalNotes
                                else -> comp.dadVocalNotes
                            }
                            val accent = trackColors[selectedTrack]

                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                pitchLabels.forEachIndexed { rowIdx, pLabel ->
                                    val midi = pitchMidis[rowIdx]
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = pLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary,
                                            modifier = Modifier.width(34.dp)
                                        )
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            for (s in 0 until 16) {
                                                val globalStep = baseStep + s
                                                val on = noteMap[globalStep] == midi
                                                StepCell(
                                                    active = on,
                                                    isPlayhead = globalStep == activeStepGlobal,
                                                    isBeatStart = s % 4 == 0,
                                                    activeColor = accent,
                                                    onClick = {
                                                        viewModel.setMelodicStep(
                                                            selectedTrack,
                                                            globalStep,
                                                            if (on) null else midi
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCell(
    active: Boolean,
    isPlayhead: Boolean,
    isBeatStart: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val bg = when {
        active -> activeColor
        isPlayhead -> StudioCyan.copy(alpha = 0.28f)
        isBeatStart -> StudioSurfaceHigh
        else -> StudioSurfaceElevated
    }
    val border = when {
        isPlayhead -> Color.White
        active -> activeColor
        else -> HairlineBorder
    }
    Box(
        modifier = Modifier
            .size(width = 28.dp, height = 28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    )
}
