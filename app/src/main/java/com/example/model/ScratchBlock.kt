package com.example.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ScratchTriggerType(val label: String, val description: String) {
    ON_SONG_START("Al iniciar la canción", "Se ejecuta una vez al empezar"),
    ON_BEAT("En el Beat exacto", "Se ejecuta cuando curBeat == valor"),
    EVERY_N_BEATS("Cada N Beats", "Se repite cada cierto número de beats"),
    ON_STEP("En el Step exacto", "Precisión de 1/16 de compás"),
    EVERY_N_STEPS("Cada N Steps", "Pulsos rápidos cada N steps"),
    ON_PLAYER_HIT("Al golpear nota (Jugador)", "Cada vez que BF acierta una flecha"),
    ON_OPPONENT_HIT("Al golpear nota (Rival)", "Cada vez que el rival canta"),
    ON_MISS("Al fallar nota", "Cuando el jugador falla una flecha"),
    ON_HEALTH_LOW("Cuando vida < 25%", "Se dispara al estar en peligro"),
    ON_SONG_END("Al terminar la canción", "Justo antes de finalizar el tema")
}

@Serializable
enum class ScratchCategory(val label: String) {
    ARROWS("Flechas"),
    CHARACTERS("Personajes"),
    CAMERA("Cámara"),
    HUD("Juego / HUD")
}

