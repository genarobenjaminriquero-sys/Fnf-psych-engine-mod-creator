package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import com.antonkarpenko.ffmpegkit.FFmpegKit
import com.antonkarpenko.ffmpegkit.ReturnCode
import com.example.model.AudioSourceMode
import com.example.model.CharacterAsset
import com.example.model.CustomNoteLuaCompiler
import com.example.model.CustomNoteShape
import com.example.model.ModProject
import com.example.model.NoteSkinMode
import com.example.model.ScratchActionType
import com.example.model.ScratchLuaCompiler
import com.example.model.ScratchTriggerType
import com.example.model.SongProject
import com.example.model.SpriteMode
import com.example.model.StageAsset
import com.example.model.TargetEngine
import com.example.model.slugify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ValidationCheck(
    val title: String,
    val detail: String,
    val passed: Boolean,
    val isWarning: Boolean = false
)

data class ModValidationReport(
    val checks: List<ValidationCheck>,
    val canExport: Boolean
)

object PsychModExporter {

    private val prettyJson = Json { prettyPrint = true }

    fun validateMod(project: ModProject): ModValidationReport {
        val checks = mutableListOf<ValidationCheck>()

        checks += ValidationCheck(
            title = "Nombre y metadatos del Mod",
            detail = "${project.name} (${project.targetEngine.label})",
            passed = project.name.isNotBlank() && project.slug.isNotBlank()
        )

        val hasSongs = project.songs.isNotEmpty()
        checks += ValidationCheck(
            title = "Canciones configuradas",
            detail = if (hasSongs) "${project.songs.size} canción(es) listas" else "Agrega al menos 1 canción",
            passed = hasSongs
        )

        project.songs.forEach { song ->
            val audioReady = when (song.audioSourceMode) {
                AudioSourceMode.SYNTH_COMPOSER -> true
                AudioSourceMode.IMPORTED_AUDIO -> !song.importedInstPath.isNullOrBlank() && File(song.importedInstPath).exists()
            }
            checks += ValidationCheck(
                title = "Audio de \"${song.name}\"",
                detail = if (song.audioSourceMode == AudioSourceMode.SYNTH_COMPOSER) {
                    "Sintetizador nativo (${song.composerData.barsCount} compases) → Inst.ogg + Voices.ogg"
                } else if (audioReady) {
                    "Audio importado (${song.importedInstName ?: "Inst"}) → Ogg Vorbis"
                } else {
                    "Falta seleccionar el archivo Inst desde el celular"
                },
                passed = audioReady
            )

            val noteCount = song.sections.sumOf { it.notes.size }
            checks += ValidationCheck(
                title = "Chart de \"${song.name}\"",
                detail = if (noteCount > 0) "$noteCount notas en ${song.sections.size} secciones" else "Sin notas todavía (se exportará vacío)",
                passed = true,
                isWarning = noteCount == 0
            )
        }

        val charIds = project.characters.map { it.id }.toSet()
        val missingCharRefs = project.songs.flatMap { listOf(it.player1, it.player2, it.gfVersion) }
            .filter { it !in charIds }
        checks += ValidationCheck(
            title = "Personajes y Sprites (${project.characters.size})",
            detail = if (missingCharRefs.isEmpty()) {
                "JSON, PNG Atlas, Sparrow XML e Íconos listos"
            } else {
                "Faltan personajes: ${missingCharRefs.distinct().joinToString()}"
            },
            passed = missingCharRefs.isEmpty()
        )

        checks += ValidationCheck(
            title = "Escenarios (${project.stages.size})",
            detail = "Stage JSON + Script Lua + Capas PNG listos",
            passed = project.stages.isNotEmpty()
        )

        val eventCount = project.songs.sumOf { it.scratchBlocks.count { b -> b.enabled } }
        checks += ValidationCheck(
            title = "Eventos Scratch → Lua",
            detail = if (eventCount > 0) "$eventCount bloque(s) compilados con precarga automática" else "Sin eventos extra (script limpio)",
            passed = true
        )

        checks += ValidationCheck(
            title = "Notas Personalizadas (${project.customNoteTypes.size})",
            detail = "Scripts custom_notetypes/*.lua y sprites listos",
            passed = true
        )

        val canExport = checks.all { it.passed }
        return ModValidationReport(checks, canExport)
    }

