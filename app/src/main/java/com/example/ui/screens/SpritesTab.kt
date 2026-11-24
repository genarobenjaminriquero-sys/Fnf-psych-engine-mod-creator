package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.CharacterAsset
import com.example.model.ModProject
import com.example.model.NoteSkinMode
import com.example.model.SpriteMode
import com.example.ui.ModStudioViewModel
import com.example.ui.components.ColorSwatchRow
import com.example.ui.components.CustomNotesEditorContent
import com.example.ui.components.FnfArrowIcon
import com.example.ui.components.PillToggle
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHigh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.laneColor
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun SpritesTab(
    viewModel: ModStudioViewModel,
    project: ModProject
) {
    var subTab by remember { mutableIntStateOf(0) } // 0=Personajes, 1=Escenario, 2=Flechas, 3=Notas Nuevas, 4=Pack Icon

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Personajes", "Escenario", "Flechas / Notas", "Notas Nuevas", "Pack Icon").forEachIndexed { idx, label ->
                PillToggle(
                    label = label,
                    selected = subTab == idx,
                    accent = StudioCyan,
                    onClick = { subTab = idx },
                    modifier = Modifier.testTag("sprites_subtab_$idx")
                )
            }
        }

        when (subTab) {
            0 -> CharactersEditor(viewModel, project)
            1 -> StageEditor(viewModel, project)
            2 -> NoteSkinEditor(viewModel, project)
            3 -> CustomNotesEditorContent(viewModel, project)
            else -> PackIconEditor(viewModel, project)
        }
    }
}