@Serializable
enum class ScratchActionType(
    val label: String,
    val category: ScratchCategory,
    val defaultParam1: String,
    val defaultParam2: String,
    val defaultParam3: String,
    val param1Label: String,
    val param2Label: String,
    val param3Label: String
) {
    // --- FLECHAS / MODCHART ---
    MOVE_ARROWS(
        label = "Mover flechas",
        category = ScratchCategory.ARROWS,
        defaultParam1 = "all",
        defaultParam2 = "0",
        defaultParam3 = "-25",
        param1Label = "Grupo",
        param2Label = "Offset X",
        param3Label = "Offset Y"
    ),
    SPIN_ARROWS(
        label = "Rotar flechas",
        category = ScratchCategory.ARROWS,
        defaultParam1 = "all",
        defaultParam2 = "360",
        defaultParam3 = "0.4",
        param1Label = "Grupo",
        param2Label = "Ángulo°",
        param3Label = "Duración s"
    ),
    FADE_ARROWS(
        label = "Opacidad de flechas",
        category = ScratchCategory.ARROWS,
        defaultParam1 = "opponent",
        defaultParam2 = "0.35",
        defaultParam3 = "0.4",
        param1Label = "Grupo",
        param2Label = "Opacidad",
        param3Label = "Duración s"
    ),
    WAVE_MODCHART(
        label = "Ondular flechas (Modchart)",
        category = ScratchCategory.ARROWS,
        defaultParam1 = "18",
        defaultParam2 = "4",
        defaultParam3 = "",
        param1Label = "Amplitud px",
        param2Label = "Velocidad",
        param3Label = ""
    ),
    BUMP_ARROWS(
        label = "Pulso / Rebote de flechas",
        category = ScratchCategory.ARROWS,
        defaultParam1 = "all",
        defaultParam2 = "22",
        defaultParam3 = "0.2",
        param1Label = "Grupo",
        param2Label = "Salto px",
        param3Label = "Duración s"
    ),
    SHAKE_ARROWS(
        label = "Vibrar flechas",
        category = ScratchCategory.ARROWS,
        defaultParam1 = "12",
        defaultParam2 = "0.35",
        defaultParam3 = "",
        param1Label = "Fuerza px",
        param2Label = "Duración s",
        param3Label = ""
    ),
    SWAP_STRUMS(
        label = "Intercambiar lados (BF ↔ Rival)",
        category = ScratchCategory.ARROWS,
        defaultParam1 = "0.5",
        defaultParam2 = "",
        defaultParam3 = "",
        param1Label = "Duración s",
        param2Label = "",
        param3Label = ""
    ),
    CENTER_STRUMS(
        label = "Centrar flechas (Middlescroll)",
        category = ScratchCategory.ARROWS,
        defaultParam1 = "true",
        defaultParam2 = "0.45",
        defaultParam3 = "",
        param1Label = "Activar",
        param2Label = "Duración s",
        param3Label = ""
    ),
    RESET_ARROWS(
        label = "Reiniciar flechas a normal",
        category = ScratchCategory.ARROWS,
        defaultParam1 = "0.4",
        defaultParam2 = "",
        defaultParam3 = "",
        param1Label = "Duración s",
        param2Label = "",
        param3Label = ""
    ),
    SCROLL_SPEED(
        label = "Cambiar velocidad scroll",
        category = ScratchCategory.ARROWS,
        defaultParam1 = "3.0",
        defaultParam2 = "0.5",
        defaultParam3 = "",
        param1Label = "Velocidad",
        param2Label = "Tiempo s",
        param3Label = ""
    ),

    // --- PERSONAJES ---
    CHANGE_CHARACTER(
        label = "Cambiar personaje",
        category = ScratchCategory.CHARACTERS,
        defaultParam1 = "dad",
        defaultParam2 = "bf",
        defaultParam3 = "",
        param1Label = "Slot",
        param2Label = "Personaje",
        param3Label = ""
    ),
    PLAY_ANIM(
        label = "Reproducir animación",
        category = ScratchCategory.CHARACTERS,
        defaultParam1 = "boyfriend",
        defaultParam2 = "hey",
        defaultParam3 = "",
        param1Label = "Personaje",
        param2Label = "Animación",
        param3Label = ""
    ),
    TOGGLE_CHAR_VISIBLE(
        label = "Mostrar / Ocultar personaje",
        category = ScratchCategory.CHARACTERS,
        defaultParam1 = "dad",
        defaultParam2 = "false",
        defaultParam3 = "",
        param1Label = "Slot",
        param2Label = "Visible",
        param3Label = ""
    ),
    FLOAT_CHARACTER(
        label = "Hacer levitar personaje",
        category = ScratchCategory.CHARACTERS,
        defaultParam1 = "dad",
        defaultParam2 = "45",
        defaultParam3 = "2.5",
        param1Label = "Slot",
        param2Label = "Altura px",
        param3Label = "Velocidad"
    ),
    TINT_CHARACTER(
        label = "Teñir color de personaje",
        category = ScratchCategory.CHARACTERS,
        defaultParam1 = "dad",
        defaultParam2 = "FF2E93",
        defaultParam3 = "",
        param1Label = "Slot",
        param2Label = "Color Hex",
        param3Label = ""
    ),

    // --- CÁMARA Y ESCENA ---
    CAMERA_ZOOM(
        label = "Zoom de cámara",
        category = ScratchCategory.CAMERA,
        defaultParam1 = "1.15",
        defaultParam2 = "0.04",
        defaultParam3 = "",
        param1Label = "Zoom base",
        param2Label = "Bump",
        param3Label = ""
    ),
    CAMERA_SHAKE(
        label = "Sacudir cámara",
        category = ScratchCategory.CAMERA,
        defaultParam1 = "0.018",
        defaultParam2 = "0.25",
        defaultParam3 = "",
        param1Label = "Intensidad",
        param2Label = "Duración s",
        param3Label = ""
    ),
    CAMERA_FLASH(
        label = "Flash de pantalla",
        category = ScratchCategory.CAMERA,
        defaultParam1 = "FFFFFF",
        defaultParam2 = "0.45",
        defaultParam3 = "",
        param1Label = "Color Hex",
        param2Label = "Duración s",
        param3Label = ""
    ),
    CAMERA_FADE(
        label = "Fundido oscuro (Fade)",
        category = ScratchCategory.CAMERA,
        defaultParam1 = "000000",
        defaultParam2 = "0.8",
        defaultParam3 = "",
        param1Label = "Color Hex",
        param2Label = "Duración s",
        param3Label = ""
    ),
    CAMERA_TILT(
        label = "Inclinar cámara (Ángulo)",
        category = ScratchCategory.CAMERA,
        defaultParam1 = "6",
        defaultParam2 = "0.35",
        defaultParam3 = "",
        param1Label = "Grados°",
        param2Label = "Duración s",
        param3Label = ""
    ),
    CAMERA_FOCUS(
        label = "Enfocar cámara en",
        category = ScratchCategory.CAMERA,
        defaultParam1 = "dad",
        defaultParam2 = "",
        defaultParam3 = "",
        param1Label = "Objetivo",
        param2Label = "",
        param3Label = ""
    ),
    CINEMATIC_BARS(
        label = "Barras cinemáticas (Cine)",
        category = ScratchCategory.CAMERA,
        defaultParam1 = "true",
        defaultParam2 = "80",
        defaultParam3 = "0.4",
        param1Label = "Activar",
        param2Label = "Grosor px",
        param3Label = "Duración s"
    ),

    // --- JUEGO / HUD ---
    CHANGE_HEALTH(
        label = "Cambiar vida",
        category = ScratchCategory.HUD,
        defaultParam1 = "-0.08",
        defaultParam2 = "",
        defaultParam3 = "",
        param1Label = "Cantidad",
        param2Label = "",
        param3Label = ""
    ),
    HEALTH_DRAIN(
        label = "Drenaje de vida del Rival",
        category = ScratchCategory.HUD,
        defaultParam1 = "true",
        defaultParam2 = "0.025",
        defaultParam3 = "",
        param1Label = "Activar",
        param2Label = "Fuerza",
        param3Label = ""
    ),
    SHOW_FLOATING_TEXT(
        label = "Mostrar texto en pantalla",
        category = ScratchCategory.HUD,
        defaultParam1 = "¡A DARLE!",
        defaultParam2 = "1.5",
        defaultParam3 = "",
        param1Label = "Texto",
        param2Label = "Duración s",
        param3Label = ""
    ),
    WARNING_BANNER(
        label = "Alerta de peligro",
        category = ScratchCategory.HUD,
        defaultParam1 = "¡CUIDADO!",
        defaultParam2 = "1.2",
        defaultParam3 = "",
        param1Label = "Mensaje",
        param2Label = "Duración s",
        param3Label = ""
    ),
    PLAY_SOUND_FX(
        label = "Reproducir sonido FX",
        category = ScratchCategory.HUD,
        defaultParam1 = "confirmMenu",
        defaultParam2 = "0.8",
        defaultParam3 = "",
        param1Label = "Sonido",
        param2Label = "Volumen",
        param3Label = ""
    ),
    TOGGLE_HUD(
        label = "Mostrar / Ocultar HUD",
        category = ScratchCategory.HUD,
        defaultParam1 = "false",
        defaultParam2 = "",
        defaultParam3 = "",
        param1Label = "Visible",
        param2Label = "",
        param3Label = ""
    )
}

