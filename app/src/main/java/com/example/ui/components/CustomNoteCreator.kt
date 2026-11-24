package com.example.ui.components

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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.CustomNoteEffect
import com.example.model.CustomNoteShape
import com.example.model.CustomNoteType
import com.example.model.ModProject
import com.example.ui.ModStudioViewModel
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHigh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CustomNoteShapeIcon(
    shape: CustomNoteShape,
    color: Color,
    lane: Int = 2,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    if (shape == CustomNoteShape.ARROW) {
        FnfArrowIcon(lane = lane, color = color, filled = true, size = size, modifier = modifier)
        return
    }
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension * 0.42f
        val strokeWidth = 1.6.dp.toPx()

        when (shape) {
            CustomNoteShape.ARROW -> Unit
            CustomNoteShape.MINE_SPIKE -> {
                val path = Path()
                val spikes = 8
                for (i in 0 until spikes * 2) {
                    val rad = if (i % 2 == 0) r * 1.05f else r * 0.55f
                    val angle = Math.toRadians((i * 180.0 / spikes) - 90.0)
                    val px = cx + (rad * cos(angle)).toFloat()
                    val py = cy + (rad * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, color = color)
                drawPath(path, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = strokeWidth))
                drawCircle(color = ObsidianBg, radius = r * 0.24f, center = Offset(cx, cy))
            }
            CustomNoteShape.HEART_HEAL -> {
                val path = Path().apply {
                    moveTo(cx, cy + r * 0.85f)
                    cubicTo(cx - r * 1.15f, cy + r * 0.2f, cx - r * 0.9f, cy - r * 0.85f, cx, cy - r * 0.28f)
                    cubicTo(cx + r * 0.9f, cy - r * 0.85f, cx + r * 1.15f, cy + r * 0.2f, cx, cy + r * 0.85f)
                    close()
                }
                drawPath(path, color = color)
                drawPath(path, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = strokeWidth))
            }
            CustomNoteShape.STAR_PARRY -> {
                val path = Path()
                for (i in 0 until 10) {
                    val rad = if (i % 2 == 0) r else r * 0.44f
                    val angle = Math.toRadians((i * 36.0) - 90.0)
                    val px = cx + (rad * cos(angle)).toFloat()
                    val py = cy + (rad * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, color = color)
                drawPath(path, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = strokeWidth))
            }
            CustomNoteShape.GLITCH_BLOCK -> {
                val side = r * 1.6f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cx - side / 2f, cy - side / 2f),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(cx - side / 2f, cy - side / 2f),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )
                drawRect(
                    color = ObsidianBg,
                    topLeft = Offset(cx - side * 0.3f, cy - side * 0.1f),
                    size = Size(side * 0.6f, side * 0.2f)
                )
            }
        }
    }
}

