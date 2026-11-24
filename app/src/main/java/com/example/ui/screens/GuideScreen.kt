package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ScratchActionType
import com.example.model.ScratchCategory
import com.example.model.ScratchTriggerType
import com.example.ui.components.PillToggle
import com.example.ui.components.StudioCard
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ScratchArrowsColor
import com.example.ui.theme.ScratchCameraColor
import com.example.ui.theme.ScratchCharacterColor
import com.example.ui.theme.ScratchHudColor
import com.example.ui.theme.ScratchTriggerColor
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

private enum class GuideCategory(val label: String, val accent: Color) {
    ALL("Todo", StudioCyan),
    QUICK_START("Inicio Rápido", StudioMint),
    TABS("Pestañas", StudioCyan),
    GLOSSARY("Diccionario FNF", StudioIndigo),
    SCRATCH("Bloques Scratch", ScratchTriggerColor),
    CUSTOM_NOTES("Notas Nuevas", ScratchCameraColor),
    INSTALL("Instalar ZIP", WarningAmber)
}

private data class GuideItem(
    val id: String,
    val category: GuideCategory,
    val title: String,
    val summary: String,
    val details: List<String>,
    val icon: ImageVector = Icons.Filled.MenuBook,
    val codeSnippet: String? = null
)

@Composable
fun GuideScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedCategory by remember { mutableStateOf(GuideCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedIds by remember { mutableStateOf(setOf("qs_steps", "tabs_overview")) }

    val staticGuides = remember { buildGuideItems() }

    val filteredItems = remember(staticGuides, selectedCategory, searchQuery) {
        val q = searchQuery.trim()
        staticGuides.filter { item ->
            val catMatch = selectedCategory == GuideCategory.ALL || item.category == selectedCategory
            val qMatch = q.isBlank() ||
                item.title.contains(q, ignoreCase = true) ||
                item.summary.contains(q, ignoreCase = true) ||
                item.details.any { it.contains(q, ignoreCase = true) }
            catMatch && qMatch
        }
    }

    val showScratchReference = (selectedCategory == GuideCategory.ALL || selectedCategory == GuideCategory.SCRATCH)
    val matchingTriggers = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) ScratchTriggerType.entries
        else ScratchTriggerType.entries.filter {
            it.label.contains(q, ignoreCase = true) || it.description.contains(q, ignoreCase = true)
        }
    }
    val matchingActions = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) ScratchActionType.entries
        else ScratchActionType.entries.filter {
            it.label.contains(q, ignoreCase = true) || it.category.label.contains(q, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = ObsidianBg,
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
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("guide_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Guía de FunkinCraft", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(
                            "Qué hace cada herramienta y cómo armar tu mod paso a paso",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar (ej: BPM, notas, vibrar, XML, ZIP...)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .testTag("guide_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GuideCategory.entries.forEach { cat ->
                        PillToggle(
                            label = cat.label,
                            selected = selectedCategory == cat,
                            accent = cat.accent,
                            onClick = { selectedCategory = cat },
                            modifier = Modifier.testTag("guide_cat_${cat.name.lowercase()}")
                        )
                    }
                }
                HorizontalDivider(color = HairlineBorder)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    val expanded = item.id in expandedIds
                    GuideExpandableCard(
                        item = item,
                        expanded = expanded,
                        onToggle = {
                            expandedIds = if (expanded) expandedIds - item.id else expandedIds + item.id
                        }
                    )
                }

                if (showScratchReference && (matchingTriggers.isNotEmpty() || matchingActions.isNotEmpty())) {
                    item {
                        StudioCard(borderColor = ScratchTriggerColor.copy(alpha = 0.45f)) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Enciclopedia de Bloques Scratch (${matchingTriggers.size} disparadores • ${matchingActions.size} acciones)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ScratchTriggerColor
                                )
                                Text(
                                    text = "Disparadores (CUANDO):",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                                matchingTriggers.forEach { trig ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(StudioSurfaceElevated)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(trig.label, style = MaterialTheme.typography.labelLarge, color = ScratchTriggerColor)
                                        Text(trig.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Acciones disponibles:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                                matchingActions.forEach { act ->
                                    val col = guideCategoryColor(act.category)
                                    val params = listOf(act.param1Label, act.param2Label, act.param3Label)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" • ")
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(StudioSurfaceElevated)
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(act.label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                            if (params.isNotBlank()) {
                                                Text("Parámetros: $params", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(col.copy(alpha = 0.18f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(act.category.label, style = MaterialTheme.typography.labelSmall, color = col)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun GuideExpandableCard(
    item: GuideItem,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    StudioCard(
        borderColor = if (expanded) item.category.accent.copy(alpha = 0.6f) else HairlineBorder,
        modifier = Modifier
            .clickable(onClick = onToggle)
            .testTag("guide_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(item.category.accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = null, tint = item.category.accent, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(item.category.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = item.category.accent)
                        Text(item.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Contraer" else "Expandir",
                    tint = TextSecondary
                )
            }

            Text(item.summary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = HairlineBorder, modifier = Modifier.padding(vertical = 4.dp))
                    item.details.forEach { bullet ->
                        Text(
                            text = "• $bullet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                    item.codeSnippet?.let { code ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianBg)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = code,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun guideCategoryColor(category: ScratchCategory): Color = when (category) {
    ScratchCategory.ARROWS -> ScratchArrowsColor
    ScratchCategory.CHARACTERS -> ScratchCharacterColor
    ScratchCategory.CAMERA -> ScratchCameraColor
    ScratchCategory.HUD -> ScratchHudColor
}

private fun buildGuideItems(): List<GuideItem> = listOf(
    GuideItem(
        id = "qs_steps",
        category = GuideCategory.QUICK_START,
        title = "Crea tu primer Mod en 5 pasos",
        summary = "Flujo rápido de principio a fin sin necesidad de programar ni usar PC.",
        icon = Icons.Filled.PlayArrow,
        details = listOf(
            "1. Crea un proyecto y elige si lo jugarás en Psych Engine 1.0.4 o 0.7.3.",
            "2. Ve a la pestaña Música: arma tu canción con Batería, Bajo, Synth y Voces o sube un archivo MP3/WAV/OGG de tu celular.",
            "3. Ve a la pestaña Chart: coloca las flechas en el orden que quieras o presiona 'Sincronizar con voces' para generarlas automáticamente.",
            "4. En Sprites y Eventos: sube tus personajes/fondos, crea Notas Nuevas y agrega bloques tipo Scratch (vibrar flechas, barras de cine, zoom).",
            "5. Toca 'Probar' arriba para jugarlo dentro de la app. Cuando quede perfecto, ve a Exportar y descarga tu .zip."
        )
    ),
    GuideItem(
        id = "tabs_overview",
        category = GuideCategory.TABS,
        title = "Qué hace cada pestaña del Estudio",
        summary = "Resumen de las 5 herramientas inferiores y el botón Probar.",
        icon = Icons.Filled.GridOn,
        details = listOf(
            "Música: compositor nativo con 5 instrumentos (Batería, Bajo, Synth, Voz BF, Voz Rival) o importador de archivos Inst.ogg/Voices.ogg desde tu teléfono.",
            "Chart: editor de flechas paso a paso (16 steps por sección). Ajusta velocidad de caída (Scroll), Snap, notas largas (Hold) y tipos de nota.",
            "Sprites: edita personajes (Auto Vector, PNGs sueltos o PNG+XML), escenarios (Fondo/Piso/Props), skin de flechas, Notas Nuevas e ícono del Mod.",
            "Eventos: programación visual estilo Scratch con 10 disparadores y 28 bloques de acción que se compilan a script.lua real.",
            "Exportar: configura el nombre de la Semana, dificultades y verifica que todo esté en verde antes de crear el .zip final."
        )
    ),
    GuideItem(
        id = "tabs_music",
        category = GuideCategory.TABS,
        title = "Cómo usar el Compositor vs Audio Propio",
        summary = "Dos formas de crear tus canciones sin errores de formato.",
        icon = Icons.Filled.MusicNote,
        details = listOf(
            "Modo Sintetizador: activa cuadros de batería y elige notas musicales (Do, Re, Mi...) para BF y el Rival. Al exportar se convierte automáticamente en Inst.ogg y Voices.ogg.",
            "Modo Importar Audio: elige cualquier canción MP3, WAV, M4A u OGG de tu celular. La app lo convierte a Ogg Vorbis 44.1kHz compatible con Psych Engine.",
            "Tip de Sincronización: ajusta el BPM para que el grid del Chart coincida con el pulso real de tu canción importada."
        )
    ),
    GuideItem(
        id = "tabs_sprites",
        category = GuideCategory.TABS,
        title = "Cómo subir Sprites y Personajes",
        summary = "No necesitas saber usar Adobe Animate: usa imágenes sueltas.",
        icon = Icons.Filled.Palette,
        details = listOf(
            "Auto Vector: personajes listos al instante con colores personalizables y animación automática.",
            "Imágenes sueltas (Recomendado): elige 1 o más PNGs de tu galería para Idle, Left, Down, Up y Right. FunkinCraft construye el spritesheet PNG y el Sparrow XML automáticamente.",
            "PNG + XML: si ya tienes un spritesheet de PC/FNF, súbelo directo junto con su archivo .xml.",
            "Ícono de vida: sube cualquier imagen cuadrada y FunkinCraft genera el ícono doble de 300x150 px que pide Psych Engine."
        )
    ),
    GuideItem(
        id = "glossary_fnf",
        category = GuideCategory.GLOSSARY,
        title = "Diccionario de términos FNF y Psych Engine",
        summary = "Aprende qué significa BPM, Scroll, Snap, Hold, MustHit y Sparrow XML.",
        icon = Icons.Filled.MenuBook,
        details = listOf(
            "BPM (Beats Por Minuto): qué tan rápida es la canción. 100 es lento, 128 es estándar, 170+ es muy rápido.",
            "Scroll Speed: qué tan rápido bajan/suben las flechas en pantalla (2.0 a 3.2 es lo más cómodo).",
            "Snap (1/16, 1/8, 1/4): qué tan juntas puedes poner notas en el grid. 1/16 permite ritmos rápidos; 1/4 solo pone notas en cada beat.",
            "Hold (Tap / Corto / Largo): convierte una flecha normal en una nota mantenida con cola.",
            "Cám: BF / Rival (mustHitSection): decide a qué personaje enfoca la cámara durante esa sección.",
            "Psych 1.0.4 vs 0.7.3: en 1.0.4 el formato del chart es 'psych_v1' y los lados de flechas son fijos; en 0.7.3 usa formato clásico. Selecciona arriba tu versión antes de exportar."
        )
    ),
    GuideItem(
        id = "custom_notes_guide",
        category = GuideCategory.CUSTOM_NOTES,
        title = "Cómo crear y usar Notas Nuevas",
        summary = "Diseña minas, notas curativas, notas parry o efectos propios.",
        icon = Icons.Filled.Star,
        details = listOf(
            "Entra a Sprites -> 'Notas Nuevas' o presiona '+ Nota' dentro de la pestaña Chart.",
            "Elige Nombre, Forma (Mina, Corazón, Estrella, Glitch, Flecha) o sube un PNG propio desde tu galería.",
            "Configura 'Cambio de vida': positivo para curar (ej: +25%), negativo para dañar al jugador (ej: -35%).",
            "Activa 'Nota esquivable (Ignore Note)' si quieres que sea una trampa que el jugador NO debe presionar (si no la toca, no cuenta como Miss).",
            "Elige un 'Efecto especial' (Fuego, Flash, Sacudir Cámara, Girar Flechas, Veneno) y un mensaje flotante.",
            "Al exportar, FunkinCraft genera automáticamente 'custom_notetypes/<nombre>.lua' y su spritesheet en 'images/customNotes/'."
        )
    ),
    GuideItem(
        id = "scratch_how",
        category = GuideCategory.SCRATCH,
        title = "Cómo funcionan los Bloques Scratch",
        summary = "Combina un disparador amarillo (CUANDO) con una acción de color.",
        icon = Icons.Filled.Bolt,
        details = listOf(
            "Cada bloque tiene dos partes: arriba (Amarillo) el momento en que se activa, y abajo (Color) el efecto que ocurre.",
            "Ejemplo Modchart: 'Cada 4 Beats' + 'Pulso / Rebote de flechas' hace que las flechas salten al ritmo de la música.",
            "Ejemplo Cambio de Fase: 'En el Beat exacto 32' + 'Cambiar personaje' cambia al rival por otro personaje (con precarga automática para evitar lag).",
            "Ejemplo Cinemática: 'En el Beat exacto 16' + 'Barras cinemáticas' + 'Zoom de cámara' crea una entrada épica.",
            "Puedes activar o desactivar cualquier bloque con su interruptor, reordenarlos con las flechas o ver el código Lua real en el botón 'Lua'."
        )
    ),
    GuideItem(
        id = "install_zip",
        category = GuideCategory.INSTALL,
        title = "Cómo instalar tu .zip en Psych Engine",
        summary = "Dónde copiar la carpeta extraída para jugar sin errores en Android o PC.",
        icon = Icons.Filled.FolderZip,
        details = listOf(
            "1. En la pestaña Exportar, asegúrate de que todas las verificaciones estén en verde y toca 'Guardar Mod (.ZIP)'.",
            "2. Abre tu explorador de archivos y descomprime el archivo .zip.",
            "3. Verás una carpeta con el nombre de tu mod (dentro tiene pack.json, songs/, data/, characters/, stages/, custom_notetypes/).",
            "4. Copia esa carpeta completa dentro de la carpeta 'mods/' de tu Psych Engine (en Android suele estar en Android/media/ o Documents/PsychEngine/mods/; en PC junto al .exe en /mods/).",
            "5. Abre Psych Engine -> menú Mods -> activa tu mod y ¡juégalo en Story Mode o Freeplay!"
        ),
        codeSnippet = "PsychEngine/mods/tu-mod/\n  ├── pack.json & pack.png\n  ├── songs/<tema>/Inst.ogg & Voices.ogg\n  ├── data/<tema>/<tema>.json & script.lua\n  ├── custom_notetypes/*.lua\n  ├── characters/*.json\n  ├── stages/*.json & *.lua\n  └── images/ (characters, icons, customNotes)"
    )
)
