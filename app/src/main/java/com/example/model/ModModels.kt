package com.example.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class TargetEngine(val label: String, val versionTag: String) {
    PSYCH_1_0_4("Psych Engine 1.0.4", "1.0.4"),
    PSYCH_0_7_3("Psych Engine 0.7.3", "0.7.3")
}

@Serializable
enum class AudioSourceMode {
    SYNTH_COMPOSER,
    IMPORTED_AUDIO
}

@Serializable
enum class SpriteMode {
    BUILTIN_VECTOR,
    IMPORTED_FRAMES,
    IMPORTED_ATLAS
}

@Serializable
enum class NoteSkinMode {
    DEFAULT,
    CUSTOM_COLORS,
    CUSTOM_ARROWS,
    IMPORTED_ATLAS
}

@Serializable
data class ModProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val slug: String,
    val author: String = "Creador",
    val description: String = "Mod creado con FunkinCraft para Psych Engine",
    val weekName: String = "Semana 1",
    val difficulties: String = "Easy, Normal, Hard",
    val targetEngine: TargetEngine = TargetEngine.PSYCH_1_0_4,
    val packColorRgb: List<Int> = listOf(0, 229, 255),
    val packIconPath: String? = null,
    val weekBannerPath: String? = null,
    val songs: List<SongProject> = emptyList(),
    val characters: List<CharacterAsset> = defaultCharacters(),
    val stages: List<StageAsset> = listOf(defaultStage()),
    val noteSkin: NoteSkinAsset = NoteSkinAsset(),
    val customNoteTypes: List<CustomNoteType> = defaultCustomNoteTypes(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class SongProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Neon Beat",
    val slug: String = "neon-beat",
    val bpm: Float = 128f,
    val speed: Float = 2.4f,
    val player1: String = "bf",
    val player2: String = "rival",
    val gfVersion: String = "gf",
    val stage: String = "neon-stage",
    val audioSourceMode: AudioSourceMode = AudioSourceMode.SYNTH_COMPOSER,
    val importedInstPath: String? = null,
    val importedInstName: String? = null,
    val importedVoicesPath: String? = null,
    val importedVoicesName: String? = null,
    val importedDurationMs: Long = 30000L,
    val waveformPeaks: List<Float> = emptyList(),
    val composerData: ComposerTrackData = ComposerTrackData.createDefault(),
    val sections: List<ChartSection> = createEmptySections(8, 128f),
    val scratchBlocks: List<ScratchBlock> = emptyList()
)

@Serializable
data class ComposerTrackData(
    val barsCount: Int = 8,
    // Drum lanes: 0 = Kick, 1 = Snare, 2 = HiHat, 3 = Clap; each step is index 0 until barsCount*16
    val drumGrid: Map<Int, List<Int>> = emptyMap(),
    // Melodic tracks: map stepIndex -> midiNote (e.g. 36..84)
    val bassNotes: Map<Int, Int> = emptyMap(),
    val leadNotes: Map<Int, Int> = emptyMap(),
    val bfVocalNotes: Map<Int, Int> = emptyMap(),
    val dadVocalNotes: Map<Int, Int> = emptyMap(),
    val trackVolumes: List<Float> = listOf(0.95f, 0.80f, 0.70f, 0.90f, 0.90f),
    val trackMutes: List<Boolean> = listOf(false, false, false, false, false)
) {
    val totalSteps: Int get() = barsCount * 16

    companion object {
        fun createDefault(): ComposerTrackData {
            val kicks = mutableListOf<Int>()
            val snares = mutableListOf<Int>()
            val hats = mutableListOf<Int>()
            val claps = mutableListOf<Int>()
            val bass = mutableMapOf<Int, Int>()
            val lead = mutableMapOf<Int, Int>()
            val bfVocals = mutableMapOf<Int, Int>()
            val dadVocals = mutableMapOf<Int, Int>()

            val bassPattern = listOf(36, 36, 43, 39, 41, 41, 43, 46)
            val dadPattern = listOf(48, 51, 55, 53, 51, 48, 46, 48)
            val bfPattern = listOf(60, 63, 67, 65, 63, 60, 67, 72)

            for (bar in 0 until 8) {
                val base = bar * 16
                kicks.addAll(listOf(base + 0, base + 6, base + 8, base + 14))
                snares.addAll(listOf(base + 4, base + 12))
                for (s in 0 until 16 step 2) hats.add(base + s)
                if (bar % 2 == 1) claps.add(base + 12)

                bass[base + 0] = bassPattern[bar % bassPattern.size]
                bass[base + 6] = bassPattern[bar % bassPattern.size] + 12
                bass[base + 8] = bassPattern[(bar + 1) % bassPattern.size]
                bass[base + 12] = bassPattern[(bar + 2) % bassPattern.size]

                lead[base + 0] = 60 + (bar % 3) * 2
                lead[base + 4] = 67
                lead[base + 8] = 63
                lead[base + 12] = 65

                if (bar % 2 == 0) {
                    // Opponent sings even bars
                    dadVocals[base + 0] = dadPattern[0]
                    dadVocals[base + 3] = dadPattern[1]
                    dadVocals[base + 6] = dadPattern[2]
                    dadVocals[base + 8] = dadPattern[3]
                    dadVocals[base + 12] = dadPattern[4]
                } else {
                    // BF sings odd bars
                    bfVocals[base + 0] = bfPattern[0]
                    bfVocals[base + 3] = bfPattern[1]
                    bfVocals[base + 6] = bfPattern[2]
                    bfVocals[base + 8] = bfPattern[3]
                    bfVocals[base + 12] = bfPattern[7]
                }
            }

            return ComposerTrackData(
                barsCount = 8,
                drumGrid = mapOf(0 to kicks, 1 to snares, 2 to hats, 3 to claps),
                bassNotes = bass,
                leadNotes = lead,
                bfVocalNotes = bfVocals,
                dadVocalNotes = dadVocals
            )
        }
    }
}

