package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TargetEngine
import com.example.ui.ModStudioViewModel
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class StudioTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    MUSIC("Música", Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
    CHART("Chart", Icons.Filled.GridOn, Icons.Outlined.GridOn),
    SPRITES("Sprites", Icons.Filled.Palette, Icons.Outlined.Palette),
    EVENTS("Eventos", Icons.Filled.Bolt, Icons.Outlined.Bolt),
    EXPORT("Exportar", Icons.Filled.FolderZip, Icons.Outlined.FolderZip)
}

@Composable
fun StudioScreen(
    viewModel: ModStudioViewModel,
    onBackHome: () -> Unit,
    onOpenPlaytest: () -> Unit,
    onOpenGuide: () -> Unit = {}
) {
    val project by viewModel.activeProject.collectAsStateWithLifecycle()
    val songIndex by viewModel.activeSongIndex.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddSongDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    BackHandler {
        viewModel.stopPlayback()
        onBackHome()
    }

    val currentProject = project ?: return
    val currentSong = currentProject.songs.getOrNull(songIndex) ?: currentProject.songs.firstOrNull()

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        val isWide = maxWidth >= 700.dp

        Scaffold(
            containerColor = ObsidianBg,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioSurface)
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.stopPlayback()
                                    onBackHome()
                                },
                                modifier = Modifier.testTag("studio_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver a proyectos",
                                    tint = TextPrimary
                                )
                            }
                            Column {
                                Text(
                                    text = currentProject.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val engCol = if (currentProject.targetEngine == TargetEngine.PSYCH_1_0_4) StudioCyan else StudioMint
                                Text(
                                    text = currentProject.targetEngine.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = engCol,
                                    modifier = Modifier.clickable {
                                        val next = if (currentProject.targetEngine == TargetEngine.PSYCH_1_0_4) {
                                            TargetEngine.PSYCH_0_7_3
                                        } else {
                                            TargetEngine.PSYCH_1_0_4
                                        }
                                        viewModel.updateModMetadata(targetEngine = next)
                                    }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.stopPlayback()
                                    onOpenGuide()
                                },
                                modifier = Modifier.testTag("studio_guide_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                                    contentDescription = "Abrir guía de ayuda",
                                    tint = StudioCyan
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.stopPlayback()
                                    onOpenPlaytest()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioCyan,
                                    contentColor = ObsidianBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("studio_playtest_button")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Probar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Song Selector Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        currentProject.songs.forEachIndexed { idx, s ->
                            val active = idx == songIndex
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) StudioCyan.copy(alpha = 0.16f) else StudioSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (active) StudioCyan else HairlineBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectSong(idx) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "${s.name} • ${s.bpm.toInt()} BPM",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) StudioCyan else TextSecondary
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioSurfaceElevated)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                                .clickable { showAddSongDialog = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Add, contentDescription = "Agregar canción", tint = StudioCyan, modifier = Modifier.size(14.dp))
                                Text("Canción", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                            }
                        }
                    }
                    HorizontalDivider(color = HairlineBorder)
                }
            },
            bottomBar = {
                if (!isWide) {
                    NavigationBar(
                        containerColor = StudioSurface,
                        tonalElevation = 0.dp
                    ) {
                        StudioTab.entries.forEachIndexed { idx, tab ->
                            val selected = selectedTab == idx
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = StudioCyan,
                                    selectedTextColor = StudioCyan,
                                    indicatorColor = StudioCyan.copy(alpha = 0.14f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWide) {
                    NavigationRail(
                        containerColor = StudioSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        StudioTab.entries.forEachIndexed { idx, tab ->
                            val selected = selectedTab == idx
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = StudioCyan,
                                    selectedTextColor = StudioCyan,
                                    indicatorColor = StudioCyan.copy(alpha = 0.14f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (StudioTab.entries[selectedTab]) {
                        StudioTab.MUSIC -> MusicTab(viewModel = viewModel, song = currentSong)
                        StudioTab.CHART -> ChartTab(viewModel = viewModel, song = currentSong)
                        StudioTab.SPRITES -> SpritesTab(viewModel = viewModel, project = currentProject)
                        StudioTab.EVENTS -> EventsTab(viewModel = viewModel, project = currentProject, song = currentSong)
                        StudioTab.EXPORT -> ExportTab(viewModel = viewModel, project = currentProject)
                    }
                }
            }
        }
    }

    if (showAddSongDialog) {
        var songName by remember { mutableStateOf("") }
        var bpmText by remember { mutableStateOf("128") }
        AlertDialog(
            onDismissRequest = { showAddSongDialog = false },
            containerColor = StudioSurface,
            title = { Text("Agregar Canción", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = songName,
                        onValueChange = { songName = it },
                        label = { Text("Nombre de la canción") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bpmText,
                        onValueChange = { bpmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("BPM") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addSong(songName.ifBlank { "Track ${currentProject.songs.size + 1}" }, bpmText.toFloatOrNull() ?: 128f)
                        showAddSongDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = ObsidianBg)
                ) {
                    Text("Agregar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSongDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}
