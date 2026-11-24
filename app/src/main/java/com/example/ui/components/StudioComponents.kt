package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StudioCard(
    modifier: Modifier = Modifier,
    borderColor: Color = HairlineBorder,
    backgroundColor: Color = StudioSurface,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor
    ) {
        content()
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun PillToggle(
    label: String,
    selected: Boolean,
    accent: Color = StudioCyan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) accent.copy(alpha = 0.16f) else StudioSurfaceElevated
    val border = if (selected) accent else HairlineBorder
    val fg = if (selected) accent else TextSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun FnfArrowIcon(
    lane: Int,
    color: Color,
    filled: Boolean = true,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension * 0.40f
        val rot = when (lane % 4) {
            0 -> -90f
            1 -> 180f
            2 -> 0f
            else -> 90f
        }
        rotate(degrees = rot) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val path = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r * 0.92f, cy)
                lineTo(cx + r * 0.36f, cy)
                lineTo(cx + r * 0.36f, cy + r * 0.86f)
                lineTo(cx - r * 0.36f, cy + r * 0.86f)
                lineTo(cx - r * 0.36f, cy)
                lineTo(cx - r * 0.92f, cy)
                close()
            }
            if (filled) {
                drawPath(path, color = color)
                drawPath(
                    path,
                    color = Color.White.copy(alpha = 0.85f),
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else {
                drawPath(
                    path,
                    color = color,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

@Composable
fun WaveformCanvas(
    peaks: List<Float>,
    progress: Float,
    accentColor: Color = StudioCyan,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(StudioSurfaceElevated)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        val data = if (peaks.isEmpty()) List(64) { 0.15f } else peaks
        val barWidth = size.width / (data.size * 1.6f)
        val gap = (size.width - barWidth * data.size) / (data.size - 1).coerceAtLeast(1)
        val centerY = size.height / 2f

        data.forEachIndexed { idx, amp ->
            val x = idx * (barWidth + gap)
            val barH = (amp.coerceIn(0.06f, 1f) * size.height).coerceAtLeast(4.dp.toPx())
            val ratio = idx.toFloat() / data.size
            val col = if (ratio <= progress) accentColor else TextMuted.copy(alpha = 0.45f)
            drawRoundRect(
                color = col,
                topLeft = Offset(x, centerY - barH / 2f),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun ColorSwatchRow(
    selectedColorHex: Long,
    onSelect: (Long) -> Unit
) {
    val swatches = listOf(
        0xFF00E5FFL,
        0xFFFF2E93L,
        0xFF2EFF9AL,
        0xFFFF5E5BL,
        0xFF8B5CF6L,
        0xFFFFB020L,
        0xFFF3F6FCL,
        0xFF38BDF8L
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        swatches.forEach { hex ->
            val selected = selectedColorHex == hex
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(hex))
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = if (selected) Color.White else HairlineBorder,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}