@Serializable
data class ChartSection(
    val mustHitSection: Boolean = true,
    val gfSection: Boolean = false,
    val sectionBeats: Int = 4,
    val changeBpm: Boolean = false,
    val bpm: Float = 128f,
    val notes: List<ChartNote> = emptyList()
)

@Serializable
data class ChartNote(
    val id: String = UUID.randomUUID().toString(),
    // Step index inside the section (0..15 for 1/16 grid, or fractional for 1/8, etc.)
    val stepInSection: Int,
    // 0..3 = Opponent (Left, Down, Up, Right), 4..7 = Boyfriend/Player (Left, Down, Up, Right)
    val lane: Int,
    val sustainSteps: Int = 0,
    val noteType: String = ""
)

@Serializable
data class CharacterAsset(
    val id: String,
    val displayName: String,
    val role: String = "opponent", // "player", "opponent", "gf"
    val healthIconName: String = id,
    val healthBarRgb: List<Int> = listOf(140, 90, 255),
    val flipX: Boolean = false,
    val noAntialiasing: Boolean = false,
    val scale: Float = 1f,
    val singDuration: Float = 4f,
    val positionX: Int = 0,
    val positionY: Int = 0,
    val cameraX: Int = 0,
    val cameraY: Int = 0,
    val spriteMode: SpriteMode = SpriteMode.BUILTIN_VECTOR,
    val primaryColorHex: Long = 0xFF8B5CF6,
    val accentColorHex: Long = 0xFF00E5FF,
    val atlasPngPath: String? = null,
    val atlasXmlPath: String? = null,
    val iconPngPath: String? = null,
    // Map animation key ("idle", "singLEFT", "singDOWN", "singUP", "singRIGHT") -> list of local PNG paths
    val animFrames: Map<String, List<String>> = emptyMap(),
    val animFps: Int = 24
)

@Serializable
data class StageAsset(
    val id: String = "neon-stage",
    val name: String = "Neon Stage",
    val defaultZoom: Float = 0.9f,
    val bgColorHex: Long = 0xFF101626,
    val floorColorHex: Long = 0xFF1B2338,
    val skyImagePath: String? = null,
    val groundImagePath: String? = null,
    val fgImagePath: String? = null,
    val bfPos: List<Int> = listOf(770, 100),
    val dadPos: List<Int> = listOf(100, 100),
    val gfPos: List<Int> = listOf(400, 130)
)

@Serializable
data class NoteSkinAsset(
    val mode: NoteSkinMode = NoteSkinMode.DEFAULT,
    // Lane 0..3 custom PNG paths
    val customArrowPaths: Map<Int, String> = emptyMap(),
    val atlasPngPath: String? = null,
    val atlasXmlPath: String? = null,
    val customColorsHex: List<Long> = listOf(0xFFFF2E93, 0xFF00E5FF, 0xFF2EFF9A, 0xFFFF5E5B)
)

