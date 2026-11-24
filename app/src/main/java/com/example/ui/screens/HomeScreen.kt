package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ModProject
import com.example.model.TargetEngine
import com.example.ui.components.PillToggle
import com.example.ui.components.StudioCard
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    projects: List<ModProject>,
    onOpenMod: (String) -> Unit,
    onPlaytestMod: (String) -> Unit,
    onCreateMod: (name: String, author: String, engine: TargetEngine) -> Unit,
    onDeleteMod: (String) -> Unit,
    onOpenGuide: () -> Unit = {}
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var confirmDeleteMod by remember { mutableStateOf<ModProject?>(null) }

    Scaffold(
        containerColor = ObsidianBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = StudioCyan,
                contentColor = ObsidianBg,
                modifier = Modifier.testTag("new_mod_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_mod))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    StudioHeroCard(
                        onCreateClick = { showCreateDialog = true },
                        onGuideClick = onOpenGuide
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tus Proyectos (${projects.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Listos para ZIP",
                            style = MaterialTheme.typography.labelMedium,
                            color = StudioCyan
                        )
                    }
                }

                if (projects.isEmpty()) {
                    item {
                        StudioCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FolderZip,
                                    contentDescription = null,
                                    tint = StudioCyan,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = stringResource(R.string.empty_mods_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.empty_mods_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(projects, key = { it.id }) { mod ->
                        ModProjectCard(
                            mod = mod,
                            onOpen = { onOpenMod(mod.id) },
                            onPlaytest = { onPlaytestMod(mod.id) },
                            onDelete = { confirmDeleteMod = mod }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showCreateDialog) {
        CreateModDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, author, engine ->
                showCreateDialog = false
                onCreateMod(name, author, engine)
            }
        )
    }

    confirmDeleteMod?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeleteMod = null },
            containerColor = StudioSurface,
            title = { Text("Eliminar Mod", color = TextPrimary) },
            text = {
                Text(
                    "¿Seguro que quieres borrar \"${target.name}\"? Se eliminarán sus charts, audios y sprites guardados.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMod(target.id)
                    confirmDeleteMod = null
                }) {
                    Text("Eliminar", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteMod = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun StudioHeroCard(
    onCreateClick: () -> Unit,
    onGuideClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
            .background(StudioSurface)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_studio_header_1795383588663),
            contentDescription = "Banner de estudio FunkinCraft",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ObsidianBg.copy(alpha = 0.94f),
                            ObsidianBg.copy(alpha = 0.72f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "FUNKINCRAFT",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Crea tu Mod FNF",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Psych Engine 1.0.4 y 0.7.3 • Sin errores",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCreateClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioCyan,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("hero_new_mod_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nuevo Mod", fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onGuideClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = StudioSurfaceElevated.copy(alpha = 0.92f),
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("home_guide_button")
                ) {
                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = StudioMint, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guía")
                }
            }
        }
    }
}

@Composable
private fun ModProjectCard(
    mod: ModProject,
    onOpen: () -> Unit,
    onPlaytest: () -> Unit,
    onDelete: () -> Unit
) {
    val engineColor = if (mod.targetEngine == TargetEngine.PSYCH_1_0_4) StudioCyan else StudioMint
    val totalNotes = mod.songs.sumOf { s -> s.sections.sumOf { it.notes.size } }

    StudioCard(
        modifier = Modifier
            .clickable(onClick = onOpen)
            .testTag("mod_card_${mod.slug}")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = mod.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(engineColor.copy(alpha = 0.14f))
                                .border(1.dp, engineColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Psych ${mod.targetEngine.versionTag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = engineColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mod.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_mod_${mod.slug}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Eliminar mod",
                        tint = TextSecondary
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                    Text("${mod.songs.size} canción(es) • $totalNotes notas", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.PeopleOutline, contentDescription = null, tint = StudioIndigo, modifier = Modifier.size(16.dp))
                    Text("${mod.characters.size} personajes", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpen,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_mod_${mod.slug}"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = StudioSurfaceElevated,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Abrir Estudio")
                }

                Button(
                    onClick = onPlaytest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioCyan,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("playtest_mod_${mod.slug}")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Probar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CreateModDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, author: String, engine: TargetEngine) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("Creador") }
    var selectedEngine by remember { mutableStateOf(TargetEngine.PSYCH_1_0_4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        title = { Text("Nuevo Mod de Psych Engine", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Mod") },
                    placeholder = { Text("Ej: Vs Cyber Funk") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_mod_name_input")
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Tu nombre / Autor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Versión objetivo", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PillToggle(
                        label = "Psych 1.0.4",
                        selected = selectedEngine == TargetEngine.PSYCH_1_0_4,
                        accent = StudioCyan,
                        onClick = { selectedEngine = TargetEngine.PSYCH_1_0_4 },
                        modifier = Modifier.weight(1f)
                    )
                    PillToggle(
                        label = "Psych 0.7.3",
                        selected = selectedEngine == TargetEngine.PSYCH_0_7_3,
                        accent = StudioMint,
                        onClick = { selectedEngine = TargetEngine.PSYCH_0_7_3 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.ifBlank { "Mi Nuevo Mod" }, author, selectedEngine) },
                colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = ObsidianBg),
                modifier = Modifier.testTag("confirm_create_mod_button")
            ) {
                Text("Crear Mod", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