@Composable
fun CustomNotesEditorContent(
    viewModel: ModStudioViewModel,
    project: ModProject,
    onSelectNoteTypeSlug: ((String) -> Unit)? = null
) {
    var editingNote by remember { mutableStateOf<CustomNoteType?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingImageTargetId by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val targetId = pendingImageTargetId
        if (uri != null && targetId != null) {
            viewModel.importCustomNoteImage(targetId, uri)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StudioCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Notas Nuevas (${project.customNoteTypes.size})",
                        subtitle = "Crea notas especiales con daño, curación, efectos o sprite propio. Exporta directo a custom_notetypes/",
                        trailing = {
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = ObsidianBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("create_custom_note_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nueva Nota", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
        }

        items(project.customNoteTypes, key = { it.id }) { noteType ->
            val noteCol = Color(noteType.colorHex)
            StudioCard(borderColor = noteCol.copy(alpha = 0.45f)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(noteCol.copy(alpha = 0.16f))
                                    .border(1.dp, noteCol.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!noteType.customPngPath.isNullOrBlank() && File(noteType.customPngPath).exists()) {
                                    AsyncImage(
                                        model = File(noteType.customPngPath),
                                        contentDescription = noteType.name,
                                        modifier = Modifier.size(34.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    CustomNoteShapeIcon(shape = noteType.shape, color = noteCol, size = 28.dp)
                                }
                            }
                            Column {
                                Text(noteType.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    text = "ID: \"${noteType.slug}\" • ${noteType.hitEffect.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { editingNote = noteType },
                                modifier = Modifier.testTag("edit_custom_note_${noteType.id}")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar nota", tint = StudioCyan)
                            }
                            if (project.customNoteTypes.size > 1) {
                                IconButton(
                                    onClick = { viewModel.deleteCustomNoteType(noteType.id) },
                                    modifier = Modifier.testTag("delete_custom_note_${noteType.id}")
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar nota", tint = ErrorRed)
                                }
                            }
                        }
                    }

                    // Stats Badges
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val healthSign = if (noteType.healthDelta >= 0) "+" else ""
                        NoteStatChip(
                            label = "Vida al tocar: $healthSign${(noteType.healthDelta * 100).toInt()}%",
                            color = if (noteType.healthDelta >= 0) StudioMint else ErrorRed
                        )
                        NoteStatChip(
                            label = if (noteType.ignoreNote) "Esquivable (Ignore)" else "Obligatoria",
                            color = if (noteType.ignoreNote) WarningAmber else StudioCyan
                        )
                        if (noteType.hitMessage.isNotBlank()) {
                            NoteStatChip(label = "Texto: ${noteType.hitMessage}", color = noteCol)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                pendingImageTargetId = noteType.id
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = StudioSurfaceElevated,
                                contentColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = StudioMint)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (noteType.customPngPath != null) "Cambiar PNG" else "Subir PNG")
                        }

                        if (onSelectNoteTypeSlug != null) {
                            Button(
                                onClick = { onSelectNoteTypeSlug(noteType.slug) },
                                colors = ButtonDefaults.buttonColors(containerColor = noteCol, contentColor = ObsidianBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Usar en Chart", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CustomNoteEditDialog(
            initial = null,
            onDismiss = { showCreateDialog = false },
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
                showCreateDialog = false
                onSelectNoteTypeSlug?.invoke(created.slug)
            }
        )
    }

    editingNote?.let { target ->
        CustomNoteEditDialog(
            initial = target,
            onDismiss = { editingNote = null },
            onSave = { name, shape, colorHex, ignoreNote, noAnim, healthDelta, missPenalty, effect, message ->
                viewModel.updateCustomNoteType(target.id) { current ->
                    current.copy(
                        name = name.trim().ifEmpty { current.name },
                        shape = shape,
                        colorHex = colorHex,
                        ignoreNote = ignoreNote,
                        noAnimation = noAnim,
                        healthDelta = healthDelta,
                        missPenalty = missPenalty,
                        hitEffect = effect,
                        hitMessage = message.trim()
                    )
                }
                editingNote = null
            }
        )
    }
}

@Composable
private fun NoteStatChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun CustomNoteEditDialog(
    initial: CustomNoteType?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        shape: CustomNoteShape,
        colorHex: Long,
        ignoreNote: Boolean,
        noAnimation: Boolean,
        healthDelta: Float,
        missPenalty: Float,
        effect: CustomNoteEffect,
        message: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var shape by remember { mutableStateOf(initial?.shape ?: CustomNoteShape.STAR_PARRY) }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: 0xFFFFD60AL) }
    var ignoreNote by remember { mutableStateOf(initial?.ignoreNote ?: false) }
    var noAnim by remember { mutableStateOf(initial?.noAnimation ?: false) }
    var healthDelta by remember { mutableFloatStateOf(initial?.healthDelta ?: 0.15f) }
    var missPenalty by remember { mutableFloatStateOf(initial?.missPenalty ?: 0.12f) }
    var hitEffect by remember { mutableStateOf(initial?.hitEffect ?: CustomNoteEffect.CAMERA_FLASH) }
    var hitMessage by remember { mutableStateOf(initial?.hitMessage ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        title = {
            Text(
                text = if (initial == null) "Crear Nota Personalizada" else "Editar ${initial.name}",
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la nota") },
                    placeholder = { Text("Ej: Nota Veneno, Nota Escudo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_note_name_input")
                )

                Text("Forma visual:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CustomNoteShape.entries.forEach { sh ->
                        PillToggle(sh.label, shape == sh, Color(colorHex), { shape = sh })
                    }
                }

                Text("Color de la nota:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                ColorSwatchRow(selectedColorHex = colorHex, onSelect = { colorHex = it })

                Text("Efecto especial al golpear:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CustomNoteEffect.entries.forEach { ef ->
                        PillToggle(ef.label, hitEffect == ef, StudioCyan, { hitEffect = ef })
                    }
                }

                OutlinedTextField(
                    value = hitMessage,
                    onValueChange = { hitMessage = it },
                    label = { Text("Mensaje al acertar (opcional)") },
                    placeholder = { Text("Ej: +VIDA, ¡BOOM!, PARRY") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    val sign = if (healthDelta >= 0) "+" else ""
                    Text(
                        text = "Cambio de vida al tocar: $sign${(healthDelta * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (healthDelta >= 0) StudioMint else ErrorRed
                    )
                    Slider(
                        value = healthDelta,
                        onValueChange = { healthDelta = ((it * 20).toInt() / 20f).coerceIn(-0.6f, 0.5f) },
                        valueRange = -0.6f..0.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = StudioCyan,
                            activeTrackColor = StudioCyan,
                            inactiveTrackColor = StudioSurfaceHigh
                        )
                    )
                }

                Column {
                    Text(
                        text = "Daño si la fallas: ${(missPenalty * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = WarningAmber
                    )
                    Slider(
                        value = missPenalty,
                        onValueChange = { missPenalty = ((it * 20).toInt() / 20f).coerceIn(0f, 0.6f) },
                        valueRange = 0f..0.6f,
                        colors = SliderDefaults.colors(
                            thumbColor = WarningAmber,
                            activeTrackColor = WarningAmber,
                            inactiveTrackColor = StudioSurfaceHigh
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nota esquivable (Ignore Note)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("No penaliza si la dejas pasar (ideal para minas)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Switch(checked = ignoreNote, onCheckedChange = { ignoreNote = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sin animación al cantar", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("El personaje no hace pose al tocarla", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Switch(checked = noAnim, onCheckedChange = { noAnim = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.ifBlank { "Nota Especial" },
                        shape,
                        colorHex,
                        ignoreNote,
                        noAnim,
                        healthDelta,
                        missPenalty,
                        hitEffect,
                        hitMessage
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = ObsidianBg),
                modifier = Modifier.testTag("save_custom_note_button")
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
