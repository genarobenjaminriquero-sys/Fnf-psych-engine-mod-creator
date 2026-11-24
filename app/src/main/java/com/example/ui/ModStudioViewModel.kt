package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.AudioSynthEngine
import com.example.engine.ModRepository
import com.example.engine.ModValidationReport
import com.example.engine.PsychModExporter
import com.example.engine.SongPlaybackController
import com.example.model.AudioSourceMode
import com.example.model.CharacterAsset
import com.example.model.ChartNote
import com.example.model.ChartSection
import com.example.model.ComposerTrackData
import com.example.model.CustomNoteEffect
import com.example.model.CustomNoteShape
import com.example.model.CustomNoteType
import com.example.model.ModProject
import com.example.model.NoteSkinAsset
import com.example.model.NoteSkinMode
import com.example.model.ScratchActionType
import com.example.model.ScratchBlock
import com.example.model.ScratchTriggerType
import com.example.model.SongProject
import com.example.model.SpriteMode
import com.example.model.StageAsset
import com.example.model.TargetEngine
import com.example.model.createEmptySections
import com.example.model.slugify
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max

class ModStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ModRepository(application)
    val playback = SongPlaybackController(viewModelScope)

    private val _projects = MutableStateFlow<List<ModProject>>(emptyList())
    val projects: StateFlow<List<ModProject>> = _projects.asStateFlow()

    private val _activeProject = MutableStateFlow<ModProject?>(null)
    val activeProject: StateFlow<ModProject?> = _activeProject.asStateFlow()

    private val _activeSongIndex = MutableStateFlow(0)
    val activeSongIndex: StateFlow<Int> = _activeSongIndex.asStateFlow()

    private val _validationReport = MutableStateFlow<ModValidationReport?>(null)
    val validationReport: StateFlow<ModValidationReport?> = _validationReport.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow("")
    val exportProgress: StateFlow<String> = _exportProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        refreshProjects()
    }

    fun refreshProjects() {
        viewModelScope.launch {
            _projects.value = repo.loadAllProjects()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showStatus(msg: String) {
        _statusMessage.value = msg
    }

    fun openProject(projectId: String) {
        val found = _projects.value.find { it.id == projectId } ?: return
        playback.stop()
        _activeProject.value = found
        _activeSongIndex.value = 0
        refreshValidation(found)
    }

    fun createProject(name: String, author: String, engine: TargetEngine, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val created = repo.createNewBlankProject(name.trim().ifEmpty { "Mi Mod" }, author.trim(), engine)
            val saved = repo.saveProject(created)
            _projects.value = listOf(saved) + _projects.value
            _activeProject.value = saved
            _activeSongIndex.value = 0
            refreshValidation(saved)
            onCreated(saved.id)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            if (_activeProject.value?.id == projectId) {
                playback.stop()
                _activeProject.value = null
            }
            repo.deleteProject(projectId)
            _projects.value = _projects.value.filterNot { it.id == projectId }
            if (_projects.value.isEmpty()) {
                _projects.value = repo.loadAllProjects()
            }
            showStatus("Mod eliminado")
        }
    }

    fun selectSong(index: Int) {
        val proj = _activeProject.value ?: return
        if (index in proj.songs.indices) {
            playback.stop()
            _activeSongIndex.value = index
        }
    }

    fun addSong(name: String, bpm: Float) {
        updateActiveProject { proj ->
            val cleanName = name.trim().ifEmpty { "Nueva Canción ${proj.songs.size + 1}" }
            val newSong = SongProject(
                name = cleanName,
                slug = slugify(cleanName),
                bpm = bpm.coerceIn(50f, 240f),
                sections = createEmptySections(8, bpm.coerceIn(50f, 240f))
            )
            val updated = proj.copy(songs = proj.songs + newSong)
            _activeSongIndex.value = updated.songs.lastIndex
            updated
        }
    }

    fun updateModMetadata(
        name: String? = null,
        author: String? = null,
        description: String? = null,
        weekName: String? = null,
        difficulties: String? = null,
        targetEngine: TargetEngine? = null,
        packColorRgb: List<Int>? = null
    ) {
        updateActiveProject { proj ->
            val newName = name ?: proj.name
            proj.copy(
                name = newName,
                slug = slugify(newName),
                author = author ?: proj.author,
                description = description ?: proj.description,
                weekName = weekName ?: proj.weekName,
                difficulties = difficulties ?: proj.difficulties,
                targetEngine = targetEngine ?: proj.targetEngine,
                packColorRgb = packColorRgb ?: proj.packColorRgb
            )
        }
    }

    fun updateActiveSong(transform: (SongProject) -> SongProject) {
        updateActiveProject { proj ->
            val idx = _activeSongIndex.value.coerceIn(0, max(0, proj.songs.lastIndex))
            if (proj.songs.isEmpty()) return@updateActiveProject proj
            val updatedSongs = proj.songs.toMutableList()
            updatedSongs[idx] = transform(updatedSongs[idx])
            proj.copy(songs = updatedSongs)
        }
    }

    fun togglePlaySong(startMs: Long = 0L) {
        val song = currentSong() ?: return
        if (playback.isPlaying.value) {
            playback.stop()
        } else {
            playback.playSong(song, startMs)
        }
    }

    fun stopPlayback() {
        playback.stop()
    }

    fun currentSong(): SongProject? {
        val proj = _activeProject.value ?: return null
        return proj.songs.getOrNull(_activeSongIndex.value) ?: proj.songs.firstOrNull()
    }

    // --- Composer Actions ---
    fun toggleDrumStep(drumLane: Int, step: Int) {
        updateActiveSong { song ->
            val grid = song.composerData.drumGrid.toMutableMap()
            val list = grid[drumLane].orEmpty().toMutableList()
            val added = if (step in list) {
                list.remove(step)
                false
            } else {
                list.add(step)
                true
            }
            grid[drumLane] = list.sorted()
            if (added) playback.playOneShot(0, drumLane)
            song.copy(composerData = song.composerData.copy(drumGrid = grid))
        }
    }

    fun setMelodicStep(trackIndex: Int, step: Int, midiNote: Int?) {
        updateActiveSong { song ->
            val comp = song.composerData
            val updatedComp = when (trackIndex) {
                1 -> {
                    val m = comp.bassNotes.toMutableMap()
                    if (midiNote == null) m.remove(step) else m[step] = midiNote
                    comp.copy(bassNotes = m)
                }
                2 -> {
                    val m = comp.leadNotes.toMutableMap()
                    if (midiNote == null) m.remove(step) else m[step] = midiNote
                    comp.copy(leadNotes = m)
                }
                3 -> {
                    val m = comp.bfVocalNotes.toMutableMap()
                    if (midiNote == null) m.remove(step) else m[step] = midiNote
                    comp.copy(bfVocalNotes = m)
                }
                else -> {
                    val m = comp.dadVocalNotes.toMutableMap()
                    if (midiNote == null) m.remove(step) else m[step] = midiNote
                    comp.copy(dadVocalNotes = m)
                }
            }
            if (midiNote != null) playback.playOneShot(trackIndex, midiNote)
            song.copy(composerData = updatedComp)
        }
    }

    fun toggleTrackMute(trackIndex: Int) {
        updateActiveSong { song ->
            val mutes = song.composerData.trackMutes.toMutableList()
            while (mutes.size <= trackIndex) mutes.add(false)
            mutes[trackIndex] = !mutes[trackIndex]
            song.copy(composerData = song.composerData.copy(trackMutes = mutes))
        }
    }

    fun setComposerBars(bars: Int) {
        val targetBars = bars.coerceIn(2, 32)
        updateActiveSong { song ->
            val newSections = if (song.sections.size < targetBars) {
                song.sections + List(targetBars - song.sections.size) { idx ->
                    ChartSection(mustHitSection = (song.sections.size + idx) % 2 == 1, bpm = song.bpm)
                }
            } else {
                song.sections.take(targetBars)
            }
            song.copy(
                composerData = song.composerData.copy(barsCount = targetBars),
                sections = newSections
            )
        }
    }

    fun syncChartFromComposerVocals() {
        updateActiveSong { song ->
            val generated = ModRepository.buildSectionsFromComposer(song.composerData, song.bpm)
            song.copy(sections = generated)
        }
        showStatus("Chart sincronizado con las voces del compositor")
    }

    // --- Audio Import ---
    fun importAudioFile(uri: Uri, isVoices: Boolean) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            runCatching {
                val (path, name) = repo.importUriToProjectAsset(proj.id, uri, "audio")
                val analysis = AudioSynthEngine.analyzeImportedAudio(getApplication(), path)
                updateActiveSong { song ->
                    if (isVoices) {
                        song.copy(
                            audioSourceMode = AudioSourceMode.IMPORTED_AUDIO,
                            importedVoicesPath = path,
                            importedVoicesName = name
                        )
                    } else {
                        val neededSections = ((analysis.durationMs / (AudioSynthEngine.stepDurationMs(song.bpm) * 16)).toInt() + 1)
                            .coerceIn(4, 128)
                        val adjustedSections = if (song.sections.size < neededSections) {
                            song.sections + List(neededSections - song.sections.size) { idx ->
                                ChartSection(mustHitSection = (song.sections.size + idx) % 2 == 1, bpm = song.bpm)
                            }
                        } else song.sections

                        song.copy(
                            audioSourceMode = AudioSourceMode.IMPORTED_AUDIO,
                            importedInstPath = path,
                            importedInstName = name,
                            importedDurationMs = analysis.durationMs,
                            waveformPeaks = analysis.waveformPeaks,
                            sections = adjustedSections
                        )
                    }
                }
                showStatus(if (isVoices) "Voces importadas: $name" else "Instrumental importada: $name")
            }.onFailure {
                showStatus("Error al importar audio: ${it.message}")
            }
        }
    }

    // --- Chart Editor Actions ---
    fun toggleChartNote(sectionIndex: Int, stepInSection: Int, lane: Int, sustainSteps: Int = 0, noteType: String = "") {
        updateActiveSong { song ->
            if (sectionIndex !in song.sections.indices) return@updateActiveSong song
            val sections = song.sections.toMutableList()
            val sec = sections[sectionIndex]
            val existing = sec.notes.find { it.stepInSection == stepInSection && it.lane == lane }
            val updatedNotes = if (existing != null) {
                sec.notes.filterNot { it.id == existing.id }
            } else {
                playback.playOneShot(if (lane >= 4) 3 else 4, 56 + (lane % 4) * 3)
                sec.notes + ChartNote(
                    stepInSection = stepInSection,
                    lane = lane,
                    sustainSteps = sustainSteps,
                    noteType = noteType
                )
            }
            sections[sectionIndex] = sec.copy(notes = updatedNotes)
            song.copy(sections = sections)
        }
    }

    fun updateSectionProperties(sectionIndex: Int, mustHit: Boolean, gfSec: Boolean) {
        updateActiveSong { song ->
            if (sectionIndex !in song.sections.indices) return@updateActiveSong song
            val sections = song.sections.toMutableList()
            sections[sectionIndex] = sections[sectionIndex].copy(
                mustHitSection = mustHit,
                gfSection = gfSec
            )
            song.copy(sections = sections)
        }
    }

    fun addChartSection() {
        updateActiveSong { song ->
            val next = ChartSection(mustHitSection = song.sections.size % 2 == 1, bpm = song.bpm)
            song.copy(sections = song.sections + next)
        }
    }

    fun clearChartSection(sectionIndex: Int) {
        updateActiveSong { song ->
            if (sectionIndex !in song.sections.indices) return@updateActiveSong song
            val sections = song.sections.toMutableList()
            sections[sectionIndex] = sections[sectionIndex].copy(notes = emptyList())
            song.copy(sections = sections)
        }
    }

    // --- Sprites, Characters, Stage, NoteSkin ---
    fun addCharacter(displayName: String, role: String) {
        val clean = displayName.trim().ifEmpty { "Nuevo Personaje" }
        val newId = slugify(clean) + "-" + (System.currentTimeMillis() % 1000)
        val char = CharacterAsset(
            id = newId,
            displayName = clean,
            role = role,
            healthIconName = newId,
            flipX = role == "player"
        )
        updateActiveProject { proj ->
            proj.copy(characters = proj.characters + char)
        }
        showStatus("Personaje \"$clean\" agregado")
    }

    fun updateCharacter(charId: String, transform: (CharacterAsset) -> CharacterAsset) {
        updateActiveProject { proj ->
            val updated = proj.characters.map { if (it.id == charId) transform(it) else it }
            proj.copy(characters = updated)
        }
    }

    fun importCharacterFrames(charId: String, animKey: String, uris: List<Uri>) {
        val proj = _activeProject.value ?: return
        if (uris.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val paths = uris.map { uri ->
                    repo.importUriToProjectAsset(proj.id, uri, "sprites/${slugify(charId)}/$animKey").first
                }
                updateCharacter(charId) { char ->
                    val map = char.animFrames.toMutableMap()
                    map[animKey] = paths
                    char.copy(spriteMode = SpriteMode.IMPORTED_FRAMES, animFrames = map)
                }
                showStatus("${paths.size} frame(s) cargados para $animKey")
            }.onFailure {
                showStatus("Error importando frames: ${it.message}")
            }
        }
    }

    fun importCharacterAtlasFile(charId: String, uri: Uri, isXml: Boolean) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            runCatching {
                val (path, name) = repo.importUriToProjectAsset(proj.id, uri, "sprites/${slugify(charId)}")
                updateCharacter(charId) { char ->
                    if (isXml) {
                        char.copy(spriteMode = SpriteMode.IMPORTED_ATLAS, atlasXmlPath = path)
                    } else {
                        char.copy(spriteMode = SpriteMode.IMPORTED_ATLAS, atlasPngPath = path)
                    }
                }
                showStatus("Importado: $name")
            }.onFailure {
                showStatus("Error importando atlas: ${it.message}")
            }
        }
    }

    fun importCharacterIcon(charId: String, uri: Uri) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            runCatching {
                val (path, _) = repo.importUriToProjectAsset(proj.id, uri, "icons")
                updateCharacter(charId) { it.copy(iconPngPath = path) }
                showStatus("Ícono de vida actualizado")
            }
        }
    }

    fun updateStage(transform: (StageAsset) -> StageAsset) {
        updateActiveProject { proj ->
            val first = proj.stages.firstOrNull() ?: StageAsset()
            val updated = listOf(transform(first)) + proj.stages.drop(1)
            proj.copy(stages = updated)
        }
    }

    fun importStageLayer(layerType: String, uri: Uri) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            runCatching {
                val (path, _) = repo.importUriToProjectAsset(proj.id, uri, "stage")
                updateStage { st ->
                    when (layerType) {
                        "sky" -> st.copy(skyImagePath = path)
                        "ground" -> st.copy(groundImagePath = path)
                        else -> st.copy(fgImagePath = path)
                    }
                }
                showStatus("Imagen de escenario importada")
            }
        }
    }

    fun updateNoteSkin(transform: (NoteSkinAsset) -> NoteSkinAsset) {
        updateActiveProject { proj ->
            proj.copy(noteSkin = transform(proj.noteSkin))
        }
    }

    fun importCustomNoteArrow(lane: Int, uri: Uri) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            runCatching {
                val (path, _) = repo.importUriToProjectAsset(proj.id, uri, "noteskin")
                updateNoteSkin { skin ->
                    val map = skin.customArrowPaths.toMutableMap()
                    map[lane] = path
                    skin.copy(mode = NoteSkinMode.CUSTOM_ARROWS, customArrowPaths = map)
                }
                showStatus("Flecha ${lane + 1} actualizada")
            }
        }
    }

    fun importNoteAtlasFile(uri: Uri, isXml: Boolean) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            runCatching {
                val (path, name) = repo.importUriToProjectAsset(proj.id, uri, "noteskin")
                updateNoteSkin { skin ->
                    if (isXml) skin.copy(mode = NoteSkinMode.IMPORTED_ATLAS, atlasXmlPath = path)
                    else skin.copy(mode = NoteSkinMode.IMPORTED_ATLAS, atlasPngPath = path)
                }
                showStatus("NoteSkin $name importado")
            }
        }
    }

    fun importPackIcon(uri: Uri) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            runCatching {
                val (path, _) = repo.importUriToProjectAsset(proj.id, uri, "pack")
                updateActiveProject { it.copy(packIconPath = path) }
                showStatus("Ícono del Mod actualizado")
            }
        }
    }

    // --- Custom Note Types ---
    fun addCustomNoteType(
        name: String,
        shape: CustomNoteShape,
        colorHex: Long,
        ignoreNote: Boolean,
        noAnimation: Boolean,
        healthDelta: Float,
        missPenalty: Float,
        hitEffect: CustomNoteEffect,
        hitMessage: String
    ): CustomNoteType {
        val cleanName = name.trim().ifEmpty { "Nota Especial" }
        val existingSlugs = _activeProject.value?.customNoteTypes.orEmpty().map { it.slug }.toSet()
        var candidateSlug = cleanName.replace(Regex("[^a-zA-Z0-9 ]+"), "").trim().ifEmpty { "Custom Note" }
        if (candidateSlug in existingSlugs) {
            candidateSlug = "$candidateSlug ${(_activeProject.value?.customNoteTypes?.size ?: 1) + 1}"
        }
        val created = CustomNoteType(
            name = cleanName,
            slug = candidateSlug,
            colorHex = colorHex,
            shape = shape,
            ignoreNote = ignoreNote,
            noAnimation = noAnimation,
            healthDelta = healthDelta,
            missPenalty = missPenalty,
            hitEffect = hitEffect,
            hitMessage = hitMessage.trim()
        )
        updateActiveProject { proj ->
            proj.copy(customNoteTypes = proj.customNoteTypes + created)
        }
        showStatus("Nota \"$cleanName\" creada")
        return created
    }

    fun updateCustomNoteType(noteTypeId: String, transform: (CustomNoteType) -> CustomNoteType) {
        updateActiveProject { proj ->
            proj.copy(
                customNoteTypes = proj.customNoteTypes.map { if (it.id == noteTypeId) transform(it) else it }
            )
        }
    }

    fun deleteCustomNoteType(noteTypeId: String) {
        updateActiveProject { proj ->
            val target = proj.customNoteTypes.find { it.id == noteTypeId } ?: return@updateActiveProject proj
            val updatedSongs = proj.songs.map { song ->
                song.copy(
                    sections = song.sections.map { sec ->
                        sec.copy(
                            notes = sec.notes.map { note ->
                                if (note.noteType == target.slug) note.copy(noteType = "") else note
                            }
                        )
                    }
                )
            }
            proj.copy(
                customNoteTypes = proj.customNoteTypes.filterNot { it.id == noteTypeId },
                songs = updatedSongs
            )
        }
        showStatus("Tipo de nota eliminado")
    }

    fun importCustomNoteImage(noteTypeId: String, uri: Uri) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            runCatching {
                val (path, _) = repo.importUriToProjectAsset(proj.id, uri, "custom_notes")
                updateCustomNoteType(noteTypeId) { it.copy(customPngPath = path) }
                showStatus("Sprite de nota personalizada cargado")
            }.onFailure {
                showStatus("Error al cargar imagen de nota: ${it.message}")
            }
        }
    }

    // --- Scratch Events ---
    fun addScratchBlock(trigger: ScratchTriggerType, action: ScratchActionType) {
        val block = ScratchBlock(
            triggerType = trigger,
            triggerValue = if (trigger == ScratchTriggerType.ON_BEAT) 8 else 4,
            actionType = action
        )
        updateActiveSong { song ->
            song.copy(scratchBlocks = song.scratchBlocks + block)
        }
    }

    fun updateScratchBlock(blockId: String, transform: (ScratchBlock) -> ScratchBlock) {
        updateActiveSong { song ->
            song.copy(scratchBlocks = song.scratchBlocks.map { if (it.id == blockId) transform(it) else it })
        }
    }

    fun moveScratchBlock(blockId: String, delta: Int) {
        updateActiveSong { song ->
            val list = song.scratchBlocks.toMutableList()
            val idx = list.indexOfFirst { it.id == blockId }
            val target = idx + delta
            if (idx >= 0 && target in list.indices) {
                val item = list.removeAt(idx)
                list.add(target, item)
            }
            song.copy(scratchBlocks = list)
        }
    }

    fun deleteScratchBlock(blockId: String) {
        updateActiveSong { song ->
            song.copy(scratchBlocks = song.scratchBlocks.filterNot { it.id == blockId })
        }
    }

    // --- Export ZIP ---
    fun exportZipToUri(uri: Uri) {
        val proj = _activeProject.value ?: return
        if (_isExporting.value) return
        viewModelScope.launch {
            _isExporting.value = true
            _exportProgress.value = "Preparando exportación..."
            val result = PsychModExporter.exportModZipToUri(getApplication(), proj, uri) { step ->
                _exportProgress.value = step
            }
            _isExporting.value = false
            _exportProgress.value = ""
            result.onSuccess { fileName ->
                showStatus("¡ZIP exportado sin errores! ($fileName)")
            }.onFailure { err ->
                showStatus("Error al exportar ZIP: ${err.message}")
            }
        }
    }

    fun shareModZip(onShareIntent: (Intent) -> Unit) {
        val proj = _activeProject.value ?: return
        if (_isExporting.value) return
        viewModelScope.launch {
            _isExporting.value = true
            _exportProgress.value = "Empaquetando mod..."
            val result = PsychModExporter.exportModZipToShareFile(getApplication(), proj) { step ->
                _exportProgress.value = step
            }
            _isExporting.value = false
            _exportProgress.value = ""
            result.onSuccess { zipFile ->
                val app = getApplication<Application>()
                val contentUri = FileProvider.getUriForFile(
                    app,
                    "${app.packageName}.fileprovider",
                    zipFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "${proj.name} (${proj.targetEngine.label})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                onShareIntent(Intent.createChooser(intent, "Compartir Mod ZIP"))
            }.onFailure {
                showStatus("Error preparando ZIP: ${it.message}")
            }
        }
    }

    private fun refreshValidation(project: ModProject) {
        _validationReport.value = PsychModExporter.validateMod(project)
    }

    private fun updateActiveProject(transform: (ModProject) -> ModProject) {
        val current = _activeProject.value ?: return
        val updated = transform(current)
        _activeProject.value = updated
        _projects.value = _projects.value.map { if (it.id == updated.id) updated else it }
        refreshValidation(updated)
        scheduleAutoSave(updated)
    }

    private fun scheduleAutoSave(project: ModProject) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(250L)
            repo.saveProject(project)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playback.stop()
    }
}
