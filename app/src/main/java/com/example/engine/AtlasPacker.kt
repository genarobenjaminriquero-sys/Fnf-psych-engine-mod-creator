package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.model.CharacterAsset
import com.example.model.CustomNoteShape
import com.example.model.CustomNoteType
import com.example.model.NoteSkinAsset
import com.example.model.NoteSkinMode
import com.example.model.SpriteMode
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

object AtlasPacker {

    data class PackedAtlasResult(
        val pngFile: File,
        val xmlFile: File
    )

    private val defaultAnims = listOf("idle", "singLEFT", "singDOWN", "singUP", "singRIGHT")

    fun animPrefixFor(animKey: String): String = when (animKey) {
        "idle" -> "idle"
        "singLEFT" -> "left"
        "singDOWN" -> "down"
        "singUP" -> "up"
        "singRIGHT" -> "right"
        else -> animKey
    }

    /**
     * Builds a valid PNG spritesheet + Sparrow v2 XML for a CharacterAsset.
     * Supports:
     * 1. IMPORTED_ATLAS (uses existing PNG+XML directly)
     * 2. IMPORTED_FRAMES (packs user's individual PNG frames per animation into an atlas!)
     * 3. BUILTIN_VECTOR (renders a crisp stylized animated character spritesheet so zero errors ever occur!)
     */
    fun buildCharacterAtlas(char: CharacterAsset, outDir: File, fileBaseName: String): PackedAtlasResult {
        outDir.mkdirs()
        val outPng = File(outDir, "$fileBaseName.png")
        val outXml = File(outDir, "$fileBaseName.xml")

        if (char.spriteMode == SpriteMode.IMPORTED_ATLAS &&
            !char.atlasPngPath.isNullOrBlank() &&
            !char.atlasXmlPath.isNullOrBlank()
        ) {
            val srcPng = File(char.atlasPngPath)
            val srcXml = File(char.atlasXmlPath)
            if (srcPng.exists() && srcXml.exists()) {
                srcPng.copyTo(outPng, overwrite = true)
                val xmlContent = srcXml.readText()
                    .replace(Regex("""imagePath="[^"]*""""), """imagePath="$fileBaseName.png"""")
                outXml.writeText(xmlContent)
                return PackedAtlasResult(outPng, outXml)
            }
        }

        // Gather frames per animation
        val frameEntries = mutableListOf<Triple<String, Int, Bitmap>>()
        val targetCellW = 340
        val targetCellH = 400

        if (char.spriteMode == SpriteMode.IMPORTED_FRAMES && char.animFrames.isNotEmpty()) {
            for (anim in defaultAnims) {
                val paths = char.animFrames[anim].orEmpty()
                if (paths.isNotEmpty()) {
                    paths.forEachIndexed { idx, p ->
                        val bmp = decodeScaledBitmap(File(p), targetCellW, targetCellH)
                        if (bmp != null) {
                            frameEntries.add(Triple(animPrefixFor(anim), idx, bmp))
                        }
                    }
                } else {
                    // Fallback to idle frame or procedural frame so Psych Engine never crashes on missing anim
                    val fallbackPath = char.animFrames["idle"]?.firstOrNull()
                    val bmp = fallbackPath?.let { decodeScaledBitmap(File(it), targetCellW, targetCellH) }
                        ?: renderProceduralCharacterFrame(char, anim, 0, targetCellW, targetCellH)
                    frameEntries.add(Triple(animPrefixFor(anim), 0, bmp))
                }
            }
        } else {
            // BUILTIN_VECTOR: 2 frames per animation for snappy FNF idle/sing bounce
            for (anim in defaultAnims) {
                for (frameIdx in 0..1) {
                    val bmp = renderProceduralCharacterFrame(char, anim, frameIdx, targetCellW, targetCellH)
                    frameEntries.add(Triple(animPrefixFor(anim), frameIdx, bmp))
                }
            }
        }

        val cols = max(1, ceil(sqrt(frameEntries.size.toDouble())).toInt())
        val rows = max(1, ceil(frameEntries.size.toDouble() / cols).toInt())
        val pad = 4
        val sheetW = cols * (targetCellW + pad) + pad
        val sheetH = rows * (targetCellH + pad) + pad

        val atlasBitmap = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(atlasBitmap)
        val xml = StringBuilder()
        xml.appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        xml.appendLine("""<TextureAtlas imagePath="$fileBaseName.png">""")

        frameEntries.forEachIndexed { idx, (prefix, frameNum, bmp) ->
            val col = idx % cols
            val row = idx / cols
            val x = pad + col * (targetCellW + pad)
            val y = pad + row * (targetCellH + pad)
            val drawX = x + (targetCellW - bmp.width) / 2f
            val drawY = (y + (targetCellH - bmp.height)).toFloat()
            canvas.drawBitmap(bmp, drawX, drawY, null)

            val paddedIdx = frameNum.toString().padStart(4, '0')
            xml.appendLine(
                """    <SubTexture name="$prefix$paddedIdx" x="$x" y="$y" width="$targetCellW" height="$targetCellH" frameX="0" frameY="0" frameWidth="$targetCellW" frameHeight="$targetCellH"/>"""
            )
            bmp.recycle()
        }

        xml.appendLine("</TextureAtlas>")

        FileOutputStream(outPng).use { fos ->
            atlasBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        atlasBitmap.recycle()
        outXml.writeText(xml.toString())
        return PackedAtlasResult(outPng, outXml)
    }

    /**
     * Generates a valid 300x150 dual-frame Psych Engine health icon PNG (`icon-<name>.png`).
     */
    fun buildHealthIcon(char: CharacterAsset, outFile: File) {
        outFile.parentFile?.mkdirs()
        val outBmp = Bitmap.createBitmap(300, 150, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBmp)

        val customFile = char.iconPngPath?.let(::File)?.takeIf { it.exists() }
        if (customFile != null) {
            val src = BitmapFactory.decodeFile(customFile.absolutePath)
            if (src != null) {
                if (src.width >= src.height * 1.7f) {
                    // Already a dual-frame 2:1 health icon sheet
                    val scaled = Bitmap.createScaledBitmap(src, 300, 150, true)
                    canvas.drawBitmap(scaled, 0f, 0f, null)
                    scaled.recycle()
                } else {
                    // Single icon image: mirror/tint slightly for losing frame on right
                    val single = Bitmap.createScaledBitmap(src, 130, 130, true)
                    canvas.drawBitmap(single, 10f, 10f, null)
                    canvas.save()
                    canvas.translate(225f, 75f)
                    canvas.rotate(10f)
                    val losePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 215 }
                    canvas.drawBitmap(single, -65f, -65f, losePaint)
                    canvas.restore()
                    single.recycle()
                }
                src.recycle()
                FileOutputStream(outFile).use { outBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                outBmp.recycle()
                return
            }
        }

        // Procedural crisp geometric health icon
        val primary = char.primaryColorHex.toInt()
        val accent = char.accentColorHex.toInt()
        drawProceduralIconFace(canvas, 75f, 75f, primary, accent, losing = false)
        drawProceduralIconFace(canvas, 225f, 75f, primary, accent, losing = true)

        FileOutputStream(outFile).use { outBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        outBmp.recycle()
    }

    private fun drawProceduralIconFace(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        primary: Int,
        accent: Int,
        losing: Boolean
    ) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primary }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B0D12")
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
        }

        canvas.drawCircle(cx, cy, 48f, fill)
        canvas.drawCircle(cx, cy, 48f, stroke)

        if (!losing) {
            canvas.drawCircle(cx - 16f, cy - 8f, 8f, eyePaint)
            canvas.drawCircle(cx + 16f, cy - 8f, 8f, eyePaint)
            canvas.drawArc(RectF(cx - 18f, cy + 4f, cx + 18f, cy + 26f), 10f, 160f, false, dark)
        } else {
            // X eyes + wavy mouth
            canvas.drawLine(cx - 22f, cy - 14f, cx - 10f, cy - 2f, dark)
            canvas.drawLine(cx - 10f, cy - 14f, cx - 22f, cy - 2f, dark)
            canvas.drawLine(cx + 10f, cy - 14f, cx + 22f, cy - 2f, dark)
            canvas.drawLine(cx + 22f, cy - 14f, cx + 10f, cy - 2f, dark)
            canvas.drawArc(RectF(cx - 16f, cy + 12f, cx + 16f, cy + 30f), 190f, 160f, false, dark)
        }
    }

    fun buildPackIcon(packIconPath: String?, rgb: List<Int>, outFile: File) {
        outFile.parentFile?.mkdirs()
        if (!packIconPath.isNullOrBlank() && File(packIconPath).exists()) {
            val bmp = decodeScaledBitmap(File(packIconPath), 150, 150)
            if (bmp != null) {
                val square = Bitmap.createScaledBitmap(bmp, 150, 150, true)
                FileOutputStream(outFile).use { square.compress(Bitmap.CompressFormat.PNG, 100, it) }
                square.recycle()
                bmp.recycle()
                return
            }
        }
        val bmp = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#0B0D12"))
        val accent = Color.rgb(
            rgb.getOrElse(0) { 0 }.coerceIn(0, 255),
            rgb.getOrElse(1) { 229 }.coerceIn(0, 255),
            rgb.getOrElse(2) { 255 }.coerceIn(0, 255)
        )
        drawArrowVector(c, 75f, 75f, 44f, 2, accent, filled = true)
        FileOutputStream(outFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
    }

    /**
     * Generates complete Psych Engine NOTE_assets.png + NOTE_assets.xml when custom note skin is enabled.
     */
    fun buildNoteSkinAtlas(skin: NoteSkinAsset, outDir: File): PackedAtlasResult? {
        if (skin.mode == NoteSkinMode.DEFAULT) return null
        outDir.mkdirs()
        val outPng = File(outDir, "NOTE_assets.png")
        val outXml = File(outDir, "NOTE_assets.xml")

        if (skin.mode == NoteSkinMode.IMPORTED_ATLAS &&
            !skin.atlasPngPath.isNullOrBlank() &&
            !skin.atlasXmlPath.isNullOrBlank()
        ) {
            val p = File(skin.atlasPngPath)
            val x = File(skin.atlasXmlPath)
            if (p.exists() && x.exists()) {
                p.copyTo(outPng, overwrite = true)
                outXml.writeText(x.readText().replace(Regex("""imagePath="[^"]*""""), """imagePath="NOTE_assets.png""""))
                return PackedAtlasResult(outPng, outXml)
            }
        }

        // Generate complete 4-column x 6-row NOTE_assets atlas covering all Psych Engine subtextures
        val cell = 160
        val cols = 4
        val rows = 6
        val bmp = Bitmap.createBitmap(cols * cell, rows * cell, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val xml = StringBuilder()
        xml.appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        xml.appendLine("""<TextureAtlas imagePath="NOTE_assets.png">""")

        val colorNames = listOf("purple", "blue", "green", "red")
        val dirUpper = listOf("LEFT", "DOWN", "UP", "RIGHT")
        val dirLower = listOf("left", "down", "up", "right")
        val laneColors = List(4) { i -> skin.customColorsHex.getOrElse(i) { 0xFF00E5FF }.toInt() }

        val customBitmaps = (0..3).associateWith { lane ->
            skin.customArrowPaths[lane]?.let(::File)?.takeIf { it.exists() }?.let {
                decodeScaledBitmap(it, 132, 132)
            }
        }

        fun addSub(name: String, col: Int, row: Int, w: Int = cell, h: Int = cell) {
            val x = col * cell
            val y = row * cell
            xml.appendLine("""    <SubTexture name="$name" x="$x" y="$y" width="$w" height="$h" frameX="0" frameY="0" frameWidth="$w" frameHeight="$h"/>""")
        }

        for (lane in 0..3) {
            val cx = lane * cell + cell / 2f
            val custom = customBitmaps[lane]
            val colInt = laneColors[lane]

            // Row 0: Colored notes (purple0000, blue0000, green0000, red0000)
            if (custom != null) {
                canvas.drawBitmap(custom, lane * cell + (cell - custom.width) / 2f, (cell - custom.height) / 2f, null)
            } else {
                drawArrowVector(canvas, cx, cell / 2f, 54f, lane, colInt, filled = true)
            }
            addSub("${colorNames[lane]}0000", lane, 0)

            // Row 1: Static strum arrows (arrowLEFT0000, etc.)
            drawArrowVector(canvas, cx, 1 * cell + cell / 2f, 54f, lane, Color.parseColor("#8E99AD"), filled = false)
            addSub("arrow${dirUpper[lane]}0000", lane, 1)

            // Row 2: Press (left press0000)
            drawArrowVector(canvas, cx, 2 * cell + cell / 2f, 48f, lane, colInt, filled = false)
            addSub("${dirLower[lane]} press0000", lane, 2)

            // Row 3: Confirm (left confirm0000)
            drawArrowVector(canvas, cx, 3 * cell + cell / 2f, 62f, lane, colInt, filled = true)
            addSub("${dirLower[lane]} confirm0000", lane, 3)

            // Row 4: Hold piece
            val holdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colInt; alpha = 195 }
            val hx = lane * cell + cell / 2f - 22f
            val hy = 4 * cell.toFloat()
            canvas.drawRect(hx, hy, hx + 44f, hy + cell, holdPaint)
            addSub("${colorNames[lane]} hold piece0000", lane, 4, w = cell, h = cell)

            // Row 5: Hold end
            canvas.drawRoundRect(
                RectF(hx, 5 * cell.toFloat(), hx + 44f, 5 * cell + 80f),
                20f, 20f, holdPaint
            )
            addSub("${colorNames[lane]} hold end0000", lane, 5, w = cell, h = 80)

            custom?.recycle()
        }

        xml.appendLine("</TextureAtlas>")
        FileOutputStream(outPng).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        outXml.writeText(xml.toString())
        return PackedAtlasResult(outPng, outXml)
    }

    /**
     * Generates a valid Sparrow v2 spritesheet for a CustomNoteType (`images/customNotes/<slug>.png` + `.xml`).
     */
    fun buildCustomNoteAtlas(
        noteType: CustomNoteType,
        outDir: File,
        fileBaseName: String
    ): PackedAtlasResult {
        outDir.mkdirs()
        val outPng = File(outDir, "$fileBaseName.png")
        val outXml = File(outDir, "$fileBaseName.xml")

        val cell = 160
        val cols = 4
        val rows = 3
        val bmp = Bitmap.createBitmap(cols * cell, rows * cell, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val xml = StringBuilder()
        xml.appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        xml.appendLine("""<TextureAtlas imagePath="$fileBaseName.png">""")

        val colorNames = listOf("purple", "blue", "green", "red")
        val noteColor = noteType.colorHex.toInt()
        val customBmp = noteType.customPngPath?.let(::File)?.takeIf { it.exists() }?.let {
            decodeScaledBitmap(it, 134, 134)
        }

        fun addSub(name: String, col: Int, row: Int, w: Int = cell, h: Int = cell) {
            val x = col * cell
            val y = row * cell
            xml.appendLine("""    <SubTexture name="$name" x="$x" y="$y" width="$w" height="$h" frameX="0" frameY="0" frameWidth="$w" frameHeight="$h"/>""")
        }

        for (lane in 0..3) {
            val cx = lane * cell + cell / 2f
            val cy = cell / 2f
            if (customBmp != null) {
                canvas.drawBitmap(
                    customBmp,
                    lane * cell + (cell - customBmp.width) / 2f,
                    (cell - customBmp.height) / 2f,
                    null
                )
            } else {
                drawCustomNoteShape(canvas, cx, cy, 54f, lane, noteColor, noteType.shape)
            }
            addSub("${colorNames[lane]}0000", lane, 0)

            val holdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = noteColor; alpha = 195 }
            val hx = lane * cell + cell / 2f - 22f
            val hy = cell.toFloat()
            canvas.drawRect(hx, hy, hx + 44f, hy + cell, holdPaint)
            addSub("${colorNames[lane]} hold piece0000", lane, 1, w = cell, h = cell)

            canvas.drawRoundRect(
                RectF(hx, 2 * cell.toFloat(), hx + 44f, 2 * cell + 80f),
                20f, 20f, holdPaint
            )
            addSub("${colorNames[lane]} hold end0000", lane, 2, w = cell, h = 80)
        }

        customBmp?.recycle()
        xml.appendLine("</TextureAtlas>")
        FileOutputStream(outPng).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        outXml.writeText(xml.toString())
        return PackedAtlasResult(outPng, outXml)
    }

    private fun drawCustomNoteShape(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        lane: Int,
        color: Int,
        shape: CustomNoteShape
    ) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.parseColor("#0B0D12") }

        when (shape) {
            CustomNoteShape.ARROW -> drawArrowVector(canvas, cx, cy, radius, lane, color, filled = true)
            CustomNoteShape.MINE_SPIKE -> {
                val path = Path()
                val spikes = 8
                for (i in 0 until spikes * 2) {
                    val r = if (i % 2 == 0) radius * 1.05f else radius * 0.56f
                    val angle = Math.toRadians((i * 180.0 / spikes) - 90.0)
                    val px = cx + (r * kotlin.math.cos(angle)).toFloat()
                    val py = cy + (r * kotlin.math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, fill)
                canvas.drawPath(path, stroke)
                canvas.drawCircle(cx, cy, radius * 0.24f, inner)
            }
            CustomNoteShape.HEART_HEAL -> {
                val path = Path().apply {
                    moveTo(cx, cy + radius * 0.85f)
                    cubicTo(cx - radius * 1.15f, cy + radius * 0.2f, cx - radius * 0.9f, cy - radius * 0.85f, cx, cy - radius * 0.28f)
                    cubicTo(cx + radius * 0.9f, cy - radius * 0.85f, cx + radius * 1.15f, cy + radius * 0.2f, cx, cy + radius * 0.85f)
                    close()
                }
                canvas.drawPath(path, fill)
                canvas.drawPath(path, stroke)
            }
            CustomNoteShape.STAR_PARRY -> {
                val path = Path()
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) radius else radius * 0.44f
                    val angle = Math.toRadians((i * 36.0) - 90.0)
                    val px = cx + (r * kotlin.math.cos(angle)).toFloat()
                    val py = cy + (r * kotlin.math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, fill)
                canvas.drawPath(path, stroke)
            }
            CustomNoteShape.GLITCH_BLOCK -> {
                val rect = RectF(cx - radius * 0.82f, cy - radius * 0.82f, cx + radius * 0.82f, cy + radius * 0.82f)
                canvas.drawRoundRect(rect, 14f, 14f, fill)
                canvas.drawRoundRect(rect, 14f, 14f, stroke)
                canvas.drawRect(cx - radius * 0.5f, cy - 10f, cx + radius * 0.5f, cy + 10f, inner)
            }
        }
    }

    private fun decodeScaledBitmap(file: File, maxW: Int, maxH: Int): Bitmap? {
        if (!file.exists()) return null
        val src = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val scale = minOf(maxW.toFloat() / src.width, maxH.toFloat() / src.height, 1f)
        val w = max(1, (src.width * scale).toInt())
        val h = max(1, (src.height * scale).toInt())
        val resized = Bitmap.createScaledBitmap(src, w, h, true)
        if (resized !== src) src.recycle()
        return resized
    }

    private fun renderProceduralCharacterFrame(
        char: CharacterAsset,
        anim: String,
        frameIdx: Int,
        w: Int,
        h: Int
    ): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val primary = char.primaryColorHex.toInt()
        val accent = char.accentColorHex.toInt()

        val bounce = if (frameIdx == 1) 8f else 0f
        val leanX = when (anim) {
            "singLEFT" -> -24f
            "singRIGHT" -> 24f
            else -> 0f
        }
        val leanY = when (anim) {
            "singDOWN" -> 22f
            "singUP" -> -22f
            else -> bounce
        }

        val cx = w / 2f + leanX
        val baseY = h - 24f

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 0, 0, 0) }
        c.drawOval(RectF(cx - 75f, baseY - 18f, cx + 75f, baseY + 8f), shadowPaint)

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primary }
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }

        // Torso
        val torso = RectF(cx - 58f, baseY - 170f + leanY, cx + 58f, baseY - 20f)
        c.drawRoundRect(torso, 30f, 30f, bodyPaint)
        c.drawRoundRect(torso, 30f, 30f, outline)

        // Head
        val headY = baseY - 228f + leanY * 1.15f
        c.drawCircle(cx, headY, 56f, bodyPaint)
        c.drawCircle(cx, headY, 56f, outline)

        // Expressive Eyes + Mouth
        c.drawCircle(cx - 18f, headY - 8f, 9f, accentPaint)
        c.drawCircle(cx + 18f, headY - 8f, 9f, accentPaint)

        val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B0D12") }
        if (anim.startsWith("sing")) {
            c.drawOval(RectF(cx - 16f, headY + 10f, cx + 16f, headY + 32f), mouthPaint)
        } else {
            c.drawRoundRect(RectF(cx - 16f, headY + 14f, cx + 16f, headY + 22f), 4f, 4f, mouthPaint)
        }

        // Mic / Accent prop
        c.drawCircle(cx + (if (char.flipX) -62f else 62f), baseY - 110f + leanY, 16f, accentPaint)
        return bmp
    }

    private fun drawArrowVector(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        lane: Int,
        color: Int,
        filled: Boolean
    ) {
        canvas.save()
        canvas.translate(cx, cy)
        val rot = when (lane % 4) {
            0 -> -90f // Left
            1 -> 180f // Down
            2 -> 0f   // Up
            else -> 90f // Right
        }
        canvas.rotate(rot)

        val path = Path().apply {
            moveTo(0f, -radius)
            lineTo(radius * 0.92f, 0f)
            lineTo(radius * 0.36f, 0f)
            lineTo(radius * 0.36f, radius * 0.85f)
            lineTo(-radius * 0.36f, radius * 0.85f)
            lineTo(-radius * 0.36f, 0f)
            lineTo(-radius * 0.92f, 0f)
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = if (filled) Paint.Style.FILL else Paint.Style.STROKE
            strokeWidth = 7f
        }
        canvas.drawPath(path, paint)
        if (filled) {
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawPath(path, border)
        }
        canvas.restore()
    }
}
