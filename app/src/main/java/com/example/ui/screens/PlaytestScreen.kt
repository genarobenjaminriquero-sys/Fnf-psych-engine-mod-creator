package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.engine.AudioSynthEngine
import com.example.model.CharacterAsset
import com.example.model.CustomNoteEffect
import com.example.model.CustomNoteShape
import com.example.model.CustomNoteType
import com.example.model.ScratchActionType
import com.example.model.ScratchTriggerType
import com.example.model.SpriteMode
import com.example.model.StageAsset
import com.example.ui.ModStudioViewModel
import com.example.ui.components.FnfArrowIcon
import com.example.ui.components.PillToggle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.laneColor
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class RuntimeNote(
    val id: String,
    val strumTimeMs: Double,
    val lane: Int, // 0..3 Opponent, 4..7 Player
    val sustainMs: Double,
    val noteType: String,
    var hit: Boolean = false,
    var missed: Boolean = false
)

@Composable
fun PlaytestScreen(
    viewModel: ModStudioViewModel,
    onBack: () -> Unit
) {
    val project by viewModel.activeProject.collectAsStateWithLifecycle()
    val songIndex by viewModel.activeSongIndex.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playback.isPlaying.collectAsStateWithLifecycle()
    val positionMs by viewModel.playback.positionMs.collectAsStateWithLifecycle()

    val currentProject = project ?: return
    val song = currentProject.songs.getOrNull(songIndex) ?: currentProject.songs.firstOrNull() ?: return
    val stage = currentProject.stages.firstOrNull() ?: StageAsset()

    BackHandler {
        viewModel.stopPlayback()
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPlayback()
        }
    }

    var autoPlay by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var hits by remember { mutableIntStateOf(0) }
    var health by remember { mutableFloatStateOf(0.5f) } // 0..1 (0.5 = balanced)
    var judgementText by remember { mutableStateOf("LISTO") }
    var judgementColor by remember { mutableStateOf(StudioCyan) }

    // Character live state (can be modified by Scratch blocks!)
    var activePlayerId by remember(song.id) { mutableStateOf(song.player1) }
    var activeOpponentId by remember(song.id) { mutableStateOf(song.player2) }
    var bfAnim by remember { mutableStateOf("idle") }
    var dadAnim by remember { mutableStateOf("idle") }
    var bfAnimUntilMs by remember { mutableLongStateOf(0L) }
    var dadAnimUntilMs by remember { mutableLongStateOf(0L) }
    var bfVisible by remember { mutableStateOf(true) }
    var dadVisible by remember { mutableStateOf(true) }
    var bfTint by remember { mutableStateOf<Color?>(null) }
    var dadTint by remember { mutableStateOf<Color?>(null) }
    var floatSlot by remember { mutableStateOf("") }
    var floatAmp by remember { mutableFloatStateOf(0f) }
    var floatSpd by remember { mutableFloatStateOf(2.5f) }

    // Scratch Live FX State
    var camZoom by remember { mutableFloatStateOf(stage.defaultZoom) }
    var camTiltDeg by remember { mutableFloatStateOf(0f) }
    var camFocusShiftX by remember { mutableFloatStateOf(0f) }
    var shakeIntensity by remember { mutableFloatStateOf(0f) }
    var shakeUntilMs by remember { mutableLongStateOf(0L) }
    var flashAlpha by remember { mutableFloatStateOf(0f) }
    var flashColor by remember { mutableStateOf(Color.White) }
    var fadeAlpha by remember { mutableFloatStateOf(0f) }
    var fadeColor by remember { mutableStateOf(Color.Black) }
    var cinematicBarPx by remember { mutableFloatStateOf(0f) }
    var hudVisible by remember { mutableStateOf(true) }
    var floatingEventText by remember { mutableStateOf("") }
    var floatingTextUntilMs by remember { mutableLongStateOf(0L) }
    var warningBannerText by remember { mutableStateOf("") }
    var warningBannerUntilMs by remember { mutableLongStateOf(0L) }
    var healthDrainEnabled by remember { mutableStateOf(false) }
    var healthDrainAmount by remember { mutableFloatStateOf(0.025f) }
    var arrowSpinDeg by remember { mutableFloatStateOf(0f) }
    var arrowOffsetX by remember { mutableFloatStateOf(0f) }
    var arrowOffsetY by remember { mutableFloatStateOf(0f) }
    var arrowBumpY by remember { mutableFloatStateOf(0f) }
    var arrowShakeAmp by remember { mutableFloatStateOf(0f) }
    var arrowShakeUntilMs by remember { mutableLongStateOf(0L) }
    var strumsSwapped by remember { mutableStateOf(false) }
    var centerStrums by remember { mutableStateOf(false) }
    var oppArrowAlpha by remember { mutableFloatStateOf(1f) }
    var playerArrowAlpha by remember { mutableFloatStateOf(1f) }
    var waveAmp by remember { mutableFloatStateOf(0f) }
    var waveSpeed by remember { mutableFloatStateOf(4f) }
    var activeScrollSpeed by remember(song.id) { mutableFloatStateOf(song.speed) }
    var pressedLanes by remember { mutableStateOf(setOf<Int>()) }

    val customNoteMap = remember(currentProject.customNoteTypes) {
        currentProject.customNoteTypes.associateBy { it.slug }
    }

    val runtimeNotes = remember(song.id, song.sections, song.bpm) {
        val stepMs = AudioSynthEngine.stepDurationMs(song.bpm)
        val list = mutableStateListOf<RuntimeNote>()
        song.sections.forEachIndexed { secIdx, sec ->
            val secStart = secIdx * 16 * stepMs
            sec.notes.forEach { n ->
                list.add(
                    RuntimeNote(
                        id = n.id,
                        strumTimeMs = secStart + n.stepInSection * stepMs,
                        lane = n.lane,
                        sustainMs = n.sustainSteps * stepMs,
                        noteType = n.noteType
                    )
                )
            }
        }
        list.sortBy { it.strumTimeMs }
        list
    }

    val lastBeatTriggered = remember { mutableIntStateOf(-1) }
    val lastStepTriggered = remember { mutableIntStateOf(-1) }
    val songStartTriggered = remember { mutableStateOf(false) }
    val songEndTriggered = remember { mutableStateOf(false) }
    val lowHealthTriggered = remember { mutableStateOf(false) }

    fun resetAndStart() {
        score = 0
        combo = 0
        misses = 0
        hits = 0
        health = 0.5f
        judgementText = "¡A DARLE!"
        judgementColor = StudioCyan
        activePlayerId = song.player1
        activeOpponentId = song.player2
        bfVisible = true
        dadVisible = true
        bfTint = null
        dadTint = null
        floatSlot = ""
        floatAmp = 0f
        camZoom = stage.defaultZoom
        camTiltDeg = 0f
        camFocusShiftX = 0f
        shakeIntensity = 0f
        flashAlpha = 0f
        fadeAlpha = 0f
        cinematicBarPx = 0f
        hudVisible = true
        floatingEventText = ""
        warningBannerText = ""
        healthDrainEnabled = false
        arrowSpinDeg = 0f
        arrowOffsetX = 0f
        arrowOffsetY = 0f
        arrowBumpY = 0f
        arrowShakeAmp = 0f
        strumsSwapped = false
        centerStrums = false
        oppArrowAlpha = 1f
        playerArrowAlpha = 1f
        waveAmp = 0f
        activeScrollSpeed = song.speed
        lastBeatTriggered.intValue = -1
        lastStepTriggered.intValue = -1
        songStartTriggered.value = false
        songEndTriggered.value = false
        lowHealthTriggered.value = false
        runtimeNotes.forEach {
            it.hit = false
            it.missed = false
        }
        viewModel.playback.playSong(song, 0L)
    }

    LaunchedEffect(song.id) {
        resetAndStart()
    }

    fun applyScratchAction(actionType: ScratchActionType, p1: String, p2: String, p3: String, nowMs: Long) {
        when (actionType) {
            ScratchActionType.CAMERA_ZOOM -> {
                camZoom = (p1.toFloatOrNull() ?: 1.15f).coerceIn(0.6f, 1.8f)
            }
            ScratchActionType.CAMERA_SHAKE -> {
                shakeIntensity = (p1.toFloatOrNull() ?: 0.02f).coerceIn(0.002f, 0.08f) * 380f
                val dur = ((p2.toFloatOrNull() ?: 0.25f) * 1000).toLong()
                shakeUntilMs = nowMs + dur
            }
            ScratchActionType.CAMERA_FLASH -> {
                val hex = p1.trim().removePrefix("#").take(6)
                flashColor = runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrDefault(Color.White)
                flashAlpha = 0.8f
            }
            ScratchActionType.CAMERA_FADE -> {
                val hex = p1.trim().removePrefix("#").take(6)
                fadeColor = runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrDefault(Color.Black)
                fadeAlpha = 0.85f
            }
            ScratchActionType.CAMERA_TILT -> {
                camTiltDeg = (p1.toFloatOrNull() ?: 6f).coerceIn(-35f, 35f)
            }
            ScratchActionType.CAMERA_FOCUS -> {
                camFocusShiftX = if (p1.lowercase().contains("bf") || p1.lowercase().contains("boy")) -28f else 28f
            }
            ScratchActionType.CINEMATIC_BARS -> {
                val enable = p1.equals("true", ignoreCase = true)
                val thick = (p2.toFloatOrNull() ?: 80f).coerceIn(16f, 140f)
                cinematicBarPx = if (enable) thick * 0.45f else 0f
            }
            ScratchActionType.CHANGE_CHARACTER -> {
                val slot = p1.lowercase()
                if (slot == "dad" || slot == "opponent") activeOpponentId = p2
                else if (slot == "bf" || slot == "boyfriend") activePlayerId = p2
            }
            ScratchActionType.PLAY_ANIM -> {
                if (p1.lowercase().contains("dad")) {
                    dadAnim = p2
                    dadAnimUntilMs = nowMs + 450L
                } else {
                    bfAnim = p2
                    bfAnimUntilMs = nowMs + 450L
                }
            }
            ScratchActionType.TOGGLE_CHAR_VISIBLE -> {
                val vis = p2.equals("true", ignoreCase = true)
                if (p1.lowercase().contains("dad")) dadVisible = vis
                else if (p1.lowercase().contains("bf") || p1.lowercase().contains("boy")) bfVisible = vis
            }
            ScratchActionType.FLOAT_CHARACTER -> {
                floatSlot = p1.lowercase()
                floatAmp = (p2.toFloatOrNull() ?: 45f).coerceIn(0f, 80f) * 0.4f
                floatSpd = (p3.toFloatOrNull() ?: 2.5f).coerceIn(0.5f, 10f)
            }
            ScratchActionType.TINT_CHARACTER -> {
                val hex = p2.trim().removePrefix("#").take(6)
                val tintCol = if (hex.equals("FFFFFF", ignoreCase = true)) null else {
                    runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrNull()
                }
                if (p1.lowercase().contains("dad")) dadTint = tintCol
                else if (p1.lowercase().contains("bf") || p1.lowercase().contains("boy")) bfTint = tintCol
            }
            ScratchActionType.SPIN_ARROWS -> {
                arrowSpinDeg = (arrowSpinDeg + (p2.toFloatOrNull() ?: 360f)) % 720f
            }
            ScratchActionType.MOVE_ARROWS -> {
                arrowOffsetX = (p2.toFloatOrNull() ?: 0f).coerceIn(-60f, 60f)
                arrowOffsetY = (p3.toFloatOrNull() ?: -20f).coerceIn(-60f, 60f)
            }
            ScratchActionType.BUMP_ARROWS -> {
                arrowBumpY = -(p2.toFloatOrNull() ?: 22f).coerceIn(-50f, 50f)
            }
            ScratchActionType.SHAKE_ARROWS -> {
                arrowShakeAmp = (p1.toFloatOrNull() ?: 12f).coerceIn(1f, 36f)
                val dur = ((p2.toFloatOrNull() ?: 0.35f) * 1000).toLong()
                arrowShakeUntilMs = nowMs + dur
            }
            ScratchActionType.SWAP_STRUMS -> {
                strumsSwapped = !strumsSwapped
            }
            ScratchActionType.CENTER_STRUMS -> {
                val enable = p1.equals("true", ignoreCase = true)
                centerStrums = enable
                oppArrowAlpha = if (enable) 0.18f else 1f
            }
            ScratchActionType.RESET_ARROWS -> {
                arrowSpinDeg = 0f
                arrowOffsetX = 0f
                arrowOffsetY = 0f
                arrowBumpY = 0f
                arrowShakeAmp = 0f
                strumsSwapped = false
                centerStrums = false
                oppArrowAlpha = 1f
                playerArrowAlpha = 1f
                waveAmp = 0f
            }
            ScratchActionType.FADE_ARROWS -> {
                val a = (p2.toFloatOrNull() ?: 0.35f).coerceIn(0.1f, 1f)
                if (p1 == "opponent") oppArrowAlpha = a else if (p1 == "player") playerArrowAlpha = a else {
                    oppArrowAlpha = a; playerArrowAlpha = a
                }
            }
            ScratchActionType.WAVE_MODCHART -> {
                waveAmp = (p1.toFloatOrNull() ?: 16f).coerceIn(0f, 40f)
                waveSpeed = (p2.toFloatOrNull() ?: 4f).coerceIn(1f, 12f)
            }
            ScratchActionType.SCROLL_SPEED -> {
                activeScrollSpeed = (p1.toFloatOrNull() ?: song.speed).coerceIn(1f, 4.5f)
            }
            ScratchActionType.CHANGE_HEALTH -> {
                health = (health + (p1.toFloatOrNull() ?: 0f) * 0.5f).coerceIn(0.05f, 1f)
            }
            ScratchActionType.HEALTH_DRAIN -> {
                healthDrainEnabled = p1.equals("true", ignoreCase = true)
                healthDrainAmount = (p2.toFloatOrNull() ?: 0.025f).coerceIn(0.005f, 0.2f)
            }
            ScratchActionType.SHOW_FLOATING_TEXT -> {
                floatingEventText = p1
                floatingTextUntilMs = nowMs + ((p2.toFloatOrNull() ?: 1.5f) * 1000).toLong()
            }
            ScratchActionType.WARNING_BANNER -> {
                warningBannerText = p1.ifBlank { "¡CUIDADO!" }
                warningBannerUntilMs = nowMs + ((p2.toFloatOrNull() ?: 1.2f) * 1000).toLong()
            }
            ScratchActionType.PLAY_SOUND_FX -> {
                viewModel.playback.playOneShot(99, 0)
            }
            ScratchActionType.TOGGLE_HUD -> {
                hudVisible = p1.equals("true", ignoreCase = true)
            }
        }
    }

    // Main Playtest Frame Tick (drives opponent notes, auto-play, miss checks, & Scratch triggers)
    LaunchedEffect(positionMs, isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        val now = positionMs
        val stepMs = AudioSynthEngine.stepDurationMs(song.bpm)
        val beatMs = stepMs * 4.0
        val curBeat = (now / beatMs).toInt()
        val curStep = (now / stepMs).toInt()

        if (!songStartTriggered.value && now >= 20L) {
            songStartTriggered.value = true
            song.scratchBlocks.filter { it.enabled && it.triggerType == ScratchTriggerType.ON_SONG_START }
                .forEach { applyScratchAction(it.actionType, it.param1, it.param2, it.param3, now) }
        }

        if (curBeat > lastBeatTriggered.intValue) {
            lastBeatTriggered.intValue = curBeat
            // Gentle camera decay toward default zoom
            camZoom += (stage.defaultZoom - camZoom) * 0.3f
            song.scratchBlocks.filter {
                it.enabled && (
                    (it.triggerType == ScratchTriggerType.ON_BEAT && it.triggerValue == curBeat) ||
                        (it.triggerType == ScratchTriggerType.EVERY_N_BEATS && curBeat % it.triggerValue.coerceAtLeast(1) == 0)
                    )
            }.forEach { applyScratchAction(it.actionType, it.param1, it.param2, it.param3, now) }
        }

        if (curStep > lastStepTriggered.intValue) {
            lastStepTriggered.intValue = curStep
            song.scratchBlocks.filter {
                it.enabled && (
                    (it.triggerType == ScratchTriggerType.ON_STEP && it.triggerValue == curStep) ||
                        (it.triggerType == ScratchTriggerType.EVERY_N_STEPS && curStep % it.triggerValue.coerceAtLeast(1) == 0)
                    )
            }.forEach { applyScratchAction(it.actionType, it.param1, it.param2, it.param3, now) }
        }

        if (health < 0.25f && !lowHealthTriggered.value) {
            lowHealthTriggered.value = true
            song.scratchBlocks.filter { it.enabled && it.triggerType == ScratchTriggerType.ON_HEALTH_LOW }
                .forEach { applyScratchAction(it.actionType, it.param1, it.param2, it.param3, now) }
        } else if (health >= 0.35f) {
            lowHealthTriggered.value = false
        }

        val totalSongMs = (song.sections.size * 16 * stepMs).toLong()
        if (!songEndTriggered.value && totalSongMs > 1000L && now >= totalSongMs - 300L) {
            songEndTriggered.value = true
            song.scratchBlocks.filter { it.enabled && it.triggerType == ScratchTriggerType.ON_SONG_END }
                .forEach { applyScratchAction(it.actionType, it.param1, it.param2, it.param3, now) }
        }

        // Decay visual FX
        if (flashAlpha > 0f) flashAlpha = (flashAlpha - 0.05f).coerceAtLeast(0f)
        if (fadeAlpha > 0f) fadeAlpha = (fadeAlpha - 0.025f).coerceAtLeast(0f)
        if (abs(arrowBumpY) > 0.5f) arrowBumpY *= 0.72f else arrowBumpY = 0f
        if (now > bfAnimUntilMs) bfAnim = "idle"
        if (now > dadAnimUntilMs) dadAnim = "idle"
        if (now > floatingTextUntilMs) floatingEventText = ""
        if (now > warningBannerUntilMs) warningBannerText = ""

        val animMap = listOf("singLEFT", "singDOWN", "singUP", "singRIGHT")

        // Opponent hits + Auto-Play player hits + Misses
        for (note in runtimeNotes) {
            if (!note.hit && !note.missed) {
                val customType = customNoteMap[note.noteType]
                val isSkippable = customType?.ignoreNote ?: (note.noteType == "Hurt Note")
                if (note.lane in 0..3 && now >= note.strumTimeMs) {
                    note.hit = true
                    if (customType?.noAnimation != true) {
                        dadAnim = animMap[note.lane % 4]
                        dadAnimUntilMs = now + 320L
                    }
                    if (healthDrainEnabled && health > 0.12f) {
                        health = (health - healthDrainAmount * 0.5f).coerceAtLeast(0.1f)
                    }
                    song.scratchBlocks.filter { it.enabled && it.triggerType == ScratchTriggerType.ON_OPPONENT_HIT }
                        .forEach { applyScratchAction(it.actionType, it.param1, it.param2, it.param3, now) }
                } else if (note.lane in 4..7) {
                    if (autoPlay && !isSkippable && now >= note.strumTimeMs) {
                        note.hit = true
                        score += 350
                        combo += 1
                        hits += 1
                        val delta = customType?.healthDelta ?: 0.03f
                        health = (health + delta * 0.5f).coerceIn(0.05f, 1f)
                        judgementText = customType?.hitMessage?.takeIf { it.isNotBlank() } ?: "AUTO SICK!"
                        judgementColor = customType?.let { Color(it.colorHex) } ?: StudioCyan
                        if (customType?.noAnimation != true) {
                            bfAnim = animMap[note.lane % 4]
                            bfAnimUntilMs = now + 320L
                        }
                    } else if (!autoPlay && now - note.strumTimeMs > 180.0) {
                        note.missed = true
                        if (!isSkippable) {
                            misses += 1
                            combo = 0
                            score = (score - 50).coerceAtLeast(0)
                            val penalty = customType?.missPenalty ?: 0.06f
                            health = (health - penalty * 0.5f).coerceAtLeast(0.05f)
                            judgementText = "MISS"
                            judgementColor = ErrorRed
                            song.scratchBlocks.filter { it.enabled && it.triggerType == ScratchTriggerType.ON_MISS }
                                .forEach { applyScratchAction(it.actionType, it.param1, it.param2, it.param3, now) }
                        }
                    }
                }
            }
        }
    }

    fun onPlayerLaneTap(laneOffset: Int) {
        val targetLane = 4 + laneOffset
        val now = positionMs.toDouble()
        val candidate = runtimeNotes.firstOrNull {
            !it.hit && !it.missed && it.lane == targetLane && abs(it.strumTimeMs - now) <= 185.0
        }
        val animMap = listOf("singLEFT", "singDOWN", "singUP", "singRIGHT")
        val candidateCustom = candidate?.noteType?.let { customNoteMap[it] }
        if (candidateCustom?.noAnimation != true) {
            bfAnim = animMap[laneOffset]
            bfAnimUntilMs = positionMs + 300L
        }

        if (candidate != null) {
            candidate.hit = true
            if (candidateCustom != null) {
                val isHurtCustom = candidateCustom.healthDelta < 0f
                if (isHurtCustom) {
                    misses += 1
                    combo = 0
                } else {
                    score += 300
                    combo += 1
                    hits += 1
                }
                health = (health + candidateCustom.healthDelta * 0.5f).coerceIn(0.05f, 1f)
                judgementText = candidateCustom.hitMessage.ifBlank { if (isHurtCustom) "¡DAÑO!" else "ESPECIAL!" }
                judgementColor = Color(candidateCustom.colorHex)
                viewModel.playback.playOneShot(99, 0)

                when (candidateCustom.hitEffect) {
                    CustomNoteEffect.NONE -> Unit
                    CustomNoteEffect.FIRE_DAMAGE -> {
                        shakeIntensity = 9f
                        shakeUntilMs = positionMs + 260L
                        flashColor = Color(candidateCustom.colorHex)
                        flashAlpha = 0.55f
                    }
                    CustomNoteEffect.HEAL_GLOW -> {
                        flashColor = StudioMint
                        flashAlpha = 0.35f
                    }
                    CustomNoteEffect.CAMERA_SHAKE -> {
                        shakeIntensity = 11f
                        shakeUntilMs = positionMs + 300L
                    }
                    CustomNoteEffect.CAMERA_FLASH -> {
                        flashColor = Color(candidateCustom.colorHex)
                        flashAlpha = 0.65f
                    }
                    CustomNoteEffect.SPIN_ARROWS -> {
                        arrowSpinDeg = (arrowSpinDeg + 360f) % 720f
                    }
                    CustomNoteEffect.PLAY_HEY -> {
                        bfAnim = "hey"
                        bfAnimUntilMs = positionMs + 450L
                    }
                    CustomNoteEffect.DRAIN_POISON -> {
                        health = (health - 0.08f).coerceAtLeast(0.05f)
                        flashColor = Color(0xFF9B5DE5)
                        flashAlpha = 0.4f
                    }
                }
                if (!isHurtCustom) {
                    song.scratchBlocks.filter { it.enabled && it.triggerType == ScratchTriggerType.ON_PLAYER_HIT }
                        .forEach { applyScratchAction(it.actionType, it.param1, it.param2, it.param3, positionMs) }
                }
            } else if (candidate.noteType == "Hurt Note") {
                misses += 1
                combo = 0
                health = (health - 0.18f).coerceAtLeast(0.05f)
                judgementText = "¡DAÑO!"
                judgementColor = ErrorRed
            } else {
                val diff = abs(candidate.strumTimeMs - now)
                val (pts, label, col) = when {
                    diff <= 55.0 -> Triple(350, "SICK!!", StudioCyan)
                    diff <= 105.0 -> Triple(200, "GOOD!", StudioMint)
                    else -> Triple(100, "BAD", WarningAmber)
                }
                score += pts
                combo += 1
                hits += 1
                health = (health + 0.04f).coerceAtMost(1f)
                judgementText = label
                judgementColor = col
                viewModel.playback.playOneShot(99, 0)
                song.scratchBlocks.filter { it.enabled && it.triggerType == ScratchTriggerType.ON_PLAYER_HIT }
                    .forEach { applyScratchAction(it.actionType, it.param1, it.param2, it.param3, positionMs) }
            }
        } else {
            viewModel.playback.playOneShot(99, 0)
        }
    }

    val bfChar = currentProject.characters.find { it.id == activePlayerId } ?: currentProject.characters.first()
    val dadChar = currentProject.characters.find { it.id == activeOpponentId } ?: currentProject.characters.getOrElse(1) { bfChar }

    val shakeX = if (positionMs < shakeUntilMs) (Random.nextFloat() - 0.5f) * shakeIntensity else 0f
    val shakeY = if (positionMs < shakeUntilMs) (Random.nextFloat() - 0.5f) * shakeIntensity else 0f
    val arrowJitterX = if (positionMs < arrowShakeUntilMs) (Random.nextFloat() - 0.5f) * arrowShakeAmp else 0f
    val arrowJitterY = if (positionMs < arrowShakeUntilMs) (Random.nextFloat() - 0.5f) * arrowShakeAmp else 0f
    val dadFloatY = if (floatSlot.contains("dad") && floatAmp != 0f) {
        (sin((positionMs / 1000.0) * floatSpd) * floatAmp).toFloat()
    } else 0f
    val bfFloatY = if ((floatSlot.contains("bf") || floatSlot.contains("boy")) && floatAmp != 0f) {
        (sin((positionMs / 1000.0) * floatSpd) * floatAmp).toFloat()
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(stage.bgColorHex))
            .offset { IntOffset(shakeX.roundToInt(), shakeY.roundToInt()) }
    ) {
        // Stage Background Layer
        if (!stage.skyImagePath.isNullOrBlank() && File(stage.skyImagePath).exists()) {
            AsyncImage(
                model = File(stage.skyImagePath),
                contentDescription = "Escenario",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = camZoom.coerceIn(0.8f, 1.4f),
                        scaleY = camZoom.coerceIn(0.8f, 1.4f),
                        rotationZ = camTiltDeg,
                        translationX = camFocusShiftX
                    ),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .graphicsLayer(rotationZ = camTiltDeg * 0.4f)
                .background(Color(stage.floorColorHex).copy(alpha = 0.85f))
        )

        // Characters Stage Layer (Opponent Left, Player Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 24.dp, vertical = 40.dp)
                .graphicsLayer(
                    scaleX = camZoom.coerceIn(0.8f, 1.35f),
                    scaleY = camZoom.coerceIn(0.8f, 1.35f),
                    rotationZ = camTiltDeg,
                    translationX = camFocusShiftX
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .alpha(if (dadVisible) 1f else 0f)
                    .offset(y = dadFloatY.dp)
            ) {
                PlaytestCharacterView(char = dadChar, anim = dadAnim, tint = dadTint)
            }
            Box(
                modifier = Modifier
                    .alpha(if (bfVisible) 1f else 0f)
                    .offset(y = bfFloatY.dp)
            ) {
                PlaytestCharacterView(char = bfChar, anim = bfAnim, tint = bfTint)
            }
        }

        // Strumline + Scrolling Notes Canvas
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 62.dp, bottom = 96.dp)
        ) {
            val receptorY = 74.dp
            val scrollPxPerMs = 0.24f * activeScrollSpeed
            val totalOffsetY = arrowOffsetY + arrowBumpY + arrowJitterY
            val totalOffsetX = arrowOffsetX + arrowJitterX

            // Receptor Icons (Lanes 0..3 Opponent on left, 4..7 Player on right, supports Swap & Center)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .offset(x = totalOffsetX.dp, y = receptorY + totalOffsetY.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val oppReceptors: @Composable (Modifier) -> Unit = { mod ->
                    Row(
                        modifier = mod.alpha(oppArrowAlpha),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (l in 0..3) {
                            val waveY = if (waveAmp != 0f) sin((positionMs / 1000.0) * waveSpeed + l * 0.7).toFloat() * waveAmp else 0f
                            FnfArrowIcon(
                                lane = l,
                                color = Color.White.copy(alpha = 0.5f),
                                filled = false,
                                size = 34.dp,
                                modifier = Modifier
                                    .offset(y = waveY.dp)
                                    .rotate(arrowSpinDeg)
                            )
                        }
                    }
                }
                val playerReceptors: @Composable (Modifier) -> Unit = { mod ->
                    Row(
                        modifier = mod.alpha(playerArrowAlpha),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (l in 0..3) {
                            val isPressed = l in pressedLanes
                            val waveY = if (waveAmp != 0f) sin((positionMs / 1000.0) * waveSpeed + (l + 4) * 0.7).toFloat() * waveAmp else 0f
                            FnfArrowIcon(
                                lane = l,
                                color = if (isPressed) laneColor(l) else Color.White.copy(alpha = 0.75f),
                                filled = isPressed,
                                size = 36.dp,
                                modifier = Modifier
                                    .offset(y = waveY.dp)
                                    .rotate(arrowSpinDeg)
                            )
                        }
                    }
                }

                if (centerStrums) {
                    Spacer(modifier = Modifier.weight(0.5f))
                    playerReceptors(Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(0.5f))
                } else if (strumsSwapped) {
                    playerReceptors(Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(14.dp))
                    oppReceptors(Modifier.weight(1f))
                } else {
                    oppReceptors(Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(14.dp))
                    playerReceptors(Modifier.weight(1f))
                }
            }

            // Scrolling Notes
            val visibleNotes = runtimeNotes.filter {
                !it.hit && !it.missed &&
                    it.strumTimeMs - positionMs in -250.0..2400.0
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val recYPx = receptorY.toPx() + totalOffsetY.dp.toPx()
                val sidePad = 12.dp.toPx() + totalOffsetX.dp.toPx()
                val gapPx = 14.dp.toPx()
                val halfW = (size.width - 24.dp.toPx() - gapPx) / 2f
                val colW = halfW / 4f

                visibleNotes.forEach { note ->
                    val dt = (note.strumTimeMs - positionMs).toFloat()
                    val yPos = recYPx + dt * scrollPxPerMs * density
                    val isPlayer = note.lane >= 4
                    val laneLocal = note.lane % 4
                    val xCenter = when {
                        centerStrums && isPlayer -> (size.width / 2f - halfW / 2f) + colW * laneLocal + colW / 2f
                        strumsSwapped -> if (isPlayer) {
                            sidePad + colW * laneLocal + colW / 2f
                        } else {
                            sidePad + halfW + gapPx + colW * laneLocal + colW / 2f
                        }
                        else -> if (!isPlayer) {
                            sidePad + colW * laneLocal + colW / 2f
                        } else {
                            sidePad + halfW + gapPx + colW * laneLocal + colW / 2f
                        }
                    }
                    val customType = customNoteMap[note.noteType]
                    val col = when {
                        customType != null -> Color(customType.colorHex)
                        note.noteType == "Hurt Note" -> ErrorRed
                        else -> laneColor(laneLocal)
                    }
                    val shape = customType?.shape ?: if (note.noteType == "Hurt Note") CustomNoteShape.MINE_SPIKE else CustomNoteShape.ARROW
                    val alpha = if (isPlayer) playerArrowAlpha else oppArrowAlpha

                    // Sustain Tail
                    if (note.sustainMs > 0) {
                        val tailH = (note.sustainMs.toFloat() * scrollPxPerMs * density).coerceAtLeast(8f)
                        drawRoundRect(
                            color = col.copy(alpha = 0.55f * alpha),
                            topLeft = Offset(xCenter - 7.dp.toPx(), yPos),
                            size = Size(14.dp.toPx(), tailH),
                            cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                        )
                    }

                    // Note Head Shape
                    val r = 15.dp.toPx()
                    when (shape) {
                        CustomNoteShape.ARROW -> {
                            drawCircle(color = col.copy(alpha = alpha), radius = r, center = Offset(xCenter, yPos))
                            drawCircle(color = Color.White.copy(alpha = 0.9f * alpha), radius = 7.dp.toPx(), center = Offset(xCenter, yPos))
                        }
                        CustomNoteShape.MINE_SPIKE -> {
                            val p = Path()
                            for (i in 0 until 16) {
                                val rad = if (i % 2 == 0) r * 1.15f else r * 0.6f
                                val a = Math.toRadians(i * 22.5)
                                val px = xCenter + (rad * cos(a)).toFloat()
                                val py = yPos + (rad * sin(a)).toFloat()
                                if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                            }
                            p.close()
                            drawPath(p, color = col.copy(alpha = alpha))
                            drawCircle(color = ObsidianBg.copy(alpha = alpha), radius = 5.dp.toPx(), center = Offset(xCenter, yPos))
                        }
                        CustomNoteShape.HEART_HEAL -> {
                            val p = Path().apply {
                                moveTo(xCenter, yPos + r * 0.9f)
                                cubicTo(xCenter - r * 1.2f, yPos + r * 0.2f, xCenter - r * 0.9f, yPos - r * 0.9f, xCenter, yPos - r * 0.3f)
                                cubicTo(xCenter + r * 0.9f, yPos - r * 0.9f, xCenter + r * 1.2f, yPos + r * 0.2f, xCenter, yPos + r * 0.9f)
                                close()
                            }
                            drawPath(p, color = col.copy(alpha = alpha))
                            drawPath(p, color = Color.White.copy(alpha = alpha), style = Stroke(width = 2.dp.toPx()))
                        }
                        CustomNoteShape.STAR_PARRY -> {
                            val p = Path()
                            for (i in 0 until 10) {
                                val rad = if (i % 2 == 0) r * 1.1f else r * 0.48f
                                val a = Math.toRadians(i * 36.0 - 90.0)
                                val px = xCenter + (rad * cos(a)).toFloat()
                                val py = yPos + (rad * sin(a)).toFloat()
                                if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                            }
                            p.close()
                            drawPath(p, color = col.copy(alpha = alpha))
                            drawPath(p, color = Color.White.copy(alpha = alpha), style = Stroke(width = 2.dp.toPx()))
                        }
                        CustomNoteShape.GLITCH_BLOCK -> {
                            drawRoundRect(
                                color = col.copy(alpha = alpha),
                                topLeft = Offset(xCenter - r, yPos - r),
                                size = Size(r * 2f, r * 2f),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            drawRect(
                                color = ObsidianBg.copy(alpha = alpha),
                                topLeft = Offset(xCenter - r * 0.6f, yPos - 3.dp.toPx()),
                                size = Size(r * 1.2f, 6.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        // HUD Layer (Score, Health Bar, Judgement, Floating Text)
        if (hudVisible) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.stopPlayback()
                            onBack()
                        },
                        modifier = Modifier.testTag("playtest_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Salir", tint = TextPrimary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(song.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        val acc = if (hits + misses == 0) 100 else ((hits * 100) / (hits + misses))
                        Text(
                            text = "Puntos: $score • Combo: ${combo}x • Precisión: $acc%",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioCyan
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        PillToggle(
                            label = if (autoPlay) "AUTO" else "MANUAL",
                            selected = autoPlay,
                            accent = StudioMint,
                            onClick = { autoPlay = !autoPlay },
                            modifier = Modifier.testTag("playtest_autoplay_toggle")
                        )
                        IconButton(onClick = { resetAndStart() }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Replay else Icons.Filled.PlayArrow,
                                contentDescription = "Reiniciar canción",
                                tint = TextPrimary
                            )
                        }
                    }
                }

                // FNF Health Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(7.dp))
                ) {
                    val oppFrac = (1f - health).coerceIn(0.05f, 0.95f)
                    val bfFrac = health.coerceIn(0.05f, 0.95f)
                    Box(modifier = Modifier.weight(oppFrac).fillMaxHeight().background(Color(dadChar.primaryColorHex)))
                    Box(modifier = Modifier.weight(bfFrac).fillMaxHeight().background(Color(bfChar.primaryColorHex)))
                }
            }

            // Judgement & Floating Scratch Event Text
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (floatingEventText.isNotBlank()) {
                    Text(
                        text = floatingEventText,
                        style = MaterialTheme.typography.headlineMedium,
                        color = WarningAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = judgementText,
                    style = MaterialTheme.typography.titleLarge,
                    color = judgementColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4 Interactive Touch Pads (Left, Down, Up, Right)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(ObsidianBg.copy(alpha = 0.85f))
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val labels = listOf("←", "↓", "↑", "→")
            for (lane in 0..3) {
                val active = lane in pressedLanes
                val col = laneColor(lane)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) col.copy(alpha = 0.35f) else StudioSurfaceElevated)
                        .border(2.dp, if (active) col else col.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .pointerInput(lane) {
                            detectTapGestures(
                                onPress = {
                                    pressedLanes = pressedLanes + lane
                                    onPlayerLaneTap(lane)
                                    tryAwaitRelease()
                                    pressedLanes = pressedLanes - lane
                                }
                            )
                        }
                        .testTag("playtest_lane_$lane"),
                    contentAlignment = Alignment.Center
                ) {
                    FnfArrowIcon(lane = lane, color = col, filled = active, size = 32.dp)
                }
            }
        }

        // Cinematic Letterbox Bars
        if (cinematicBarPx > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(cinematicBarPx.dp)
                    .background(Color.Black)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(cinematicBarPx.dp)
                    .background(Color.Black)
            )
        }

        // Warning Banner from Scratch Events
        if (warningBannerText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 86.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ErrorRed.copy(alpha = 0.92f))
                    .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = warningBannerText,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Screen Flash & Fade Overlays from Scratch Events
        if (fadeAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fadeColor.copy(alpha = fadeAlpha))
            )
        }
        if (flashAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(flashColor.copy(alpha = flashAlpha))
            )
        }
    }
}

@Composable
private fun PlaytestCharacterView(char: CharacterAsset, anim: String, tint: Color? = null) {
    val frames = char.animFrames[anim].orEmpty().ifEmpty { char.animFrames["idle"].orEmpty() }
    val firstFrame = frames.firstOrNull()?.let(::File)?.takeIf { it.exists() }
    Box(
        modifier = if (tint != null) {
            Modifier.border(2.dp, tint.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .background(tint.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                .padding(4.dp)
        } else Modifier
    ) {
        if (char.spriteMode == SpriteMode.IMPORTED_FRAMES && firstFrame != null) {
            AsyncImage(
                model = firstFrame,
                contentDescription = char.displayName,
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            ProceduralCharacterPreview(char = char, anim = anim, tick = if (anim == "idle") 0 else 1)
        }
    }
}
