package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.model.CustomNoteShape
import com.example.model.CustomNoteType
import com.example.model.SongProject
import com.example.ui.ModStudioViewModel
import com.example.ui.components.CustomNoteEditDialog
import com.example.ui.components.CustomNoteShapeIcon
import com.example.ui.components.FnfArrowIcon
import com.example.ui.components.PillToggle
import com.example.ui.components.StudioCard
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LaneLeftColor
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.laneColor

@Composable
fun ChartTab(
    viewModel: ModStudioViewModel,
    song: SongProject?
) {
    if (song == null || song.sections.isEmpty()) return

    val isPlaying by viewModel.playback.isPlaying.collectAsStateWithLifecycle()
    val positionMs by viewModel.playback.positionMs.collectAsStateWithLifecycle()
    val project by viewModel.activeProject.collectAsStateWithLifecycle()
    val customNoteTypes = project?.customNoteTypes.orEmpty()
    val customNoteMap = remember(customNoteTypes) { customNoteTypes.associateBy { it.slug } }

    var selectedSectionIdx by remember(song.id) { mutableIntStateOf(0) }
    val safeSecIdx = selectedSectionIdx.coerceIn(0, song.sections.lastIndex)
    val section = song.sections[safeSecIdx]

    var snapDivisor by remember { mutableIntStateOf(1) } // 1 = 1/16, 2 = 1/8, 4 = 1/4
    var sustainSteps by remember { mutableIntStateOf(0) }
    var selectedNoteType by remember { mutableStateOf("") }
    var showCreateNoteDialog by remember { mutableStateOf(false) }

    val stepMs = AudioSynthEngine.stepDurationMs(song.bpm)
    val activeStepGlobal = if (isPlaying) (positionMs / stepMs).toInt() else -1
    val playSecIdx = if (activeStepGlobal >= 0) activeStepGlobal / 16 else -1
    val playStepInSec = if (activeStepGlobal >= 0) activeStepGlobal % 16 else -1

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky Chart Toolbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            val secStartMs = (safeSecIdx * 16 * stepMs).toLong()
                            viewModel.togglePlaySong(secStartMs)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) ErrorRed else StudioCyan,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("chart_play_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPlaying) "Stop" else "Oír Sec.", fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Vel: ${String.format("%.1f", song.speed)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = StudioCyan
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PillToggle(
                        label = if (section.mustHitSection) "Cám: BF" else "Cám: Rival",
                        selected = section.mustHitSection,
                        accent = StudioCyan,
                        onClick = { viewModel.updateSectionProperties(safeSecIdx, !section.mustHitSection, section.gfSection) }
                    )
                    FilledTonalButton(
                        onClick = { viewModel.clearChartSection(safeSecIdx) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StudioSurfaceElevated,
                            contentColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = "Limpiar sección", modifier = Modifier.size(15.dp))
                    }
                }
            }

            // Section Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                song.sections.forEachIndexed { idx, s ->
                    val isSel = idx == safeSecIdx
                    val isPlayingHere = idx == playSecIdx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isPlayingHere -> StudioMint.copy(alpha = 0.24f)
                                    isSel -> StudioCyan.copy(alpha = 0.18f)
                                    else -> StudioSurfaceElevated
                                }
                            )
                            .border(1.dp, if (isSel) StudioCyan else HairlineBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedSectionIdx = idx }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Sec ${idx + 1} (${s.notes.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSel) StudioCyan else TextSecondary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.addChartSection()
                            selectedSectionIdx = song.sections.size
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = "Agregar sección", tint = StudioCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Sección", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    }
                }
            }

            // Snap + Hold + Note Type Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Snap:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                listOf(1 to "1/16", 2 to "1/8", 4 to "1/4").forEach { (div, lbl) ->
                    PillToggle(label = lbl, selected = snapDivisor == div, onClick = { snapDivisor = div })
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hold:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                listOf(0 to "Tap", 2 to "Corto", 4 to "Largo").forEach { (sus, lbl) ->
                    PillToggle(label = lbl, selected = sustainSteps == sus, accent = StudioMint, onClick = { sustainSteps = sus })
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tipo:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                PillToggle(
                    label = "Normal",
                    selected = selectedNoteType == "",
                    accent = StudioCyan,
                    onClick = { selectedNoteType = "" }
                )
                PillToggle(
                    label = "Alt Anim",
                    selected = selectedNoteType == "Alt Animation",
                    accent = WarningAmber,
                    onClick = { selectedNoteType = "Alt Animation" }
                )
                customNoteTypes.forEach { custom ->
                    PillToggle(
                        label = custom.name,
                        selected = selectedNoteType == custom.slug,
                        accent = Color(custom.colorHex),
                        onClick = { selectedNoteType = custom.slug },
                        modifier = Modifier.testTag("chart_notetype_${custom.slug}")
                    )
                }
                PillToggle(
                    label = "+ Nota",
                    selected = false,
                    accent = StudioMint,
                    onClick = { showCreateNoteDialog = true },
                    modifier = Modifier.testTag("chart_add_custom_note")
                )
            }

            // Scroll Speed Slider
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Scroll", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Slider(
                    value = song.speed,
                    onValueChange = { sp ->
                        val rounded = ((sp * 10).toInt() / 10f).coerceIn(1.0f, 4.5f)
                        viewModel.updateActiveSong { it.copy(speed = rounded) }
                    },
                    valueRange = 1.0f..4.5f,
                    modifier = Modifier.weight(1f).height(26.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = StudioCyan,
                        activeTrackColor = StudioCyan,
                        inactiveTrackColor = StudioSurfaceHigh
                    )
                )
            }
        }

        // 8-Lane Column Header (4 Rival | 4 Boyfriend)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurfaceElevated)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Step", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.width(34.dp))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RIVAL (OPONENTE)", style = MaterialTheme.typography.labelSmall, color = LaneLeftColor)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (l in 0..3) FnfArrowIcon(lane = l, color = laneColor(l), filled = false, size = 18.dp)
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BOYFRIEND (JUGADOR)", style = MaterialTheme.typography.labelSmall, color = StudioCyan)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (l in 0..3) FnfArrowIcon(lane = l, color = laneColor(l), filled = false, size = 18.dp)
                }
            }
        }

        // 16-Step Interactive Grid
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(16) { step ->
                val enabledBySnap = step % snapDivisor == 0
                val isBeatLine = step % 4 == 0
                val isPlayhead = playSecIdx == safeSecIdx && playStepInSec == step
                val stepNotes = section.notes.filter { it.stepInSection == step }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isPlayhead -> StudioCyan.copy(alpha = 0.20f)
                                isBeatLine -> StudioSurfaceElevated
                                else -> StudioSurface.copy(alpha = 0.65f)
                            }
                        )
                        .border(
                            1.dp,
                            if (isPlayhead) StudioCyan else if (isBeatLine) HairlineBorder else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = step.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isBeatLine) TextPrimary else TextMuted,
                        modifier = Modifier.width(28.dp)
                    )

                    // Lanes 0..3 (Opponent)
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (lane in 0..3) {
                            val note = stepNotes.find { it.lane == lane }
                            val custom = note?.noteType?.let { customNoteMap[it] }
                            ChartCell(
                                lane = lane,
                                hasNote = note != null,
                                sustainSteps = note?.sustainSteps ?: 0,
                                customType = custom,
                                isHurt = note?.noteType == "Hurt Note",
                                enabled = enabledBySnap,
                                onClick = {
                                    if (enabledBySnap || note != null) {
                                        viewModel.toggleChartNote(safeSecIdx, step, lane, sustainSteps, selectedNoteType)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Lanes 4..7 (Player BF)
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (lane in 4..7) {
                            val note = stepNotes.find { it.lane == lane }
                            val custom = note?.noteType?.let { customNoteMap[it] }
                            ChartCell(
                                lane = lane,
                                hasNote = note != null,
                                sustainSteps = note?.sustainSteps ?: 0,
                                customType = custom,
                                isHurt = note?.noteType == "Hurt Note",
                                enabled = enabledBySnap,
                                onClick = {
                                    if (enabledBySnap || note != null) {
                                        viewModel.toggleChartNote(safeSecIdx, step, lane, sustainSteps, selectedNoteType)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                StudioCard {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tip de Chart", style = MaterialTheme.typography.labelLarge, color = StudioCyan)
                        Text(
                            "Toca cualquier celda para colocar o quitar flechas. Elige un \"Tipo\" o crea una \"+ Nota\" nueva (minas, curación, parry) para pintarla directo en el grid.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }

    if (showCreateNoteDialog) {
        CustomNoteEditDialog(
            initial = null,
            onDismiss = { showCreateNoteDialog = false },
            onSave = { name, shape, colorHex, ignoreNote, noAnim, healthDelta, missPenalty, effect, message ->
                val created = viewModel.addCustomNoteType(
                    name = name,
                    shape = shape,
                    colorHex = colorHex,
                    ignoreNote = ignoreNote,
                    noAnimation = noAnim,
                    healthDelta = healthDelta,
                    missPenalty = missPenalty,
                    hitEffect = effect,
                    hitMessage = message
                )
                selectedNoteType = created.slug
                showCreateNoteDialog = false
            }
        )
    }
}

@Composable
private fun ChartCell(
    lane: Int,
    hasNote: Boolean,
    sustainSteps: Int,
    customType: CustomNoteType?,
    isHurt: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val col = when {
        customType != null -> Color(customType.colorHex)
        isHurt -> ErrorRed
        else -> laneColor(lane)
    }
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    hasNote -> col.copy(alpha = 0.22f)
                    enabled -> StudioSurfaceHigh.copy(alpha = 0.55f)
                    else -> Color.Transparent
                }
            )
            .border(
                1.dp,
                if (hasNote) col else HairlineBorder.copy(alpha = 0.5f),
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .testTag("chart_cell_$lane"),
        contentAlignment = Alignment.Center
    ) {
        if (hasNote) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (customType != null && customType.shape != CustomNoteShape.ARROW) {
                    CustomNoteShapeIcon(shape = customType.shape, color = col, lane = lane, size = 20.dp)
                } else {
                    FnfArrowIcon(lane = lane, color = col, filled = true, size = 20.dp)
                }
                if (sustainSteps > 0) {
                    Text(
                        text = "+$sustainSteps",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