    suspend fun exportModZipToUri(
        context: Context,
        project: ModProject,
        destinationUri: Uri,
        onProgress: (String) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val tempZip = File(context.cacheDir, "exports/${project.slug}_temp.zip").apply {
                parentFile?.mkdirs()
                delete()
            }
            buildModZip(context, project, tempZip, onProgress)
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                FileInputStream(tempZip).use { inp -> inp.copyTo(out) }
            } ?: error("No se pudo abrir la ubicación de destino.")
            tempZip.delete()
            "${project.slug}.zip"
        }
    }

    suspend fun exportModZipToShareFile(
        context: Context,
        project: ModProject,
        onProgress: (String) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val outZip = File(context.cacheDir, "exports/${project.slug}-${project.targetEngine.versionTag}.zip").apply {
                parentFile?.mkdirs()
                delete()
            }
            buildModZip(context, project, outZip, onProgress)
            outZip
        }
    }

    suspend fun buildModZip(
        context: Context,
        project: ModProject,
        outputZipFile: File,
        onProgress: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "mod_build_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            val modSlug = slugify(project.slug.ifBlank { project.name })
            val modRoot = File(workDir, modSlug).apply { mkdirs() }

            onProgress("Escribiendo pack.json e ícono...")
            File(modRoot, "pack.json").writeText(buildPackJson(project))
            AtlasPacker.buildPackIcon(project.packIconPath, project.packColorRgb, File(modRoot, "pack.png"))

            // Characters + Icons
            onProgress("Empaquetando spritesheets PNG y Sparrow XML...")
            val charsDir = File(modRoot, "characters").apply { mkdirs() }
            val charImgDir = File(modRoot, "images/characters").apply { mkdirs() }
            val iconsDir = File(modRoot, "images/icons").apply { mkdirs() }

            project.characters.forEach { char ->
                val safeId = slugify(char.id)
                val atlasResult = AtlasPacker.buildCharacterAtlas(char, charImgDir, safeId)
                val detectedPrefixes = parseXmlPrefixes(atlasResult.xmlFile)
                File(charsDir, "$safeId.json").writeText(buildCharacterJson(char, safeId, detectedPrefixes))
                AtlasPacker.buildHealthIcon(char, File(iconsDir, "icon-${slugify(char.healthIconName)}.png"))
            }

            // Stages
            onProgress("Generando escenarios...")
            val stagesDir = File(modRoot, "stages").apply { mkdirs() }
            project.stages.forEach { stage ->
                val stageSlug = slugify(stage.id)
                val stageImgDir = File(modRoot, "images/stages/$stageSlug").apply { mkdirs() }
                exportStageAssets(stage, stageSlug, stagesDir, stageImgDir)
            }

            // Custom Note Skins
            if (project.noteSkin.mode != NoteSkinMode.DEFAULT) {
                onProgress("Generando NOTE_assets personalizadas...")
                val imagesDir = File(modRoot, "images").apply { mkdirs() }
                AtlasPacker.buildNoteSkinAtlas(project.noteSkin, imagesDir)
                if (project.targetEngine == TargetEngine.PSYCH_1_0_4) {
                    val skinsDir = File(modRoot, "images/noteSkins").apply { mkdirs() }
                    AtlasPacker.buildNoteSkinAtlas(project.noteSkin, skinsDir)
                }
            }

            // Custom Note Types (custom_notetypes/*.lua + images/customNotes/*)
            if (project.customNoteTypes.isNotEmpty()) {
                onProgress("Compilando notas personalizadas...")
                val customNoteDir = File(modRoot, "custom_notetypes").apply { mkdirs() }
                val customNoteImgDir = File(modRoot, "images/customNotes").apply { mkdirs() }
                project.customNoteTypes.forEach { noteType ->
                    val safeSlug = noteType.slug.replace("/", "").replace("\\", "").trim()
                    if (safeSlug.isNotEmpty()) {
                        File(customNoteDir, "$safeSlug.lua").writeText(CustomNoteLuaCompiler.compileToLua(noteType))
                        if (!noteType.customPngPath.isNullOrBlank() || noteType.shape != CustomNoteShape.ARROW) {
                            AtlasPacker.buildCustomNoteAtlas(noteType, customNoteImgDir, slugify(safeSlug))
                        }
                    }
                }
            }

            // Songs, Charts, Audio (.ogg), Lua Scripts
            val diffList = parseDifficulties(project.difficulties)
            project.songs.forEach { song ->
                val songSlug = slugify(song.slug.ifBlank { song.name })
                val songAudioDir = File(modRoot, "songs/$songSlug").apply { mkdirs() }
                val songDataDir = File(modRoot, "data/$songSlug").apply { mkdirs() }

                onProgress("Codificando audio OGG de ${song.name}...")
                val hasVoices = exportSongAudioToOgg(song, songAudioDir, workDir)

                onProgress("Compilando chart (${project.targetEngine.versionTag}) y eventos Scratch...")
                val chartJson = buildChartJson(song, project.targetEngine, hasVoices)
                // Base chart + all difficulty variants so Story/Freeplay never throws missing JSON
                File(songDataDir, "$songSlug.json").writeText(chartJson)
                diffList.forEach { diff ->
                    val diffSlug = slugify(diff)
                    if (diffSlug != "normal") {
                        File(songDataDir, "$songSlug-$diffSlug.json").writeText(chartJson)
                    }
                }

                // Lua script from Scratch blocks
                val luaCode = ScratchLuaCompiler.compileToLua(song.scratchBlocks, song.name)
                File(songDataDir, "script.lua").writeText(luaCode)
            }

            // Week JSON
            onProgress("Generando semana y menú...")
            val weeksDir = File(modRoot, "weeks").apply { mkdirs() }
            val weekSlug = slugify(project.weekName.ifBlank { modSlug })
            File(weeksDir, "$weekSlug.json").writeText(buildWeekJson(project, weekSlug))

            // Optional Week Story Menu banner
            if (!project.weekBannerPath.isNullOrBlank() && File(project.weekBannerPath).exists()) {
                val menuBgDir = File(modRoot, "images/menubackgrounds").apply { mkdirs() }
                convertOrCopyPng(File(project.weekBannerPath), File(menuBgDir, "menu_${slugify(project.stages.firstOrNull()?.id ?: "stage")}.png"))
            }

            onProgress("Comprimiendo archivo .zip...")
            zipDirectory(modRoot, modSlug, outputZipFile)
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun exportSongAudioToOgg(song: SongProject, outDir: File, workDir: File): Boolean {
        val instOut = File(outDir, "Inst.ogg")
        val voicesOut = File(outDir, "Voices.ogg")

        if (song.audioSourceMode == AudioSourceMode.IMPORTED_AUDIO && !song.importedInstPath.isNullOrBlank()) {
            val srcInst = File(song.importedInstPath)
            if (srcInst.exists()) {
                convertAudioFileToOgg(srcInst, instOut)
            }
            val srcVoices = song.importedVoicesPath?.let(::File)?.takeIf { it.exists() }
            return if (srcVoices != null) {
                convertAudioFileToOgg(srcVoices, voicesOut)
                true
            } else {
                false
            }
        } else {
            val stems = AudioSynthEngine.renderComposerStems(song.composerData, song.bpm)
            val instWav = File(workDir, "${song.id}_inst.wav")
            val voicesWav = File(workDir, "${song.id}_voices.wav")
            AudioSynthEngine.writeWavFile(stems.instPcm, instWav)
            AudioSynthEngine.writeWavFile(stems.voicesPcm, voicesWav)
            convertAudioFileToOgg(instWav, instOut)
            convertAudioFileToOgg(voicesWav, voicesOut)
            return true
        }
    }

    private fun convertAudioFileToOgg(input: File, outputOgg: File) {
        if (isValidOggFile(input)) {
            input.copyTo(outputOgg, overwrite = true)
            return
        }
        val converted = runCatching {
            val cmd = "-y -i \"${input.absolutePath}\" -vn -ar 44100 -ac 2 -c:a libvorbis -q:a 5 \"${outputOgg.absolutePath}\""
            val session = FFmpegKit.execute(cmd)
            ReturnCode.isSuccess(session.returnCode) && outputOgg.exists() && outputOgg.length() > 0L
        }.getOrDefault(false)

        if (!converted) {
            input.copyTo(outputOgg, overwrite = true)
        }
    }

    private fun isValidOggFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return runCatching {
            FileInputStream(file).use { fis ->
                val magic = ByteArray(4)
                fis.read(magic) == 4 && String(magic) == "OggS"
            }
        }.getOrDefault(false)
    }

    fun buildPackJson(project: ModProject): String {
        val colorList = project.packColorRgb.take(3).ifEmpty { listOf(0, 229, 255) }
        val obj = buildJsonObject {
            put("name", project.name)
            put("description", project.description)
            put("restart", false)
            put("runsGlobally", false)
            put("color", buildJsonArray { colorList.forEach { add(it) } })
        }
        return prettyJson.encodeToString(obj)
    }

    fun buildWeekJson(project: ModProject, weekSlug: String): String {
        val firstSongs = project.songs.ifEmpty { listOf(SongProject()) }
        val firstSong = firstSongs.first()
        val hasCustomMenuBg = !project.weekBannerPath.isNullOrBlank() && File(project.weekBannerPath).exists()
        val stageBg = if (hasCustomMenuBg) slugify(project.stages.firstOrNull()?.id ?: "stage") else "stage"

        val obj = buildJsonObject {
            put("songs", buildJsonArray {
                firstSongs.forEach { song ->
                    val oppChar = project.characters.find { it.id == song.player2 }
                    val icon = slugify(oppChar?.healthIconName ?: song.player2)
                    val color = oppChar?.healthBarRgb?.take(3) ?: project.packColorRgb.take(3)
                    addJsonArray {
                        add(song.name)
                        add(icon)
                        addJsonArray { color.forEach { add(it) } }
                    }
                }
            })
            put("weekCharacters", buildJsonArray {
                add(slugify(firstSong.player2))
                add(slugify(firstSong.player1))
                add(slugify(firstSong.gfVersion))
            })
            put("weekBackground", stageBg)
            put("weekBefore", "tutorial")
            put("storyName", project.weekName)
            put("weekName", weekSlug)
            put("startUnlocked", true)
            put("hiddenUntilUnlocked", false)
            put("hideStoryMode", false)
            put("hideFreeplay", false)
            put("difficulties", project.difficulties)
        }
        return prettyJson.encodeToString(obj)
    }

    fun buildCharacterJson(
        char: CharacterAsset,
        imageBaseName: String,
        detectedPrefixes: Map<String, String>
    ): String {
        val animList = listOf(
            "idle" to "idle",
            "singLEFT" to "left",
            "singDOWN" to "down",
            "singUP" to "up",
            "singRIGHT" to "right"
        )
        val hbColor = char.healthBarRgb.take(3).ifEmpty { listOf(161, 161, 161) }

        val obj = buildJsonObject {
            put("animations", buildJsonArray {
                animList.forEach { (animKey, defaultPrefix) ->
                    val xmlPrefix = detectedPrefixes[animKey] ?: defaultPrefix
                    addJsonObject {
                        put("anim", animKey)
                        put("name", xmlPrefix)
                        put("fps", char.animFps.coerceIn(12, 60))
                        put("loop", false)
                        put("indices", buildJsonArray {})
                        put("offsets", buildJsonArray { add(0); add(0) })
                    }
                }
            })
            put("no_antialiasing", char.noAntialiasing)
            put("image", "characters/$imageBaseName")
            put("position", buildJsonArray { add(char.positionX); add(char.positionY) })
            put("healthicon", slugify(char.healthIconName))
            put("flip_x", char.flipX)
            put("healthbar_colors", buildJsonArray { hbColor.forEach { add(it) } })
            put("camera_position", buildJsonArray { add(char.cameraX); add(char.cameraY) })
            put("sing_duration", char.singDuration.toDouble())
            put("scale", char.scale.toDouble())
        }
        return prettyJson.encodeToString(obj)
    }

    fun buildChartJson(song: SongProject, targetEngine: TargetEngine, needsVoices: Boolean): String {
        val stepMs = AudioSynthEngine.stepDurationMs(song.bpm)
        val beatMs = 60_000.0 / song.bpm.coerceIn(40f, 300f)

        val innerSong = buildJsonObject {
            put("song", song.name)
            put("bpm", song.bpm.toDouble())
            put("needsVoices", needsVoices)
            put("speed", song.speed.toDouble())
            put("player1", slugify(song.player1))
            put("player2", slugify(song.player2))
            put("gfVersion", slugify(song.gfVersion))
            put("stage", slugify(song.stage))
            put("validScore", true)
            if (targetEngine == TargetEngine.PSYCH_1_0_4) {
                put("format", "psych_v1")
            }
            put("notes", buildJsonArray {
                song.sections.forEachIndexed { secIdx, sec ->
                    val secStartMs = secIdx * 16 * stepMs
                    addJsonObject {
                        put("sectionNotes", buildJsonArray {
                            sec.notes.sortedBy { it.stepInSection }.forEach { note ->
                                val strumTime = secStartMs + note.stepInSection * stepMs
                                val sustainMs = note.sustainSteps * stepMs
                                val mappedLane = when (targetEngine) {
                                    TargetEngine.PSYCH_1_0_4 -> if (note.lane in 4..7) note.lane - 4 else note.lane + 4
                                    TargetEngine.PSYCH_0_7_3 -> if (sec.mustHitSection) {
                                        if (note.lane in 4..7) note.lane - 4 else note.lane + 4
                                    } else {
                                        note.lane
                                    }
                                }
                                addJsonArray {
                                    add(strumTime)
                                    add(mappedLane)
                                    add(sustainMs)
                                    if (note.noteType.isNotBlank()) add(note.noteType)
                                }
                            }
                        })
                        put("sectionBeats", sec.sectionBeats)
                        put("lengthInSteps", 16)
                        put("mustHitSection", sec.mustHitSection)
                        put("gfSection", sec.gfSection)
                        put("bpm", sec.bpm.toDouble())
                        put("changeBPM", sec.changeBpm)
                        put("altAnim", false)
                    }
                }
            })
            put("events", buildJsonArray {
                song.scratchBlocks.filter { it.enabled }.forEach { block ->
                    val timeMs: Double? = when (block.triggerType) {
                        ScratchTriggerType.ON_SONG_START -> 0.0
                        ScratchTriggerType.ON_BEAT -> block.triggerValue * beatMs
                        ScratchTriggerType.ON_STEP -> block.triggerValue * stepMs
                        else -> null
                    }
                    if (timeMs != null && block.actionType == ScratchActionType.CHANGE_CHARACTER) {
                        addJsonArray {
                            add(timeMs)
                            addJsonArray {
                                addJsonArray {
                                    add("Change Character")
                                    add(block.param1)
                                    add(slugify(block.param2))
                                }
                            }
                        }
                    }
                }
            })
        }

        val root: JsonObject = when (targetEngine) {
            TargetEngine.PSYCH_1_0_4 -> innerSong
            TargetEngine.PSYCH_0_7_3 -> buildJsonObject { put("song", innerSong) }
        }
        return prettyJson.encodeToString(root)
    }

    private fun exportStageAssets(
        stage: StageAsset,
        stageSlug: String,
        stagesDir: File,
        stageImgDir: File
    ) {
        val stageObj = buildJsonObject {
            put("directory", "")
            put("defaultZoom", stage.defaultZoom.toDouble())
            put("isPixelStage", false)
            put("boyfriend", buildJsonArray { stage.bfPos.forEach { add(it) } })
            put("girlfriend", buildJsonArray { stage.gfPos.forEach { add(it) } })
            put("opponent", buildJsonArray { stage.dadPos.forEach { add(it) } })
        }
        File(stagesDir, "$stageSlug.json").writeText(prettyJson.encodeToString(stageObj))

        val skyOut = File(stageImgDir, "sky.png")
        val floorOut = File(stageImgDir, "floor.png")
        exportOrGenerateLayer(stage.skyImagePath, skyOut, stage.bgColorHex.toInt(), 1280, 720)
        exportOrGenerateLayer(stage.groundImagePath, floorOut, stage.floorColorHex.toInt(), 1400, 320)

        val lua = buildString {
            appendLine("function onCreate()")
            appendLine("    makeLuaSprite('fcStageSky', 'stages/$stageSlug/sky', -500, -300)")
            appendLine("    setScrollFactor('fcStageSky', 0.4, 0.4)")
            appendLine("    scaleObject('fcStageSky', 1.5, 1.5)")
            appendLine("    addLuaSprite('fcStageSky', false)")
            appendLine()
            appendLine("    makeLuaSprite('fcStageFloor', 'stages/$stageSlug/floor', -600, 560)")
            appendLine("    setScrollFactor('fcStageFloor', 1.0, 1.0)")
            appendLine("    scaleObject('fcStageFloor', 1.6, 1.2)")
            appendLine("    addLuaSprite('fcStageFloor', false)")
            if (!stage.fgImagePath.isNullOrBlank() && File(stage.fgImagePath).exists()) {
                val fgOut = File(stageImgDir, "fg.png")
                convertOrCopyPng(File(stage.fgImagePath), fgOut)
                appendLine()
                appendLine("    makeLuaSprite('fcStageFg', 'stages/$stageSlug/fg', -450, -150)")
                appendLine("    setScrollFactor('fcStageFg', 1.2, 1.2)")
                appendLine("    addLuaSprite('fcStageFg', true)")
            }
            appendLine("end")
        }
        File(stagesDir, "$stageSlug.lua").writeText(lua)
    }

    private fun exportOrGenerateLayer(srcPath: String?, outFile: File, colorInt: Int, w: Int, h: Int) {
        if (!srcPath.isNullOrBlank() && File(srcPath).exists()) {
            convertOrCopyPng(File(srcPath), outFile)
            return
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(colorInt)
        FileOutputStream(outFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
    }

    private fun convertOrCopyPng(src: File, dst: File) {
        dst.parentFile?.mkdirs()
        val bmp = BitmapFactory.decodeFile(src.absolutePath)
        if (bmp != null) {
            FileOutputStream(dst).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
        } else {
            src.copyTo(dst, overwrite = true)
        }
    }

    private fun parseXmlPrefixes(xmlFile: File): Map<String, String> {
        if (!xmlFile.exists()) return emptyMap()
        val text = xmlFile.readText()
        val names = Regex("""name="([^"]+)"""").findAll(text).map { it.groupValues[1] }.toList()
        if (names.isEmpty()) return emptyMap()

        fun findMatch(keywords: List<String>, fallback: String): String {
            val found = names.firstOrNull { n -> keywords.any { k -> n.contains(k, ignoreCase = true) } }
            return found?.replace(Regex("""\d{4}$"""), "") ?: fallback
        }

        return mapOf(
            "idle" to findMatch(listOf("idle", "dance"), "idle"),
            "singLEFT" to findMatch(listOf("left"), "left"),
            "singDOWN" to findMatch(listOf("down"), "down"),
            "singUP" to findMatch(listOf("up"), "up"),
            "singRIGHT" to findMatch(listOf("right"), "right")
        )
    }

    private fun parseDifficulties(raw: String): List<String> =
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Normal") }

    private fun zipDirectory(sourceDir: File, rootEntryName: String, outZip: File) {
        outZip.parentFile?.mkdirs()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outZip))).use { zos ->
            addDirToZip(zos, sourceDir, rootEntryName)
        }
    }

    private fun addDirToZip(zos: ZipOutputStream, dir: File, pathPrefix: String) {
        val children = dir.listFiles() ?: return
        for (child in children) {
            val entryName = "$pathPrefix/${child.name}"
            if (child.isDirectory) {
                addDirToZip(zos, child, entryName)
            } else {
                val entry = ZipEntry(entryName)
                zos.putNextEntry(entry)
                BufferedInputStream(FileInputStream(child)).use { bis ->
                    bis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
    }
}