@Serializable
enum class CustomNoteShape(val label: String) {
    ARROW("Flecha"),
    MINE_SPIKE("Mina / Pinchos"),
    HEART_HEAL("Corazón"),
    STAR_PARRY("Estrella"),
    GLITCH_BLOCK("Glitch")
}

@Serializable
enum class CustomNoteEffect(val label: String) {
    NONE("Sin efecto"),
    FIRE_DAMAGE("Fuego / Daño"),
    HEAL_GLOW("Brillo Curativo"),
    CAMERA_SHAKE("Sacudir Cámara"),
    CAMERA_FLASH("Flash Pantalla"),
    SPIN_ARROWS("Girar Flechas"),
    PLAY_HEY("Animación Hey"),
    DRAIN_POISON("Veneno")
}

@Serializable
data class CustomNoteType(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val slug: String,
    val colorHex: Long = 0xFFFF3B5C,
    val shape: CustomNoteShape = CustomNoteShape.ARROW,
    val ignoreNote: Boolean = false,
    val noAnimation: Boolean = false,
    val healthDelta: Float = 0.05f,
    val missPenalty: Float = 0.08f,
    val hitEffect: CustomNoteEffect = CustomNoteEffect.NONE,
    val hitMessage: String = "",
    val customPngPath: String? = null
)

fun slugify(input: String): String {
    val clean = input.lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return clean.ifEmpty { "custom-item" }
}

fun createEmptySections(count: Int, bpm: Float): List<ChartSection> =
    List(count) { idx ->
        ChartSection(
            mustHitSection = idx % 2 == 1,
            bpm = bpm
        )
    }

fun defaultCharacters(): List<CharacterAsset> = listOf(
    CharacterAsset(
        id = "bf",
        displayName = "Boyfriend",
        role = "player",
        healthIconName = "bf",
        healthBarRgb = listOf(49, 176, 209),
        flipX = true,
        positionX = 0,
        positionY = 350,
        primaryColorHex = 0xFF00E5FF,
        accentColorHex = 0xFFFF2E93
    ),
    CharacterAsset(
        id = "rival",
        displayName = "Rival Synth",
        role = "opponent",
        healthIconName = "rival",
        healthBarRgb = listOf(175, 102, 206),
        flipX = false,
        positionX = 0,
        positionY = 100,
        primaryColorHex = 0xFF8B5CF6,
        accentColorHex = 0xFF2EFF9A
    ),
    CharacterAsset(
        id = "gf",
        displayName = "Girlfriend",
        role = "gf",
        healthIconName = "gf",
        healthBarRgb = listOf(165, 0, 77),
        flipX = false,
        positionX = 0,
        positionY = 130,
        primaryColorHex = 0xFFFF2E93,
        accentColorHex = 0xFFFFB020
    )
)

fun defaultStage(): StageAsset = StageAsset()

fun defaultCustomNoteTypes(): List<CustomNoteType> = listOf(
    CustomNoteType(
        id = "builtin_hurt",
        name = "Nota Mina",
        slug = "Hurt Note",
        colorHex = 0xFFFF3B5C,
        shape = CustomNoteShape.MINE_SPIKE,
        ignoreNote = true,
        noAnimation = true,
        healthDelta = -0.35f,
        missPenalty = 0f,
        hitEffect = CustomNoteEffect.FIRE_DAMAGE,
        hitMessage = "¡MINA!"
    ),
    CustomNoteType(
        id = "custom_heal",
        name = "Nota Curativa",
        slug = "Heal Note",
        colorHex = 0xFF2EFF9A,
        shape = CustomNoteShape.HEART_HEAL,
        ignoreNote = false,
        noAnimation = false,
        healthDelta = 0.25f,
        missPenalty = 0.1f,
        hitEffect = CustomNoteEffect.HEAL_GLOW,
        hitMessage = "+VIDA"
    ),
    CustomNoteType(
        id = "custom_parry",
        name = "Nota Parry",
        slug = "Parry Note",
        colorHex = 0xFFFFD60A,
        shape = CustomNoteShape.STAR_PARRY,
        ignoreNote = false,
        noAnimation = false,
        healthDelta = 0.12f,
        missPenalty = 0.35f,
        hitEffect = CustomNoteEffect.CAMERA_FLASH,
        hitMessage = "¡PARRY!"
    )
)