@Composable
private fun CharactersEditor(viewModel: ModStudioViewModel, project: ModProject) {
    var selectedCharIdx by remember(project.id) { mutableIntStateOf(0) }
    val safeIdx = selectedCharIdx.coerceIn(0, (project.characters.lastIndex).coerceAtLeast(0))
    val char = project.characters.getOrNull(safeIdx) ?: return

    var previewAnim by remember { mutableStateOf("idle") }
    var pendingAnimTarget by remember { mutableStateOf("idle") }
    var showAddCharDialog by remember { mutableStateOf(false) }

    val multiFramePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(12)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importCharacterFrames(char.id, pendingAnimTarget, uris)
        }
    }

    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.importCharacterIcon(char.id, it) }
    }

    val atlasPngPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.importCharacterAtlasFile(char.id, it, isXml = false) }
    }

    val atlasXmlPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importCharacterAtlasFile(char.id, it, isXml = true) }
    }

    // Animate preview frames
    var animTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(char.id, previewAnim) {
        while (true) {
            delay(260L)
            animTick = (animTick + 1) % 100
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                project.characters.forEachIndexed { idx, c ->
                    PillToggle(
                        label = c.displayName,
                        selected = idx == safeIdx,
                        accent = Color(c.primaryColorHex),
                        onClick = { selectedCharIdx = idx }
                    )
                }
                PillToggle(
                    label = "+ Personaje",
                    selected = false,
                    accent = StudioMint,
                    onClick = { showAddCharDialog = true }
                )
            }
        }

        // Live Sprite Preview Card
        item {
            StudioCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(char.displayName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Text("ID: ${char.id} • Rol: ${char.role}", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                        // Health Icon Preview
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val iconFile = char.iconPngPath?.let(::File)?.takeIf { it.exists() }
                            if (iconFile != null) {
                                AsyncImage(
                                    model = iconFile,
                                    contentDescription = "Ícono de vida",
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(char.primaryColorHex).copy(alpha = 0.25f))
                                        .border(1.dp, Color(char.primaryColorHex), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Face, contentDescription = "Ícono por defecto", tint = Color(char.primaryColorHex))
                                }
                            }
                            FilledTonalButton(
                                onClick = {
                                    iconPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = StudioSurfaceElevated,
                                    contentColor = StudioCyan
                                )
                            ) {
                                Text("Ícono")
                            }
                        }
                    }

                    // Character Preview Viewport
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioSurfaceElevated)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val frames = char.animFrames[previewAnim].orEmpty().ifEmpty {
                            char.animFrames["idle"].orEmpty()
                        }
                        val framePath = if (frames.isNotEmpty()) frames[animTick % frames.size] else null
                        if (char.spriteMode == SpriteMode.IMPORTED_FRAMES && framePath != null && File(framePath).exists()) {
                            AsyncImage(
                                model = File(framePath),
                                contentDescription = "Vista previa de $previewAnim",
                                modifier = Modifier.size(140.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else if (char.spriteMode == SpriteMode.IMPORTED_ATLAS && !char.atlasPngPath.isNullOrBlank() && File(char.atlasPngPath).exists()) {
                            AsyncImage(
                                model = File(char.atlasPngPath),
                                contentDescription = "Spritesheet del personaje",
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            ProceduralCharacterPreview(char = char, anim = previewAnim, tick = animTick)
                        }
                    }

                    // Animation Preview Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("idle" to "Idle", "singLEFT" to "Left", "singDOWN" to "Down", "singUP" to "Up", "singRIGHT" to "Right")
                            .forEach { (key, label) ->
                                PillToggle(
                                    label = label,
                                    selected = previewAnim == key,
                                    accent = StudioCyan,
                                    onClick = { previewAnim = key }
                                )
                            }
                    }
                }
            }
        }

        // Sprite Mode & Import Options
        item {
            StudioCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "Sprites del Personaje",
                        subtitle = "Importa imágenes sueltas (auto-atlas) o un spritesheet PNG + XML"
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillToggle(
                            label = "Auto Vector",
                            selected = char.spriteMode == SpriteMode.BUILTIN_VECTOR,
                            onClick = { viewModel.updateCharacter(char.id) { it.copy(spriteMode = SpriteMode.BUILTIN_VECTOR) } },
                            modifier = Modifier.weight(1f)
                        )
                        PillToggle(
                            label = "Imágenes",
                            selected = char.spriteMode == SpriteMode.IMPORTED_FRAMES,
                            accent = StudioMint,
                            onClick = { viewModel.updateCharacter(char.id) { it.copy(spriteMode = SpriteMode.IMPORTED_FRAMES) } },
                            modifier = Modifier.weight(1f)
                        )
                        PillToggle(
                            label = "PNG + XML",
                            selected = char.spriteMode == SpriteMode.IMPORTED_ATLAS,
                            accent = StudioIndigo,
                            onClick = { viewModel.updateCharacter(char.id) { it.copy(spriteMode = SpriteMode.IMPORTED_ATLAS) } },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    when (char.spriteMode) {
                        SpriteMode.BUILTIN_VECTOR -> {
                            Text("Color Principal", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            ColorSwatchRow(selectedColorHex = char.primaryColorHex) { hex ->
                                val r = ((hex shr 16) and 0xFF).toInt()
                                val g = ((hex shr 8) and 0xFF).toInt()
                                val b = (hex and 0xFF).toInt()
                                viewModel.updateCharacter(char.id) {
                                    it.copy(primaryColorHex = hex, healthBarRgb = listOf(r, g, b))
                                }
                            }
                            Text("Color Detalle / Ojos", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            ColorSwatchRow(selectedColorHex = char.accentColorHex) { hex ->
                                viewModel.updateCharacter(char.id) { it.copy(accentColorHex = hex) }
                            }
                        }

                        SpriteMode.IMPORTED_FRAMES -> {
                            Text(
                                "Elige PNGs de tu galería para cada animación. FunkinCraft creará el Atlas PNG + Sparrow XML automáticamente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            val animButtons = listOf(
                                "idle" to "Idle",
                                "singLEFT" to "Left",
                                "singDOWN" to "Down",
                                "singUP" to "Up",
                                "singRIGHT" to "Right"
                            )
                            animButtons.forEach { (key, label) ->
                                val count = char.animFrames[key]?.size ?: 0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(StudioSurfaceElevated)
                                        .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                        Text(
                                            if (count > 0) "$count frame(s) listos" else "Sin frames (usará respaldo)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (count > 0) StudioMint else TextSecondary
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            pendingAnimTarget = key
                                            multiFramePicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Elegir PNGs")
                                    }
                                }
                            }
                        }

                        SpriteMode.IMPORTED_ATLAS -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        atlasPngPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated, contentColor = TextPrimary)
                                ) {
                                    Icon(Icons.Filled.Image, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (char.atlasPngPath != null) "PNG Listo" else "Subir PNG")
                                }
                                Button(
                                    onClick = { atlasXmlPicker.launch(arrayOf("text/xml", "application/xml", "*/*")) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated, contentColor = TextPrimary)
                                ) {
                                    Icon(Icons.Filled.Code, contentDescription = null, tint = StudioMint, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (char.atlasXmlPath != null) "XML Listo" else "Subir XML")
                                }
                            }
                        }
                    }

                    // Flip X & Scale
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Voltear Horizontal (Flip X)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Switch(
                            checked = char.flipX,
                            onCheckedChange = { f -> viewModel.updateCharacter(char.id) { it.copy(flipX = f) } }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Escala ${String.format("%.1f", char.scale)}x", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Slider(
                            value = char.scale,
                            onValueChange = { sc ->
                                val r = ((sc * 10).toInt() / 10f).coerceIn(0.4f, 2.5f)
                                viewModel.updateCharacter(char.id) { it.copy(scale = r) }
                            },
                            valueRange = 0.4f..2.5f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan, inactiveTrackColor = StudioSurfaceHigh)
                        )
                    }
                }
            }
        }
    }

    if (showAddCharDialog) {
        var name by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("opponent") }
        AlertDialog(
            onDismissRequest = { showAddCharDialog = false },
            containerColor = StudioSurface,
            title = { Text("Nuevo Personaje", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre (ej: Phase 2 Rival)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillToggle("Rival", role == "opponent", onClick = { role = "opponent" }, modifier = Modifier.weight(1f))
                        PillToggle("Jugador", role == "player", onClick = { role = "player" }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addCharacter(name.ifBlank { "Custom Char" }, role)
                        showAddCharDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = ObsidianBg)
                ) { Text("Agregar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddCharDialog = false }) { Text("Cancelar", color = TextSecondary) }
            }
        )
    }
}