@Serializable
data class ScratchBlock(
    val id: String = UUID.randomUUID().toString(),
    val triggerType: ScratchTriggerType = ScratchTriggerType.ON_BEAT,
    val triggerValue: Int = 4,
    val actionType: ScratchActionType = ScratchActionType.CAMERA_ZOOM,
    val param1: String = actionType.defaultParam1,
    val param2: String = actionType.defaultParam2,
    val param3: String = actionType.defaultParam3,
    val enabled: Boolean = true
)

object ScratchLuaCompiler {

    /**
     * Generates a clean, crash-free Psych Engine Lua script compatible with BOTH 0.7.3 and 1.0.4.
     * Automatically preloads any swapped characters via addCharacterToList in onCreate().
     */
    fun compileToLua(blocks: List<ScratchBlock>, songName: String): String {
        val active = blocks.filter { it.enabled }
        if (active.isEmpty()) {
            return "-- FunkinCraft Lua Script para $songName\n-- Agrega bloques en la pestaña Eventos para generar lógica aquí.\n"
        }

        val sb = StringBuilder()
        sb.appendLine("-- Generado por FunkinCraft (Compatible con Psych Engine 0.7.3 y 1.0.4)")
        sb.appendLine("-- Canción: $songName")
        sb.appendLine()

        val hasWaveBlock = active.any { it.actionType == ScratchActionType.WAVE_MODCHART }
        val hasShakeArrows = active.any { it.actionType == ScratchActionType.SHAKE_ARROWS }
        val hasFloatChar = active.any { it.actionType == ScratchActionType.FLOAT_CHARACTER }
        val hasHealthDrain = active.any { it.actionType == ScratchActionType.HEALTH_DRAIN }
        val hasCinBars = active.any { it.actionType == ScratchActionType.CINEMATIC_BARS }
        val hasTextBlocks = active.any {
            it.actionType == ScratchActionType.SHOW_FLOATING_TEXT ||
                it.actionType == ScratchActionType.WARNING_BANNER
        }
        val hasStrumSetup = hasWaveBlock || hasShakeArrows ||
            active.any {
                it.actionType == ScratchActionType.SWAP_STRUMS ||
                    it.actionType == ScratchActionType.CENTER_STRUMS ||
                    it.actionType == ScratchActionType.RESET_ARROWS ||
                    it.actionType == ScratchActionType.BUMP_ARROWS
            }

        if (hasWaveBlock) {
            sb.appendLine("local fcWaveAmp = 0")
            sb.appendLine("local fcWaveSpeed = 4")
        }
        if (hasShakeArrows) {
            sb.appendLine("local fcArrowShakeAmp = 0")
            sb.appendLine("local fcArrowShakeTime = 0")
        }
        if (hasFloatChar) {
            sb.appendLine("local fcFloatSlot = ''")
            sb.appendLine("local fcFloatAmp = 0")
            sb.appendLine("local fcFloatSpd = 2.5")
            sb.appendLine("local fcBaseCharY = {}")
        }
        if (hasHealthDrain) {
            sb.appendLine("local fcDrainEnabled = false")
            sb.appendLine("local fcDrainAmount = 0.025")
        }
        if (hasStrumSetup) {
            sb.appendLine("local fcBaseStrumX = {}")
            sb.appendLine("local fcBaseStrumY = {}")
        }
        sb.appendLine()

        val charChanges = active.filter { it.actionType == ScratchActionType.CHANGE_CHARACTER }

        sb.appendLine("function onCreate()")
        if (charChanges.isNotEmpty()) {
            charChanges.distinctBy { "${it.param1}:${it.param2}" }.forEach { b ->
                val slot = sanitizeLuaSlot(b.param1)
                val charId = escapeLua(b.param2.ifBlank { "bf" })
                sb.appendLine("    addCharacterToList('$charId', '$slot')")
            }
        }
        if (hasFloatChar) {
            sb.appendLine("    fcBaseCharY['dad'] = getProperty('dad.y')")
            sb.appendLine("    fcBaseCharY['boyfriend'] = getProperty('boyfriend.y')")
            sb.appendLine("    fcBaseCharY['gf'] = getProperty('gf.y')")
        }
        if (hasCinBars) {
            sb.appendLine("    makeLuaSprite('fcTopBar', '', 0, -200)")
            sb.appendLine("    makeGraphic('fcTopBar', 1280, 200, '000000')")
            sb.appendLine("    setObjectCamera('fcTopBar', 'hud')")
            sb.appendLine("    addLuaSprite('fcTopBar', false)")
            sb.appendLine("    makeLuaSprite('fcBotBar', '', 0, 720)")
            sb.appendLine("    makeGraphic('fcBotBar', 1280, 200, '000000')")
            sb.appendLine("    setObjectCamera('fcBotBar', 'hud')")
            sb.appendLine("    addLuaSprite('fcBotBar', false)")
        }
        if (hasTextBlocks) {
            sb.appendLine("    makeLuaText('fcEventText', '', 900, 0, 140)")
            sb.appendLine("    setTextSize('fcEventText', 38)")
            sb.appendLine("    setTextAlignment('fcEventText', 'center')")
            sb.appendLine("    screenCenter('fcEventText', 'x')")
            sb.appendLine("    setProperty('fcEventText.alpha', 0)")
            sb.appendLine("    addLuaText('fcEventText')")
        }
        sb.appendLine("end")
        sb.appendLine()

        if (hasStrumSetup) {
            sb.appendLine("function onCountdownStarted()")
            sb.appendLine("    for i = 0, 7 do")
            sb.appendLine("        fcBaseStrumX[i] = getPropertyFromGroup('strumLineNotes', i, 'x')")
            sb.appendLine("        fcBaseStrumY[i] = getPropertyFromGroup('strumLineNotes', i, 'y')")
            sb.appendLine("    end")
            sb.appendLine("end")
            sb.appendLine()
        }

        val lowHealthBlocks = active.filter { it.triggerType == ScratchTriggerType.ON_HEALTH_LOW }
        if (hasWaveBlock || hasShakeArrows || hasFloatChar || lowHealthBlocks.isNotEmpty()) {
            if (lowHealthBlocks.isNotEmpty()) {
                sb.appendLine("local fcLowHealthFired = false")
            }
            sb.appendLine("function onUpdate(elapsed)")
            sb.appendLine("    local songPos = getSongPosition() / 1000")
            if (hasWaveBlock) {
                sb.appendLine("    if fcWaveAmp ~= 0 then")
                sb.appendLine("        for i = 0, 7 do")
                sb.appendLine("            local baseY = fcBaseStrumY[i] or getPropertyFromGroup('strumLineNotes', i, 'y')")
                sb.appendLine("            setPropertyFromGroup('strumLineNotes', i, 'y', baseY + math.sin((songPos * fcWaveSpeed) + i * 0.7) * fcWaveAmp)")
                sb.appendLine("        end")
                sb.appendLine("    end")
            }
            if (hasShakeArrows) {
                sb.appendLine("    if fcArrowShakeTime > 0 then")
                sb.appendLine("        fcArrowShakeTime = fcArrowShakeTime - elapsed")
                sb.appendLine("        for i = 0, 7 do")
                sb.appendLine("            local bx = fcBaseStrumX[i] or getPropertyFromGroup('strumLineNotes', i, 'x')")
                sb.appendLine("            local by = fcBaseStrumY[i] or getPropertyFromGroup('strumLineNotes', i, 'y')")
                sb.appendLine("            setPropertyFromGroup('strumLineNotes', i, 'x', bx + math.random(-fcArrowShakeAmp, fcArrowShakeAmp))")
                sb.appendLine("            setPropertyFromGroup('strumLineNotes', i, 'y', by + math.random(-fcArrowShakeAmp, fcArrowShakeAmp))")
                sb.appendLine("        end")
                sb.appendLine("    end")
            }
            if (hasFloatChar) {
                sb.appendLine("    if fcFloatSlot ~= '' and fcFloatAmp ~= 0 then")
                sb.appendLine("        local by = fcBaseCharY[fcFloatSlot] or getProperty(fcFloatSlot..'.y')")
                sb.appendLine("        setProperty(fcFloatSlot..'.y', by + math.sin(songPos * fcFloatSpd) * fcFloatAmp)")
                sb.appendLine("    end")
            }
            if (lowHealthBlocks.isNotEmpty()) {
                sb.appendLine("    if getHealth() < 0.5 and not fcLowHealthFired then")
                sb.appendLine("        fcLowHealthFired = true")
                lowHealthBlocks.forEach { emitAction(sb, it, "        ") }
                sb.appendLine("    elseif getHealth() >= 0.65 then")
                sb.appendLine("        fcLowHealthFired = false")
                sb.appendLine("    end")
            }
            sb.appendLine("end")
            sb.appendLine()
        }

        val startBlocks = active.filter { it.triggerType == ScratchTriggerType.ON_SONG_START }
        if (startBlocks.isNotEmpty()) {
            sb.appendLine("function onSongStart()")
            startBlocks.forEach { emitAction(sb, it, "    ") }
            sb.appendLine("end")
            sb.appendLine()
        }

        val endBlocks = active.filter { it.triggerType == ScratchTriggerType.ON_SONG_END }
        if (endBlocks.isNotEmpty()) {
            sb.appendLine("function onEndSong()")
            endBlocks.forEach { emitAction(sb, it, "    ") }
            sb.appendLine("    return Function_Continue")
            sb.appendLine("end")
            sb.appendLine()
        }

        val beatBlocks = active.filter {
            it.triggerType == ScratchTriggerType.ON_BEAT || it.triggerType == ScratchTriggerType.EVERY_N_BEATS
        }
        if (beatBlocks.isNotEmpty()) {
            sb.appendLine("function onBeatHit()")
            beatBlocks.forEach { b ->
                val n = b.triggerValue.coerceAtLeast(1)
                if (b.triggerType == ScratchTriggerType.ON_BEAT) {
                    sb.appendLine("    if curBeat == $n then")
                } else {
                    sb.appendLine("    if curBeat % $n == 0 then")
                }
                emitAction(sb, b, "        ")
                sb.appendLine("    end")
            }
            sb.appendLine("end")
            sb.appendLine()
        }

        val stepBlocks = active.filter {
            it.triggerType == ScratchTriggerType.ON_STEP || it.triggerType == ScratchTriggerType.EVERY_N_STEPS
        }
        if (stepBlocks.isNotEmpty()) {
            sb.appendLine("function onStepHit()")
            stepBlocks.forEach { b ->
                val n = b.triggerValue.coerceAtLeast(1)
                if (b.triggerType == ScratchTriggerType.ON_STEP) {
                    sb.appendLine("    if curStep == $n then")
                } else {
                    sb.appendLine("    if curStep % $n == 0 then")
                }
                emitAction(sb, b, "        ")
                sb.appendLine("    end")
            }
            sb.appendLine("end")
            sb.appendLine()
        }

        val playerHitBlocks = active.filter { it.triggerType == ScratchTriggerType.ON_PLAYER_HIT }
        if (playerHitBlocks.isNotEmpty()) {
            sb.appendLine("function goodNoteHit(id, direction, noteType, isSustainNote)")
            sb.appendLine("    if not isSustainNote then")
            playerHitBlocks.forEach { emitAction(sb, it, "        ") }
            sb.appendLine("    end")
            sb.appendLine("end")
            sb.appendLine()
        }

        val oppHitBlocks = active.filter { it.triggerType == ScratchTriggerType.ON_OPPONENT_HIT }
        if (oppHitBlocks.isNotEmpty() || hasHealthDrain) {
            sb.appendLine("function opponentNoteHit(id, direction, noteType, isSustainNote)")
            if (hasHealthDrain) {
                sb.appendLine("    if fcDrainEnabled and getHealth() > 0.12 then")
                sb.appendLine("        setHealth(math.max(0.1, getHealth() - fcDrainAmount))")
                sb.appendLine("    end")
            }
            if (oppHitBlocks.isNotEmpty()) {
                sb.appendLine("    if not isSustainNote then")
                oppHitBlocks.forEach { emitAction(sb, it, "        ") }
                sb.appendLine("    end")
            }
            sb.appendLine("end")
            sb.appendLine()
        }

        val missBlocks = active.filter { it.triggerType == ScratchTriggerType.ON_MISS }
        if (missBlocks.isNotEmpty()) {
            sb.appendLine("function noteMiss(id, direction, noteType, isSustainNote)")
            missBlocks.forEach { emitAction(sb, it, "    ") }
            sb.appendLine("end")
            sb.appendLine()
        }

        if (hasTextBlocks) {
            sb.appendLine("function onTimerCompleted(tag)")
            sb.appendLine("    if tag == 'fcHideText' then")
            sb.appendLine("        doTweenAlpha('fcTextOut', 'fcEventText', 0, 0.25, 'linear')")
            sb.appendLine("    end")
            sb.appendLine("end")
        }

        return sb.toString()
    }

