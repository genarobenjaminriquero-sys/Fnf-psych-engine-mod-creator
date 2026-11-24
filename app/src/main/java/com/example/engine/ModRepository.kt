package com.example.engine

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.ChartNote
import com.example.model.ChartSection
import com.example.model.ComposerTrackData
import com.example.model.ModProject
import com.example.model.ScratchActionType
import com.example.model.ScratchBlock
import com.example.model.ScratchTriggerType
import com.example.model.SongProject
import com.example.model.TargetEngine
import com.example.model.slugify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

class ModRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val modsRootDir: File
        get() = File(context.filesDir, "mods").apply { mkdirs() }

    suspend fun loadAllProjects(): List<ModProject> = withContext(Dispatchers.IO) {
        val dirs = modsRootDir.listFiles()?.filter { it.isDirectory }.orEmpty()
        val loaded = dirs.mapNotNull { dir ->
            val file = File(dir, "project.json")
            if (file.exists()) {
                runCatching { json.decodeFromString<ModProject>(file.readText()) }.getOrNull()
            } else null
        }.sortedByDescending { it.updatedAt }

        if (loaded.isEmpty()) {
            val starter = createStarterTemplateProject()
            saveProject(starter)
            listOf(starter)
        } else {
            loaded
        }
    }

    suspend fun saveProject(project: ModProject): ModProject = withContext(Dispatchers.IO) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        val dir = File(modsRootDir, updated.id).apply { mkdirs() }
        File(dir, "project.json").writeText(json.encodeToString(updated))
        updated
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        File(modsRootDir, projectId).deleteRecursively()
    }

    suspend fun importUriToProjectAsset(
        projectId: String,
        uri: Uri,
        subFolder: String,
        preferredFileName: String? = null
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val displayName = preferredFileName ?: resolveDisplayName(uri) ?: "asset_${System.currentTimeMillis()}"
        val safeName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val destDir = File(modsRootDir, "$projectId/assets/$subFolder").apply { mkdirs() }
        val destFile = File(destDir, "${System.currentTimeMillis()}_$safeName")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: error("No se pudo leer el archivo seleccionado")

        destFile.absolutePath to displayName
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment
    }

    fun createNewBlankProject(name: String, author: String, engine: TargetEngine): ModProject {
        val slug = slugify(name)
        val song = SongProject(
            name = "$name Song",
            slug = slugify("$name-song"),
            bpm = 120f,
            speed = 2.2f
        )
        return ModProject(
            name = name,
            slug = slug,
            author = author.ifBlank { "Creador" },
            targetEngine = engine,
            songs = listOf(song)
        )
    }

    private fun createStarterTemplateProject(): ModProject {
        val composer = ComposerTrackData.createDefault()
        val sections = buildSectionsFromComposer(composer, 128f)
        val starterSong = SongProject(
            name = "Neon Pulse",
            slug = "neon-pulse",
            bpm = 128f,
            speed = 2.5f,
            sections = sections,
            scratchBlocks = listOf(
                ScratchBlock(
                    triggerType = ScratchTriggerType.EVERY_N_BEATS,
                    triggerValue = 4,
                    actionType = ScratchActionType.CAMERA_ZOOM,
                    param1 = "1.05",
                    param2 = "0.035"
                ),
                ScratchBlock(
                    triggerType = ScratchTriggerType.ON_BEAT,
                    triggerValue = 16,
                    actionType = ScratchActionType.SPIN_ARROWS,
                    param1 = "all",
                    param2 = "360",
                    param3 = "0.4"
                ),
                ScratchBlock(
                    triggerType = ScratchTriggerType.ON_BEAT,
                    triggerValue = 16,
                    actionType = ScratchActionType.SHOW_FLOATING_TEXT,
                    param1 = "¡DROP!",
                    param2 = "1.4"
                )
            )
        )
        return ModProject(
            name = "Neon Pulse Demo",
            slug = "neon-pulse-demo",
            author = "FunkinCraft",
            description = "Mod de demostración editable con canción nativa, chart y eventos Scratch.",
            weekName = "Neon Week",
            targetEngine = TargetEngine.PSYCH_1_0_4,
            songs = listOf(starterSong)
        )
    }

    companion object {
        fun buildSectionsFromComposer(composer: ComposerTrackData, bpm: Float): List<ChartSection> {
            return (0 until composer.barsCount).map { bar ->
                val base = bar * 16
                val notes = mutableListOf<ChartNote>()
                val isPlayerBar = bar % 2 == 1

                for (step in 0 until 16) {
                    val globalStep = base + step
                    val bfMidi = composer.bfVocalNotes[globalStep]
                    if (bfMidi != null) {
                        val lane = 4 + ((bfMidi - 48).coerceAtLeast(0) % 4)
                        notes += ChartNote(stepInSection = step, lane = lane, sustainSteps = 0)
                    }
                    val dadMidi = composer.dadVocalNotes[globalStep]
                    if (dadMidi != null) {
                        val lane = (dadMidi - 48).coerceAtLeast(0) % 4
                        notes += ChartNote(stepInSection = step, lane = lane, sustainSteps = 0)
                    }
                }
                ChartSection(
                    mustHitSection = isPlayerBar,
                    bpm = bpm,
                    notes = notes
                )
            }
        }
    }
}