@Composable
fun ProceduralCharacterPreview(char: CharacterAsset, anim: String, tick: Int) {
    val primary = Color(char.primaryColorHex)
    val accent = Color(char.accentColorHex)
    val bounce = if (tick % 2 == 1) 6f else 0f
    val dx = when (anim) {
        "singLEFT" -> -16f
        "singRIGHT" -> 16f
        else -> 0f
    }
    val dy = when (anim) {
        "singDOWN" -> 14f
        "singUP" -> -14f
        else -> bounce
    }
    Canvas(modifier = Modifier.size(130.dp)) {
        val cx = size.width / 2f + dx
        val cy = size.height / 2f + dy
        // Body
        drawRoundRect(
            color = primary,
            topLeft = Offset(cx - 26.dp.toPx(), cy - 6.dp.toPx()),
            size = Size(52.dp.toPx(), 60.dp.toPx()),
            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(cx - 26.dp.toPx(), cy - 6.dp.toPx()),
            size = Size(52.dp.toPx(), 60.dp.toPx()),
            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        // Head
        drawCircle(color = primary, radius = 26.dp.toPx(), center = Offset(cx, cy - 34.dp.toPx()))
        drawCircle(color = Color.White, radius = 26.dp.toPx(), center = Offset(cx, cy - 34.dp.toPx()), style = Stroke(width = 2.dp.toPx()))
        // Eyes
        drawCircle(color = accent, radius = 4.dp.toPx(), center = Offset(cx - 9.dp.toPx(), cy - 37.dp.toPx()))
        drawCircle(color = accent, radius = 4.dp.toPx(), center = Offset(cx + 9.dp.toPx(), cy - 37.dp.toPx()))
    }
}

@Composable
private fun StageEditor(viewModel: ModStudioViewModel, project: ModProject) {
    val stage = project.stages.firstOrNull() ?: return
    var targetLayer by remember { mutableStateOf("sky") }
    val layerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.importStageLayer(targetLayer, it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StudioCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "Escenario (${stage.name})",
                        subtitle = "Sube tus fondos desde la galería o personaliza el color del escenario"
                    )

                    // Mini Stage Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(stage.bgColorHex))
                            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    ) {
                        if (!stage.skyImagePath.isNullOrBlank() && File(stage.skyImagePath).exists()) {
                            AsyncImage(
                                model = File(stage.skyImagePath),
                                contentDescription = "Fondo del escenario",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(42.dp)
                                .background(Color(stage.floorColorHex))
                        ) {
                            if (!stage.groundImagePath.isNullOrBlank() && File(stage.groundImagePath).exists()) {
                                AsyncImage(
                                    model = File(stage.groundImagePath),
                                    contentDescription = "Piso del escenario",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                targetLayer = "sky"
                                layerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated, contentColor = TextPrimary)
                        ) {
                            Icon(Icons.Filled.Landscape, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fondo")
                        }
                        Button(
                            onClick = {
                                targetLayer = "ground"
                                layerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated, contentColor = TextPrimary)
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null, tint = StudioMint, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Piso")
                        }
                        Button(
                            onClick = {
                                targetLayer = "fg"
                                layerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated, contentColor = TextPrimary)
                        ) {
                            Text("Props")
                        }
                    }

                    Text("Zoom por defecto: ${String.format("%.2f", stage.defaultZoom)}", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Slider(
                        value = stage.defaultZoom,
                        onValueChange = { z ->
                            val r = ((z * 20).toInt() / 20f).coerceIn(0.5f, 1.5f)
                            viewModel.updateStage { it.copy(defaultZoom = r) }
                        },
                        valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan, inactiveTrackColor = StudioSurfaceHigh)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteSkinEditor(viewModel: ModStudioViewModel, project: ModProject) {
    val skin = project.noteSkin
    var pendingLane by remember { mutableIntStateOf(0) }
    val arrowPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.importCustomNoteArrow(pendingLane, it) }
    }
    val atlasPngPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.importNoteAtlasFile(it, isXml = false) }
    }
    val atlasXmlPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importNoteAtlasFile(it, isXml = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StudioCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "Sprites de Flechas (NOTE_assets)",
                        subtitle = "Usa las de Psych Engine, cambia colores o sube tus propios PNGs para cada flecha"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PillToggle("Por Defecto", skin.mode == NoteSkinMode.DEFAULT, onClick = {
                            viewModel.updateNoteSkin { it.copy(mode = NoteSkinMode.DEFAULT) }
                        })
                        PillToggle("Colores Propios", skin.mode == NoteSkinMode.CUSTOM_COLORS, accent = StudioCyan, onClick = {
                            viewModel.updateNoteSkin { it.copy(mode = NoteSkinMode.CUSTOM_COLORS) }
                        })
                        PillToggle("4 PNGs Propios", skin.mode == NoteSkinMode.CUSTOM_ARROWS, accent = StudioMint, onClick = {
                            viewModel.updateNoteSkin { it.copy(mode = NoteSkinMode.CUSTOM_ARROWS) }
                        })
                        PillToggle("PNG + XML", skin.mode == NoteSkinMode.IMPORTED_ATLAS, accent = StudioIndigo, onClick = {
                            viewModel.updateNoteSkin { it.copy(mode = NoteSkinMode.IMPORTED_ATLAS) }
                        })
                    }

                    if (skin.mode == NoteSkinMode.CUSTOM_ARROWS || skin.mode == NoteSkinMode.CUSTOM_COLORS) {
                        val names = listOf("Izquierda", "Abajo", "Arriba", "Derecha")
                        (0..3).forEach { lane ->
                            val customPath = skin.customArrowPaths[lane]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StudioSurfaceElevated)
                                    .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (customPath != null && File(customPath).exists()) {
                                        AsyncImage(
                                            model = File(customPath),
                                            contentDescription = names[lane],
                                            modifier = Modifier.size(30.dp)
                                        )
                                    } else {
                                        FnfArrowIcon(lane = lane, color = Color(skin.customColorsHex.getOrElse(lane) { 0xFF00E5FF }), size = 26.dp)
                                    }
                                    Text(names[lane], style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                }
                                if (skin.mode == NoteSkinMode.CUSTOM_ARROWS) {
                                    FilledTonalButton(
                                        onClick = {
                                            pendingLane = lane
                                            arrowPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("Subir PNG") }
                                } else {
                                    Text("Auto-Atlas", style = MaterialTheme.typography.labelSmall, color = laneColor(lane))
                                }
                            }
                        }
                    }

                    if (skin.mode == NoteSkinMode.IMPORTED_ATLAS) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { atlasPngPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated, contentColor = TextPrimary)
                            ) { Text(if (skin.atlasPngPath != null) "PNG Listo" else "NOTE_assets.png") }

                            Button(
                                onClick = { atlasXmlPicker.launch(arrayOf("text/xml", "application/xml", "*/*")) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated, contentColor = TextPrimary)
                            ) { Text(if (skin.atlasXmlPath != null) "XML Listo" else "NOTE_assets.xml") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackIconEditor(viewModel: ModStudioViewModel, project: ModProject) {
    val packPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.importPackIcon(it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StudioCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        title = "Ícono del Mod (pack.png)",
                        subtitle = "Aparece en el menú Mods de Psych Engine (150x150 px automático)"
                    )
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(StudioSurfaceElevated)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!project.packIconPath.isNullOrBlank() && File(project.packIconPath).exists()) {
                            AsyncImage(
                                model = File(project.packIconPath),
                                contentDescription = "Ícono del Mod",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            FnfArrowIcon(lane = 2, color = StudioCyan, filled = true, size = 48.dp)
                        }
                    }

                    Button(
                        onClick = {
                            packPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = ObsidianBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Elegir Imagen de Galería", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