    private fun emitAction(sb: StringBuilder, block: ScratchBlock, indent: String) {
        val idTag = block.id.take(6).replace("-", "")
        when (block.actionType) {
            ScratchActionType.MOVE_ARROWS -> {
                val range = strumRange(block.param1)
                val dx = block.param2.toFloatOrNull() ?: 0f
                val dy = block.param3.toFloatOrNull() ?: -25f
                sb.appendLine("${indent}for i = ${range.first}, ${range.last} do")
                if (dx != 0f) {
                    sb.appendLine("${indent}    noteTweenX('fcMoveX_${idTag}_'..i, i, getPropertyFromGroup('strumLineNotes', i, 'x') + ($dx), 0.35, 'quadOut')")
                }
                if (dy != 0f) {
                    sb.appendLine("${indent}    noteTweenY('fcMoveY_${idTag}_'..i, i, getPropertyFromGroup('strumLineNotes', i, 'y') + ($dy), 0.35, 'quadOut')")
                }
                sb.appendLine("${indent}end")
            }

            ScratchActionType.SPIN_ARROWS -> {
                val range = strumRange(block.param1)
                val angle = block.param2.toFloatOrNull() ?: 360f
                val dur = (block.param3.toFloatOrNull() ?: 0.4f).coerceAtLeast(0.05f)
                sb.appendLine("${indent}for i = ${range.first}, ${range.last} do")
                sb.appendLine("${indent}    setPropertyFromGroup('strumLineNotes', i, 'angle', 0)")
                sb.appendLine("${indent}    noteTweenAngle('fcSpin_${idTag}_'..i, i, $angle, $dur, 'quartOut')")
                sb.appendLine("${indent}end")
            }

            ScratchActionType.FADE_ARROWS -> {
                val range = strumRange(block.param1)
                val alpha = (block.param2.toFloatOrNull() ?: 0.35f).coerceIn(0f, 1f)
                val dur = (block.param3.toFloatOrNull() ?: 0.4f).coerceAtLeast(0.05f)
                sb.appendLine("${indent}for i = ${range.first}, ${range.last} do")
                sb.appendLine("${indent}    noteTweenAlpha('fcAlpha_${idTag}_'..i, i, $alpha, $dur, 'linear')")
                sb.appendLine("${indent}end")
            }

            ScratchActionType.WAVE_MODCHART -> {
                val amp = block.param1.toFloatOrNull() ?: 18f
                val spd = block.param2.toFloatOrNull() ?: 4f
                sb.appendLine("${indent}fcWaveAmp = $amp")
                sb.appendLine("${indent}fcWaveSpeed = $spd")
            }

            ScratchActionType.BUMP_ARROWS -> {
                val range = strumRange(block.param1)
                val jump = block.param2.toFloatOrNull() ?: 22f
                val dur = (block.param3.toFloatOrNull() ?: 0.2f).coerceAtLeast(0.05f)
                sb.appendLine("${indent}for i = ${range.first}, ${range.last} do")
                sb.appendLine("${indent}    local baseY = fcBaseStrumY[i] or getPropertyFromGroup('strumLineNotes', i, 'y')")
                sb.appendLine("${indent}    setPropertyFromGroup('strumLineNotes', i, 'y', baseY - ($jump))")
                sb.appendLine("${indent}    noteTweenY('fcBump_${idTag}_'..i, i, baseY, $dur, 'quadOut')")
                sb.appendLine("${indent}end")
            }

            ScratchActionType.SHAKE_ARROWS -> {
                val amp = (block.param1.toFloatOrNull() ?: 12f).coerceIn(1f, 50f)
                val dur = (block.param2.toFloatOrNull() ?: 0.35f).coerceIn(0.05f, 5f)
                sb.appendLine("${indent}fcArrowShakeAmp = $amp")
                sb.appendLine("${indent}fcArrowShakeTime = $dur")
            }

            ScratchActionType.SWAP_STRUMS -> {
                val dur = (block.param1.toFloatOrNull() ?: 0.5f).coerceAtLeast(0.05f)
                sb.appendLine("${indent}for i = 0, 3 do")
                sb.appendLine("${indent}    local ox = fcBaseStrumX[i] or getPropertyFromGroup('strumLineNotes', i, 'x')")
                sb.appendLine("${indent}    local px = fcBaseStrumX[i + 4] or getPropertyFromGroup('strumLineNotes', i + 4, 'x')")
                sb.appendLine("${indent}    noteTweenX('fcSwapO_${idTag}_'..i, i, px, $dur, 'quartOut')")
                sb.appendLine("${indent}    noteTweenX('fcSwapP_${idTag}_'..i, i + 4, ox, $dur, 'quartOut')")
                sb.appendLine("${indent}end")
            }

            ScratchActionType.CENTER_STRUMS -> {
                val enable = block.param1.equals("true", ignoreCase = true)
                val dur = (block.param2.toFloatOrNull() ?: 0.45f).coerceAtLeast(0.05f)
                if (enable) {
                    sb.appendLine("${indent}for i = 0, 3 do noteTweenAlpha('fcMidO_${idTag}_'..i, i, 0.15, $dur, 'linear') end")
                    sb.appendLine("${indent}for i = 4, 7 do noteTweenX('fcMidP_${idTag}_'..i, i, 412 + ((i - 4) * 112), $dur, 'quartOut') end")
                } else {
                    sb.appendLine("${indent}for i = 0, 3 do noteTweenAlpha('fcMidO_${idTag}_'..i, i, 1, $dur, 'linear') end")
                    sb.appendLine("${indent}for i = 4, 7 do noteTweenX('fcMidP_${idTag}_'..i, i, fcBaseStrumX[i] or getPropertyFromGroup('strumLineNotes', i, 'x'), $dur, 'quartOut') end")
                }
            }

            ScratchActionType.RESET_ARROWS -> {
                val dur = (block.param1.toFloatOrNull() ?: 0.4f).coerceAtLeast(0.05f)
                sb.appendLine("${indent}for i = 0, 7 do")
                sb.appendLine("${indent}    noteTweenX('fcRstX_${idTag}_'..i, i, fcBaseStrumX[i] or getPropertyFromGroup('strumLineNotes', i, 'x'), $dur, 'quadOut')")
                sb.appendLine("${indent}    noteTweenY('fcRstY_${idTag}_'..i, i, fcBaseStrumY[i] or getPropertyFromGroup('strumLineNotes', i, 'y'), $dur, 'quadOut')")
                sb.appendLine("${indent}    noteTweenAngle('fcRstA_${idTag}_'..i, i, 0, $dur, 'quadOut')")
                sb.appendLine("${indent}    noteTweenAlpha('fcRstAl_${idTag}_'..i, i, 1, $dur, 'linear')")
                sb.appendLine("${indent}end")
            }

            ScratchActionType.SCROLL_SPEED -> {
                val spd = (block.param1.toFloatOrNull() ?: 3.0f).coerceIn(0.4f, 6.0f)
                val dur = (block.param2.toFloatOrNull() ?: 0.5f).coerceAtLeast(0.01f)
                sb.appendLine("${indent}triggerEvent('Change Scroll Speed', '$spd', '$dur')")
            }

            ScratchActionType.CHANGE_CHARACTER -> {
                val slot = sanitizeLuaSlot(block.param1)
                val charId = escapeLua(block.param2.ifBlank { "bf" })
                sb.appendLine("${indent}triggerEvent('Change Character', '$slot', '$charId')")
            }

            ScratchActionType.PLAY_ANIM -> {
                val target = sanitizeCharProperty(block.param1)
                val anim = escapeLua(block.param2.ifBlank { "hey" })
                sb.appendLine("${indent}playAnim('$target', '$anim', true)")
            }

            ScratchActionType.TOGGLE_CHAR_VISIBLE -> {
                val target = sanitizeCharProperty(block.param1)
                val vis = if (block.param2.equals("true", ignoreCase = true)) "true" else "false"
                sb.appendLine("${indent}setProperty('$target.visible', $vis)")
            }

            ScratchActionType.FLOAT_CHARACTER -> {
                val target = sanitizeCharProperty(block.param1)
                val amp = block.param2.toFloatOrNull() ?: 45f
                val spd = block.param3.toFloatOrNull() ?: 2.5f
                sb.appendLine("${indent}fcFloatSlot = '$target'")
                sb.appendLine("${indent}fcFloatAmp = $amp")
                sb.appendLine("${indent}fcFloatSpd = $spd")
            }

            ScratchActionType.TINT_CHARACTER -> {
                val target = sanitizeCharProperty(block.param1)
                val hex = block.param2.trim().removePrefix("#").take(6).ifBlank { "FFFFFF" }
                sb.appendLine("${indent}setProperty('$target.color', getColorFromHex('$hex'))")
            }

            ScratchActionType.CAMERA_ZOOM -> {
                val baseZoom = (block.param1.toFloatOrNull() ?: 1.15f).coerceIn(0.4f, 2.5f)
                val bump = (block.param2.toFloatOrNull() ?: 0.04f).coerceIn(0f, 0.5f)
                sb.appendLine("${indent}setProperty('defaultCamZoom', $baseZoom)")
                sb.appendLine("${indent}triggerEvent('Add Camera Zoom', '$bump', '$bump')")
            }

            ScratchActionType.CAMERA_SHAKE -> {
                val intensity = (block.param1.toFloatOrNull() ?: 0.018f).coerceIn(0.001f, 0.1f)
                val dur = (block.param2.toFloatOrNull() ?: 0.25f).coerceIn(0.05f, 3f)
                sb.appendLine("${indent}cameraShake('game', $intensity, $dur)")
                sb.appendLine("${indent}cameraShake('hud', ${intensity * 0.6f}, $dur)")
            }

            ScratchActionType.CAMERA_FLASH -> {
                val hex = block.param1.trim().removePrefix("#").take(6).ifBlank { "FFFFFF" }
                val dur = (block.param2.toFloatOrNull() ?: 0.45f).coerceIn(0.05f, 3f)
                sb.appendLine("${indent}cameraFlash('camHUD', '$hex', $dur, true)")
            }

            ScratchActionType.CAMERA_FADE -> {
                val hex = block.param1.trim().removePrefix("#").take(6).ifBlank { "000000" }
                val dur = (block.param2.toFloatOrNull() ?: 0.8f).coerceIn(0.05f, 4f)
                sb.appendLine("${indent}cameraFade('camGame', '$hex', $dur, true)")
            }

            ScratchActionType.CAMERA_TILT -> {
                val deg = (block.param1.toFloatOrNull() ?: 6f).coerceIn(-45f, 45f)
                val dur = (block.param2.toFloatOrNull() ?: 0.35f).coerceIn(0.05f, 3f)
                sb.appendLine("${indent}doTweenAngle('fcCamTilt_$idTag', 'camGame', $deg, $dur, 'quadOut')")
                sb.appendLine("${indent}doTweenAngle('fcHudTilt_$idTag', 'camHUD', ${deg * 0.5f}, $dur, 'quadOut')")
            }

            ScratchActionType.CAMERA_FOCUS -> {
                val target = if (block.param1.lowercase().contains("bf") || block.param1.lowercase().contains("boy")) "boyfriend" else "dad"
                sb.appendLine("${indent}cameraSetTarget('$target')")
            }

            ScratchActionType.CINEMATIC_BARS -> {
                val enable = block.param1.equals("true", ignoreCase = true)
                val thick = (block.param2.toIntOrNull() ?: 80).coerceIn(20, 180)
                val dur = (block.param3.toFloatOrNull() ?: 0.4f).coerceIn(0.05f, 3f)
                if (enable) {
                    sb.appendLine("${indent}doTweenY('fcBarT_$idTag', 'fcTopBar', ${thick - 200}, $dur, 'quartOut')")
                    sb.appendLine("${indent}doTweenY('fcBarB_$idTag', 'fcBotBar', ${720 - thick}, $dur, 'quartOut')")
                } else {
                    sb.appendLine("${indent}doTweenY('fcBarT_$idTag', 'fcTopBar', -200, $dur, 'quartOut')")
                    sb.appendLine("${indent}doTweenY('fcBarB_$idTag', 'fcBotBar', 720, $dur, 'quartOut')")
                }
            }

            ScratchActionType.CHANGE_HEALTH -> {
                val delta = (block.param1.toFloatOrNull() ?: -0.08f).coerceIn(-1.5f, 1.5f)
                sb.appendLine("${indent}setHealth(math.max(0.05, math.min(2.0, getHealth() + ($delta))))")
            }

            ScratchActionType.HEALTH_DRAIN -> {
                val enable = block.param1.equals("true", ignoreCase = true)
                val amt = (block.param2.toFloatOrNull() ?: 0.025f).coerceIn(0.005f, 0.2f)
                sb.appendLine("${indent}fcDrainEnabled = $enable")
                sb.appendLine("${indent}fcDrainAmount = $amt")
            }

            ScratchActionType.SHOW_FLOATING_TEXT -> {
                val txt = escapeLua(block.param1.ifBlank { "¡GO!" })
                val dur = (block.param2.toFloatOrNull() ?: 1.5f).coerceIn(0.2f, 10f)
                sb.appendLine("${indent}setTextColor('fcEventText', 'FFFFFF')")
                sb.appendLine("${indent}setTextString('fcEventText', '$txt')")
                sb.appendLine("${indent}setProperty('fcEventText.alpha', 1)")
                sb.appendLine("${indent}runTimer('fcHideText', $dur)")
            }

            ScratchActionType.WARNING_BANNER -> {
                val txt = escapeLua(block.param1.ifBlank { "¡CUIDADO!" })
                val dur = (block.param2.toFloatOrNull() ?: 1.2f).coerceIn(0.2f, 10f)
                sb.appendLine("${indent}setTextColor('fcEventText', 'FF3B5C')")
                sb.appendLine("${indent}setTextString('fcEventText', '$txt')")
                sb.appendLine("${indent}setProperty('fcEventText.alpha', 1)")
                sb.appendLine("${indent}runTimer('fcHideText', $dur)")
            }

            ScratchActionType.PLAY_SOUND_FX -> {
                val snd = escapeLua(block.param1.ifBlank { "confirmMenu" })
                val vol = (block.param2.toFloatOrNull() ?: 0.8f).coerceIn(0.05f, 1f)
                sb.appendLine("${indent}playSound('$snd', $vol)")
            }

            ScratchActionType.TOGGLE_HUD -> {
                val vis = if (block.param1.equals("true", ignoreCase = true)) "true" else "false"
                sb.appendLine("${indent}setProperty('camHUD.visible', $vis)")
            }
        }
    }

