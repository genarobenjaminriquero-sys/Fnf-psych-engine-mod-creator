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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.ModProject
import com.example.model.ScratchActionType
import com.example.model.ScratchBlock
import com.example.model.ScratchCategory
import com.example.model.ScratchLuaCompiler
import com.example.model.ScratchTriggerType
import com.example.model.SongProject
import com.example.ui.ModStudioViewModel
import com.example.ui.components.PillToggle
import com.example.ui.components.StudioCard
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ScratchArrowsColor
import com.example.ui.theme.ScratchCameraColor
import com.example.ui.theme.ScratchCharacterColor
import com.example.ui.theme.ScratchHudColor
import com.example.ui.theme.ScratchTriggerColor
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EventsTab(
    viewModel: ModStudioViewModel,
    project: ModProject,
    song: SongProject?
) {
    if (song == null) return

    var showPaletteDialog by remember { mutableStateOf(false) }
    var showLuaPreview by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Bloques de Eventos", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "${song.scratchBlocks.size} bloque(s) • 10 disparadores • 28 acciones",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { showLuaPreview = !showLuaPreview },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (showLuaPreview) StudioCyan.copy(alpha = 0.2f) else StudioSurfaceElevated,
                        contentColor = if (showLuaPreview) StudioCyan else TextPrimary
                    ),
                    modifier = Modifier.testTag("toggle_lua_preview")
                ) {
                    Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lua")
                }

                Button(
                    onClick = { showPaletteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = ObsidianBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_scratch_block_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bloque", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showLuaPreview) {
            val luaCode = remember(song.scratchBlocks, song.name) {
                ScratchLuaCompiler.compileToLua(song.scratchBlocks, song.name)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    StudioCard {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("script.lua generado automáticamente", style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                            Text(
                                text = luaCode,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (song.scratchBlocks.isEmpty()) {
                    item {
                        StudioCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = ScratchTriggerColor, modifier = Modifier.size(36.dp))
                                Text("Sin eventos todavía", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    "Arma eventos visuales como en Scratch: mueve o gira flechas, cambia de personaje mid-canción, haz zoom o sacude la cámara.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Button(
                                    onClick = { showPaletteDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ScratchTriggerColor, contentColor = ObsidianBg)
                                ) {
                                    Text("Agregar Primer Bloque", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(song.scratchBlocks, key = { it.id }) { block ->
                        ScratchBlockCard(
                            block = block,
                            project = project,
                            onUpdate = { updated -> viewModel.updateScratchBlock(block.id) { updated } },
                            onMoveUp = { viewModel.moveScratchBlock(block.id, -1) },
                            onMoveDown = { viewModel.moveScratchBlock(block.id, 1) },
                            onDelete = { viewModel.deleteScratchBlock(block.id) }
                        )
                    }
                }
            }
        }
    }

    if (showPaletteDialog) {
        ScratchPaletteDialog(
            onDismiss = { showPaletteDialog = false },
            onSelect = { trigger, action ->
                viewModel.addScratchBlock(trigger, action)
                showPaletteDialog = false
            }
        )
    }
}

@Composable
private fun ScratchBlockCard(
    block: ScratchBlock,
    project: ModProject,
    onUpdate: (ScratchBlock) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val actionColor = categoryColor(block.actionType.category)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StudioSurface)
            .border(1.dp, if (block.enabled) actionColor.copy(alpha = 0.55f) else HairlineBorder, RoundedCornerShape(14.dp))
    ) {
        // Hat Trigger Block (Scratch Yellow/Amber)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ScratchTriggerColor.copy(alpha = 0.16f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = ScratchTriggerColor, modifier = Modifier.size(18.dp))
                    Text("CUANDO", style = MaterialTheme.typography.labelSmall, color = ScratchTriggerColor, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Subir", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Bajar", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    Switch(
                        checked = block.enabled,
                        onCheckedChange = { onUpdate(block.copy(enabled = it)) }
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar bloque", tint = ErrorRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Trigger Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScratchTriggerType.entries.forEach { trig ->
                    PillToggle(
                        label = trig.label,
                        selected = block.triggerType == trig,
                        accent = ScratchTriggerColor,
                        onClick = { onUpdate(block.copy(triggerType = trig)) }
                    )
                }
            }

            Text(
                text = block.triggerType.description,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            if (block.triggerType == ScratchTriggerType.ON_BEAT ||
                block.triggerType == ScratchTriggerType.EVERY_N_BEATS ||
                block.triggerType == ScratchTriggerType.ON_STEP ||
                block.triggerType == ScratchTriggerType.EVERY_N_STEPS
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val unitLabel = when (block.triggerType) {
                        ScratchTriggerType.ON_STEP -> "Step #"
                        ScratchTriggerType.EVERY_N_STEPS -> "Cada N Steps"
                        ScratchTriggerType.EVERY_N_BEATS -> "Cada N Beats"
                        else -> "Beat #"
                    }
                    Text(unitLabel, style = MaterialTheme.typography.labelMedium, color = ScratchTriggerColor)
                    PillToggle("-4", false, ScratchTriggerColor, { onUpdate(block.copy(triggerValue = (block.triggerValue - 4).coerceAtLeast(1))) })
                    PillToggle("-1", false, ScratchTriggerColor, { onUpdate(block.copy(triggerValue = (block.triggerValue - 1).coerceAtLeast(1))) })
                    Text(
                        text = "${block.triggerValue}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    PillToggle("+1", false, ScratchTriggerColor, { onUpdate(block.copy(triggerValue = block.triggerValue + 1)) })
                    PillToggle("+4", false, ScratchTriggerColor, { onUpdate(block.copy(triggerValue = block.triggerValue + 4)) })
                }
            }
        }

        // Snapped Action Block Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(actionColor.copy(alpha = 0.12f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(actionColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = block.actionType.category.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianBg,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(block.actionType.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            }

            // Smart Parameter Controls depending on Action Type
            when (block.actionType) {
                ScratchActionType.CHANGE_CHARACTER -> {
                    Text("Slot de personaje:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("dad" to "Rival", "boyfriend" to "BF", "gf" to "GF").forEach { (slot, lbl) ->
                            PillToggle(lbl, block.param1 == slot, actionColor, { onUpdate(block.copy(param1 = slot)) })
                        }
                    }
                    Text("Cambiar a:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        project.characters.forEach { ch ->
                            PillToggle(ch.displayName, block.param2 == ch.id, actionColor, { onUpdate(block.copy(param2 = ch.id)) })
                        }
                    }
                }

                ScratchActionType.MOVE_ARROWS,
                ScratchActionType.SPIN_ARROWS,
                ScratchActionType.FADE_ARROWS,
                ScratchActionType.BUMP_ARROWS -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("all" to "Todas", "player" to "Jugador", "opponent" to "Rival").forEach { (grp, lbl) ->
                            PillToggle(lbl, block.param1 == grp, actionColor, { onUpdate(block.copy(param1 = grp)) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = block.param2,
                            onValueChange = { onUpdate(block.copy(param2 = it)) },
                            label = { Text(block.actionType.param2Label) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = block.param3,
                            onValueChange = { onUpdate(block.copy(param3 = it)) },
                            label = { Text(block.actionType.param3Label) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ScratchActionType.PLAY_ANIM -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("boyfriend" to "BF", "dad" to "Rival", "gf" to "GF").forEach { (t, l) ->
                            PillToggle(l, block.param1 == t, actionColor, { onUpdate(block.copy(param1 = t)) })
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("hey", "idle", "singLEFT", "singDOWN", "singUP", "singRIGHT").forEach { anim ->
                            PillToggle(anim, block.param2 == anim, actionColor, { onUpdate(block.copy(param2 = anim)) })
                        }
                    }
                }

                ScratchActionType.TOGGLE_CHAR_VISIBLE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("dad" to "Rival", "boyfriend" to "BF", "gf" to "GF").forEach { (t, l) ->
                            PillToggle(l, block.param1 == t, actionColor, { onUpdate(block.copy(param1 = t)) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillToggle("Mostrar", block.param2 == "true", actionColor, { onUpdate(block.copy(param2 = "true")) })
                        PillToggle("Ocultar", block.param2 == "false", actionColor, { onUpdate(block.copy(param2 = "false")) })
                    }
                }

                ScratchActionType.FLOAT_CHARACTER -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("dad" to "Rival", "boyfriend" to "BF", "gf" to "GF").forEach { (t, l) ->
                            PillToggle(l, block.param1 == t, actionColor, { onUpdate(block.copy(param1 = t)) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = block.param2,
                            onValueChange = { onUpdate(block.copy(param2 = it)) },
                            label = { Text(block.actionType.param2Label) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = block.param3,
                            onValueChange = { onUpdate(block.copy(param3 = it)) },
                            label = { Text(block.actionType.param3Label) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ScratchActionType.TINT_CHARACTER -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("dad" to "Rival", "boyfriend" to "BF", "gf" to "GF").forEach { (t, l) ->
                            PillToggle(l, block.param1 == t, actionColor, { onUpdate(block.copy(param1 = t)) })
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("FFFFFF" to "Normal", "FF2E93" to "Rosa", "00E5FF" to "Cian", "2EFF9A" to "Menta", "FF3B5C" to "Rojo", "8B5CF6" to "Morado")
                            .forEach { (hex, lbl) ->
                                PillToggle(lbl, block.param2.equals(hex, ignoreCase = true), actionColor, { onUpdate(block.copy(param2 = hex)) })
                            }
                    }
                    OutlinedTextField(
                        value = block.param2,
                        onValueChange = { onUpdate(block.copy(param2 = it)) },
                        label = { Text(block.actionType.param2Label) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ScratchActionType.CAMERA_FOCUS -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillToggle("Rival (Dad)", block.param1 != "boyfriend", actionColor, { onUpdate(block.copy(param1 = "dad")) }, Modifier.weight(1f))
                        PillToggle("Boyfriend", block.param1 == "boyfriend", actionColor, { onUpdate(block.copy(param1 = "boyfriend")) }, Modifier.weight(1f))
                    }
                }

                ScratchActionType.CAMERA_FLASH,
                ScratchActionType.CAMERA_FADE -> {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("FFFFFF" to "Blanco", "000000" to "Negro", "FF2E93" to "Rosa", "00E5FF" to "Cian", "FF3B5C" to "Rojo")
                            .forEach { (hex, lbl) ->
                                PillToggle(lbl, block.param1.equals(hex, ignoreCase = true), actionColor, { onUpdate(block.copy(param1 = hex)) })
                            }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = block.param1,
                            onValueChange = { onUpdate(block.copy(param1 = it)) },
                            label = { Text(block.actionType.param1Label) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = block.param2,
                            onValueChange = { onUpdate(block.copy(param2 = it)) },
                            label = { Text(block.actionType.param2Label) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ScratchActionType.CENTER_STRUMS,
                ScratchActionType.CINEMATIC_BARS,
                ScratchActionType.HEALTH_DRAIN -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillToggle("Activar", block.param1 == "true", actionColor, { onUpdate(block.copy(param1 = "true")) })
                        PillToggle("Desactivar", block.param1 == "false", actionColor, { onUpdate(block.copy(param1 = "false")) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (block.actionType.param2Label.isNotBlank()) {
                            OutlinedTextField(
                                value = block.param2,
                                onValueChange = { onUpdate(block.copy(param2 = it)) },
                                label = { Text(block.actionType.param2Label) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (block.actionType.param3Label.isNotBlank()) {
                            OutlinedTextField(
                                value = block.param3,
                                onValueChange = { onUpdate(block.copy(param3 = it)) },
                                label = { Text(block.actionType.param3Label) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                ScratchActionType.PLAY_SOUND_FX -> {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("confirmMenu" to "Confirm", "scrollMenu" to "Scroll", "hitsound" to "HitSound", "cancelMenu" to "Cancel")
                            .forEach { (snd, lbl) ->
                                PillToggle(lbl, block.param1 == snd, actionColor, { onUpdate(block.copy(param1 = snd)) })
                            }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = block.param1,
                            onValueChange = { onUpdate(block.copy(param1 = it)) },
                            label = { Text(block.actionType.param1Label) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = block.param2,
                            onValueChange = { onUpdate(block.copy(param2 = it)) },
                            label = { Text(block.actionType.param2Label) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ScratchActionType.TOGGLE_HUD -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillToggle("Mostrar HUD", block.param1 == "true", actionColor, { onUpdate(block.copy(param1 = "true")) })
                        PillToggle("Ocultar HUD", block.param1 == "false", actionColor, { onUpdate(block.copy(param1 = "false")) })
                    }
                }

                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (block.actionType.param1Label.isNotBlank()) {
                            OutlinedTextField(
                                value = block.param1,
                                onValueChange = { onUpdate(block.copy(param1 = it)) },
                                label = { Text(block.actionType.param1Label) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (block.actionType.param2Label.isNotBlank()) {
                            OutlinedTextField(
                                value = block.param2,
                                onValueChange = { onUpdate(block.copy(param2 = it)) },
                                label = { Text(block.actionType.param2Label) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (block.actionType.param3Label.isNotBlank()) {
                            OutlinedTextField(
                                value = block.param3,
                                onValueChange = { onUpdate(block.copy(param3 = it)) },
                                label = { Text(block.actionType.param3Label) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScratchPaletteDialog(
    onDismiss: () -> Unit,
    onSelect: (ScratchTriggerType, ScratchActionType) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ScratchCategory.ARROWS) }
    var selectedTrigger by remember { mutableStateOf(ScratchTriggerType.ON_BEAT) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredActions = remember(selectedCategory, searchQuery) {
        val q = searchQuery.trim()
        if (q.isNotBlank()) {
            ScratchActionType.entries.filter {
                it.label.contains(q, ignoreCase = true) || it.category.label.contains(q, ignoreCase = true)
            }
        } else {
            ScratchActionType.entries.filter { it.category == selectedCategory }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        title = { Text("Agregar Bloque Scratch", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. Cuándo se activa:", style = MaterialTheme.typography.labelMedium, color = ScratchTriggerColor)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ScratchTriggerType.entries.forEach { t ->
                        PillToggle(t.label, selectedTrigger == t, ScratchTriggerColor, { selectedTrigger = t })
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar acción (ej: zoom, barras, vibrar...)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scratch_palette_search")
                )

                if (searchQuery.isBlank()) {
                    Text("2. Categoría de acción:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScratchCategory.entries.forEach { cat ->
                            val count = ScratchActionType.entries.count { it.category == cat }
                            PillToggle("${cat.label} ($count)", selectedCategory == cat, categoryColor(cat), { selectedCategory = cat })
                        }
                    }
                }

                Text("3. Elige la acción (${filteredActions.size}):", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredActions.forEach { action ->
                        val col = categoryColor(action.category)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(col.copy(alpha = 0.16f))
                                .border(1.dp, col.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .clickable { onSelect(selectedTrigger, action) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("scratch_action_${action.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(action.label, style = MaterialTheme.typography.labelLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                                if (searchQuery.isNotBlank()) {
                                    Text(action.category.label, style = MaterialTheme.typography.labelSmall, color = col)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = TextSecondary)
            }
        }
    )
}

private fun categoryColor(category: ScratchCategory): Color = when (category) {
    ScratchCategory.ARROWS -> ScratchArrowsColor
    ScratchCategory.CHARACTERS -> ScratchCharacterColor
    ScratchCategory.CAMERA -> ScratchCameraColor
    ScratchCategory.HUD -> ScratchHudColor
}
