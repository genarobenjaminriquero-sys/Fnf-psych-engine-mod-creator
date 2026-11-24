package com.example

import com.example.engine.AudioSynthEngine
import com.example.engine.ModRepository
import com.example.engine.PsychModExporter
import com.example.model.ChartNote
import com.example.model.ChartSection
import com.example.model.ComposerTrackData
import com.example.model.CustomNoteEffect
import com.example.model.CustomNoteLuaCompiler
import com.example.model.CustomNoteShape
import com.example.model.CustomNoteType
import com.example.model.ModProject
import com.example.model.ScratchActionType
import com.example.model.ScratchBlock
import com.example.model.ScratchLuaCompiler
import com.example.model.ScratchTriggerType
import com.example.model.SongProject
import com.example.model.TargetEngine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PsychModExportTest {

    @Test
    fun scratchLuaCompilerPreloadsSwappedCharactersAndModchart() {
        val blocks = listOf(
            ScratchBlock(
                triggerType = ScratchTriggerType.ON_BEAT,
                triggerValue = 8,
                actionType = ScratchActionType.CHANGE_CHARACTER,
                param1 = "dad",
                param2 = "bf"
            ),
            ScratchBlock(
                triggerType = ScratchTriggerType.ON_BEAT,
                triggerValue = 12,
                actionType = ScratchActionType.SPIN_ARROWS,
                param1 = "all",
                param2 = "360",
                param3 = "0.5"
            ),
            ScratchBlock(
                triggerType = ScratchTriggerType.ON_SONG_START,
                actionType = ScratchActionType.WAVE_MODCHART,
                param1 = "18",
                param2 = "4"
            )
        )
        val lua = ScratchLuaCompiler.compileToLua(blocks, "Test Song")
        assertTrue(lua.contains("addCharacterToList('bf', 'dad')"))
        assertTrue(lua.contains("triggerEvent('Change Character', 'dad', 'bf')"))
        assertTrue(lua.contains("noteTweenAngle"))
        assertTrue(lua.contains("fcWaveAmp = 18"))
    }

    @Test
    fun synthEngineRendersNonSilentStemsAndValidWav() {
        val composer = ComposerTrackData.createDefault()
        val stems = AudioSynthEngine.renderComposerStems(composer, 128f)
        assertTrue(stems.instPcm.any { it.toInt() != 0 })
        assertTrue(stems.voicesPcm.any { it.toInt() != 0 })
        assertEquals(96, stems.waveformPeaks.size)

        val tmpWav = Files.createTempFile("funkincraft_test", ".wav").toFile()
        try {
            AudioSynthEngine.writeWavFile(stems.instPcm, tmpWav)
            assertTrue(tmpWav.exists() && tmpWav.length() > 44)
            val header = tmpWav.readBytes().take(4).toByteArray().decodeToString()
            assertEquals("RIFF", header)
        } finally {
            tmpWav.delete()
        }
    }

    @Test
    fun chartJsonMatchesPsych104And073Formats() {
        val song = SongProject(
            name = "Test Song",
            slug = "test-song",
            bpm = 120f,
            sections = listOf(
                ChartSection(
                    mustHitSection = true,
                    bpm = 120f,
                    notes = listOf(
                        ChartNote(stepInSection = 0, lane = 4), // Player Left
                        ChartNote(stepInSection = 4, lane = 0)  // Opponent Left
                    )
                )
            )
        )

        // Psych 1.0.4 (psych_v1 root format)
        val json104 = Json.parseToJsonElement(
            PsychModExporter.buildChartJson(song, TargetEngine.PSYCH_1_0_4, true)
        ).jsonObject
        assertEquals("psych_v1", json104["format"]?.jsonPrimitive?.content)
        val secNotes104 = json104["notes"]!!.jsonArray[0].jsonObject["sectionNotes"]!!.jsonArray
        assertEquals("0", secNotes104[0].jsonArray[1].jsonPrimitive.content) // Player -> 0
        assertEquals("4", secNotes104[1].jsonArray[1].jsonPrimitive.content) // Opponent -> 4

        // Psych 0.7.3 (legacy "song" wrapper)
        val json073 = Json.parseToJsonElement(
            PsychModExporter.buildChartJson(song, TargetEngine.PSYCH_0_7_3, true)
        ).jsonObject
        assertTrue(json073.containsKey("song"))
        val inner073 = json073["song"]!!.jsonObject
        assertFalse(inner073.containsKey("format"))
    }

    @Test
    fun validationReportPassesForCompleteMod() {
        val composer = ComposerTrackData.createDefault()
        val sections = ModRepository.buildSectionsFromComposer(composer, 128f)
        val proj = ModProject(
            name = "Neon Mod",
            slug = "neon-mod",
            songs = listOf(SongProject(name = "Neon", slug = "neon", sections = sections))
        )
        val report = PsychModExporter.validateMod(proj)
        assertTrue(report.canExport)
        assertTrue(report.checks.all { it.passed })
        assertTrue(report.checks.any { it.title.contains("Notas Personalizadas") })
    }

    @Test
    fun expandedScratchTriggersAndActionsCompileCleanly() {
        val blocks = listOf(
            ScratchBlock(
                triggerType = ScratchTriggerType.EVERY_N_STEPS,
                triggerValue = 4,
                actionType = ScratchActionType.BUMP_ARROWS,
                param1 = "all",
                param2 = "20",
                param3 = "0.2"
            ),
            ScratchBlock(
                triggerType = ScratchTriggerType.ON_HEALTH_LOW,
                actionType = ScratchActionType.WARNING_BANNER,
                param1 = "¡PELIGRO!",
                param2 = "1.5"
            ),
            ScratchBlock(
                triggerType = ScratchTriggerType.ON_BEAT,
                triggerValue = 16,
                actionType = ScratchActionType.CINEMATIC_BARS,
                param1 = "true",
                param2 = "80",
                param3 = "0.4"
            ),
            ScratchBlock(
                triggerType = ScratchTriggerType.ON_BEAT,
                triggerValue = 20,
                actionType = ScratchActionType.SWAP_STRUMS,
                param1 = "0.5"
            ),
            ScratchBlock(
                triggerType = ScratchTriggerType.ON_BEAT,
                triggerValue = 24,
                actionType = ScratchActionType.FLOAT_CHARACTER,
                param1 = "dad",
                param2 = "40",
                param3 = "2.5"
            ),
            ScratchBlock(
                triggerType = ScratchTriggerType.ON_SONG_START,
                actionType = ScratchActionType.HEALTH_DRAIN,
                param1 = "true",
                param2 = "0.03"
            ),
            ScratchBlock(
                triggerType = ScratchTriggerType.ON_SONG_END,
                actionType = ScratchActionType.CAMERA_FADE,
                param1 = "000000",
                param2 = "0.8"
            )
        )
        val lua = ScratchLuaCompiler.compileToLua(blocks, "Boss Rush")
        assertTrue(lua.contains("curStep % 4 == 0"))
        assertTrue(lua.contains("getHealth() < 0.5"))
        assertTrue(lua.contains("fcTopBar"))
        assertTrue(lua.contains("fcSwapO_"))
        assertTrue(lua.contains("fcFloatSlot = 'dad'"))
        assertTrue(lua.contains("fcDrainEnabled = true"))
        assertTrue(lua.contains("function onEndSong()"))
        assertTrue(lua.contains("cameraFade('camGame'"))
    }

    @Test
    fun customNoteLuaCompilerGeneratesValidPsychScript() {
        val poisonNote = CustomNoteType(
            name = "Nota Veneno",
            slug = "Poison Note",
            colorHex = 0xFF9B5DE5,
            shape = CustomNoteShape.GLITCH_BLOCK,
            ignoreNote = true,
            noAnimation = true,
            healthDelta = -0.25f,
            missPenalty = 0f,
            hitEffect = CustomNoteEffect.DRAIN_POISON,
            hitMessage = "¡VENENO!"
        )
        val lua = CustomNoteLuaCompiler.compileToLua(poisonNote)
        assertTrue(lua.contains("'Poison Note'"))
        assertTrue(lua.contains("customNotes/poison-note"))
        assertTrue(lua.contains("'ignoreNote', true"))
        assertTrue(lua.contains("'noAnimation', true"))
        assertTrue(lua.contains("'hitHealth', -0.25"))
        assertTrue(lua.contains("setHealth(math.max(0.1, getHealth() - 0.15))"))
    }
}