    private fun strumRange(group: String): IntRange = when (group.lowercase()) {
        "opponent", "dad", "rival" -> 0..3
        "player", "bf", "boyfriend" -> 4..7
        else -> 0..7
    }

    private fun sanitizeLuaSlot(slot: String): String = when (slot.lowercase()) {
        "bf", "boyfriend", "player" -> "bf"
        "gf", "girlfriend" -> "gf"
        else -> "dad"
    }

    private fun sanitizeCharProperty(slot: String): String = when (slot.lowercase()) {
        "dad", "opponent", "rival" -> "dad"
        "gf", "girlfriend" -> "gf"
        else -> "boyfriend"
    }

    private fun escapeLua(value: String): String =
        value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ")
}

object CustomNoteLuaCompiler {

    /**
     * Generates a Psych Engine 0.7.3 & 1.0.4 compatible `custom_notetypes/<slug>.lua` script.
     */
    fun compileToLua(noteType: CustomNoteType): String {
        val safeName = noteType.slug.replace("'", "\\'")
        val hexColor = (noteType.colorHex and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()
        val hasCustomTexture = !noteType.customPngPath.isNullOrBlank() || noteType.shape != CustomNoteShape.ARROW
        val textureKey = "customNotes/${slugify(noteType.slug)}"

        return buildString {
            appendLine("-- Nota Personalizada: ${noteType.name} (Generada por FunkinCraft)")
            appendLine("function onCreate()")
            appendLine("    for i = 0, getProperty('unspawnNotes.length') - 1 do")
            appendLine("        if getPropertyFromGroup('unspawnNotes', i, 'noteType') == '$safeName' then")
            if (hasCustomTexture) {
                appendLine("            setPropertyFromGroup('unspawnNotes', i, 'texture', '$textureKey')")
            } else {
                appendLine("            setPropertyFromGroup('unspawnNotes', i, 'color', getColorFromHex('$hexColor'))")
            }
            appendLine("            setPropertyFromGroup('unspawnNotes', i, 'ignoreNote', ${noteType.ignoreNote})")
            appendLine("            setPropertyFromGroup('unspawnNotes', i, 'noAnimation', ${noteType.noAnimation})")
            appendLine("            setPropertyFromGroup('unspawnNotes', i, 'hitHealth', ${noteType.healthDelta})")
            appendLine("            setPropertyFromGroup('unspawnNotes', i, 'missHealth', ${noteType.missPenalty})")
            appendLine("        end")
            appendLine("    end")
            appendLine("end")
            appendLine()
            appendLine("function goodNoteHit(id, noteData, noteType, isSustainNote)")
            appendLine("    if noteType == '$safeName' and not isSustainNote then")
            when (noteType.hitEffect) {
                CustomNoteEffect.NONE -> {}
                CustomNoteEffect.FIRE_DAMAGE -> {
                    appendLine("        cameraShake('game', 0.025, 0.25)")
                    appendLine("        cameraFlash('camHUD', '$hexColor', 0.25, true)")
                    appendLine("        playSound('hitsound', 0.8)")
                }
                CustomNoteEffect.HEAL_GLOW -> {
                    appendLine("        cameraFlash('camHUD', '2EFF9A', 0.25, true)")
                }
                CustomNoteEffect.CAMERA_SHAKE -> {
                    appendLine("        cameraShake('game', 0.03, 0.3)")
                }
                CustomNoteEffect.CAMERA_FLASH -> {
                    appendLine("        cameraFlash('camHUD', '$hexColor', 0.35, true)")
                }
                CustomNoteEffect.SPIN_ARROWS -> {
                    appendLine("        for i = 4, 7 do")
                    appendLine("            setPropertyFromGroup('strumLineNotes', i, 'angle', 0)")
                    appendLine("            noteTweenAngle('fcNoteSpin_'..i, i, 360, 0.35, 'quartOut')")
                    appendLine("        end")
                }
                CustomNoteEffect.PLAY_HEY -> {
                    appendLine("        playAnim('boyfriend', 'hey', true)")
                }
                CustomNoteEffect.DRAIN_POISON -> {
                    appendLine("        setHealth(math.max(0.1, getHealth() - 0.15))")
                    appendLine("        cameraFlash('camHUD', '9B5DE5', 0.25, true)")
                }
            }
            appendLine("    end")
            appendLine("end")
        }
    }
}
