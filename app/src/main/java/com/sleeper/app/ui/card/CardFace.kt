package com.sleeper.app.ui.card

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.sleeper.app.model.PlayingCard
import com.sleeper.app.model.Rank
import com.sleeper.app.model.Suit
import kotlin.math.cos
import kotlin.math.sin

// Standard poker card ratio: 2.5 x 3.5 inches = 5:7
private const val CARD_ASPECT_RATIO = 5f / 7f

// Bicycle-style colors
private val CARD_WHITE = Color(0xFFFAF8F5)
private val BICYCLE_RED = Color(0xFFC8102E)
private val BICYCLE_BLACK = Color(0xFF1A1A1A)
private val BORDER_COLOR = Color(0xFFB8B8B8)
private val INNER_BORDER = Color(0xFFD4D0C8)
private val FACE_GOLD = Color(0xFFD4A843)
private val FACE_BLUE = Color(0xFF2B5797)
private val FACE_RED_ROBE = Color(0xFFAA2233)
private val FACE_CREAM = Color(0xFFF5E6C8)

@Composable
fun CardFace(card: PlayingCard, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CARD_ASPECT_RATIO)
    ) {
        val w = size.width
        val h = size.height
        val cornerR = w * 0.055f
        val suitColor = if (card.suit.isRed) BICYCLE_RED else BICYCLE_BLACK

        // === Card background ===
        drawRoundRect(
            color = CARD_WHITE,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(cornerR)
        )

        // === Outer border ===
        drawRoundRect(
            color = BORDER_COLOR,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(cornerR),
            style = Stroke(width = w * 0.006f)
        )

        // === Inner decorative border (classic Bicycle double-border) ===
        val innerInset = w * 0.035f
        drawRoundRect(
            color = INNER_BORDER,
            topLeft = Offset(innerInset, innerInset),
            size = Size(w - innerInset * 2, h - innerInset * 2),
            cornerRadius = CornerRadius(cornerR * 0.6f),
            style = Stroke(width = w * 0.003f)
        )

        // === Corner indices (rank + suit) ===
        val rankFontSize = (w * 0.115f).sp
        val cornerSuitSize = (w * 0.09f).sp

        val rankStyle = TextStyle(
            color = suitColor,
            fontSize = rankFontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center
        )
        val cornerSuitStyle = TextStyle(
            color = suitColor,
            fontSize = cornerSuitSize,
            fontFamily = FontFamily.Default,
            textAlign = TextAlign.Center
        )

        val rankResult = textMeasurer.measure(card.rank.symbol, rankStyle)
        val cornerSuitResult = textMeasurer.measure(card.suit.symbol, cornerSuitStyle)

        val cX = w * 0.085f
        val cY = h * 0.04f
        val suitGap = rankResult.size.height * 0.75f

        // Top-left corner
        drawText(rankResult, topLeft = Offset(cX - rankResult.size.width / 2f, cY))
        drawText(
            cornerSuitResult,
            topLeft = Offset(cX - cornerSuitResult.size.width / 2f, cY + suitGap)
        )

        // Bottom-right corner (rotated 180°)
        rotate(180f, pivot = Offset(w / 2f, h / 2f)) {
            drawText(rankResult, topLeft = Offset(cX - rankResult.size.width / 2f, cY))
            drawText(
                cornerSuitResult,
                topLeft = Offset(cX - cornerSuitResult.size.width / 2f, cY + suitGap)
            )
        }

        // === Center content ===
        val isFaceCard = card.rank in listOf(Rank.JACK, Rank.QUEEN, Rank.KING)

        if (card.rank == Rank.ACE) {
            drawAce(card.suit, suitColor, w, h)
        } else if (isFaceCard) {
            drawBicycleFaceCard(card, suitColor, w, h)
        } else {
            drawPips(card, suitColor, w, h)
        }
    }
}

// ========== ACE RENDERING ==========

private fun DrawScope.drawAce(suit: Suit, suitColor: Color, w: Float, h: Float) {
    val cx = w / 2f
    val cy = h / 2f

    if (suit == Suit.SPADES) {
        // Ornate Ace of Spades — Bicycle signature card
        drawOrnateSpade(cx, cy, w * 0.28f, suitColor, w)
    } else {
        // Large centered suit symbol
        drawLargeSuit(suit, cx, cy, w * 0.22f, suitColor)
    }
}

private fun DrawScope.drawOrnateSpade(cx: Float, cy: Float, baseSize: Float, color: Color, w: Float) {
    val size = baseSize * 1.2f

    // Ornate border circle behind the spade
    drawCircle(
        color = color.copy(alpha = 0.08f),
        radius = size * 1.4f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = color.copy(alpha = 0.15f),
        radius = size * 1.4f,
        center = Offset(cx, cy),
        style = Stroke(width = w * 0.003f)
    )

    // Decorative ring
    drawCircle(
        color = color.copy(alpha = 0.12f),
        radius = size * 1.25f,
        center = Offset(cx, cy),
        style = Stroke(width = w * 0.002f)
    )

    // Draw the spade shape
    drawSpadePath(cx, cy - size * 0.1f, size, color)

    // Small decorative dots around the spade
    val dotRadius = w * 0.006f
    val ringR = size * 1.32f
    for (i in 0 until 12) {
        val angle = Math.toRadians((i * 30.0) - 90.0)
        val dx = cx + ringR * cos(angle).toFloat()
        val dy = cy + ringR * sin(angle).toFloat()
        drawCircle(color = color.copy(alpha = 0.2f), radius = dotRadius, center = Offset(dx, dy))
    }
}

private fun DrawScope.drawSpadePath(cx: Float, cy: Float, size: Float, color: Color) {
    val path = Path().apply {
        // Top point
        moveTo(cx, cy - size * 0.9f)
        // Left curve
        cubicTo(
            cx - size * 0.15f, cy - size * 0.7f,
            cx - size * 0.8f, cy - size * 0.3f,
            cx - size * 0.6f, cy + size * 0.15f
        )
        cubicTo(
            cx - size * 0.45f, cy + size * 0.4f,
            cx - size * 0.15f, cy + size * 0.25f,
            cx, cy + size * 0.15f
        )
        // Right curve (mirror)
        cubicTo(
            cx + size * 0.15f, cy + size * 0.25f,
            cx + size * 0.45f, cy + size * 0.4f,
            cx + size * 0.6f, cy + size * 0.15f
        )
        cubicTo(
            cx + size * 0.8f, cy - size * 0.3f,
            cx + size * 0.15f, cy - size * 0.7f,
            cx, cy - size * 0.9f
        )
        close()
    }
    drawPath(path, color = color, style = Fill)

    // Stem
    val stemPath = Path().apply {
        moveTo(cx - size * 0.08f, cy + size * 0.1f)
        lineTo(cx - size * 0.15f, cy + size * 0.65f)
        cubicTo(
            cx - size * 0.15f, cy + size * 0.75f,
            cx + size * 0.15f, cy + size * 0.75f,
            cx + size * 0.15f, cy + size * 0.65f
        )
        lineTo(cx + size * 0.08f, cy + size * 0.1f)
        close()
    }
    drawPath(stemPath, color = color, style = Fill)
}

// ========== PIP RENDERING ==========

private fun DrawScope.drawPips(
    card: PlayingCard,
    suitColor: Color,
    w: Float,
    h: Float
) {
    val pipPositions = getPipPositions(card.rank)
    val pipSize = w * 0.16f

    val pipAreaLeft = w * 0.15f
    val pipAreaTop = h * 0.15f
    val pipAreaWidth = w * 0.70f
    val pipAreaHeight = h * 0.70f

    for (pip in pipPositions) {
        val px = pipAreaLeft + pip.x * pipAreaWidth
        val py = pipAreaTop + pip.y * pipAreaHeight

        if (pip.inverted) {
            rotate(180f, pivot = Offset(px, py)) {
                drawSuitSymbol(card.suit, px, py, pipSize, suitColor)
            }
        } else {
            drawSuitSymbol(card.suit, px, py, pipSize, suitColor)
        }
    }
}

private fun DrawScope.drawSuitSymbol(suit: Suit, cx: Float, cy: Float, size: Float, color: Color) {
    when (suit) {
        Suit.SPADES -> drawSpade(cx, cy, size, color)
        Suit.HEARTS -> drawHeart(cx, cy, size, color)
        Suit.CLUBS -> drawClub(cx, cy, size, color)
        Suit.DIAMONDS -> drawDiamond(cx, cy, size, color)
    }
}

private fun DrawScope.drawLargeSuit(suit: Suit, cx: Float, cy: Float, size: Float, color: Color) {
    drawSuitSymbol(suit, cx, cy, size, color)
}

private fun DrawScope.drawSpade(cx: Float, cy: Float, s: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy - s * 0.45f)
        cubicTo(cx - s * 0.1f, cy - s * 0.35f, cx - s * 0.5f, cy - s * 0.15f, cx - s * 0.35f, cy + s * 0.1f)
        cubicTo(cx - s * 0.25f, cy + s * 0.25f, cx - s * 0.08f, cy + s * 0.15f, cx, cy + s * 0.08f)
        cubicTo(cx + s * 0.08f, cy + s * 0.15f, cx + s * 0.25f, cy + s * 0.25f, cx + s * 0.35f, cy + s * 0.1f)
        cubicTo(cx + s * 0.5f, cy - s * 0.15f, cx + s * 0.1f, cy - s * 0.35f, cx, cy - s * 0.45f)
        close()
    }
    drawPath(path, color = color)
    // Stem
    val stem = Path().apply {
        moveTo(cx - s * 0.05f, cy + s * 0.05f)
        lineTo(cx - s * 0.1f, cy + s * 0.4f)
        cubicTo(cx - s * 0.1f, cy + s * 0.47f, cx + s * 0.1f, cy + s * 0.47f, cx + s * 0.1f, cy + s * 0.4f)
        lineTo(cx + s * 0.05f, cy + s * 0.05f)
        close()
    }
    drawPath(stem, color = color)
}

private fun DrawScope.drawHeart(cx: Float, cy: Float, s: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy + s * 0.4f)
        cubicTo(cx - s * 0.05f, cy + s * 0.3f, cx - s * 0.5f, cy + s * 0.05f, cx - s * 0.5f, cy - s * 0.12f)
        cubicTo(cx - s * 0.5f, cy - s * 0.35f, cx - s * 0.25f, cy - s * 0.45f, cx, cy - s * 0.22f)
        cubicTo(cx + s * 0.25f, cy - s * 0.45f, cx + s * 0.5f, cy - s * 0.35f, cx + s * 0.5f, cy - s * 0.12f)
        cubicTo(cx + s * 0.5f, cy + s * 0.05f, cx + s * 0.05f, cy + s * 0.3f, cx, cy + s * 0.4f)
        close()
    }
    drawPath(path, color = color)
}

private fun DrawScope.drawClub(cx: Float, cy: Float, s: Float, color: Color) {
    val lobeR = s * 0.2f
    // Top lobe
    drawCircle(color = color, radius = lobeR, center = Offset(cx, cy - s * 0.2f))
    // Left lobe
    drawCircle(color = color, radius = lobeR, center = Offset(cx - s * 0.22f, cy + s * 0.05f))
    // Right lobe
    drawCircle(color = color, radius = lobeR, center = Offset(cx + s * 0.22f, cy + s * 0.05f))
    // Connectors
    val connPath = Path().apply {
        moveTo(cx - s * 0.05f, cy - s * 0.05f)
        lineTo(cx + s * 0.05f, cy - s * 0.05f)
        lineTo(cx + s * 0.05f, cy + s * 0.1f)
        lineTo(cx - s * 0.05f, cy + s * 0.1f)
        close()
    }
    drawPath(connPath, color = color)
    // Stem
    val stem = Path().apply {
        moveTo(cx - s * 0.05f, cy + s * 0.05f)
        lineTo(cx - s * 0.1f, cy + s * 0.4f)
        cubicTo(cx - s * 0.1f, cy + s * 0.47f, cx + s * 0.1f, cy + s * 0.47f, cx + s * 0.1f, cy + s * 0.4f)
        lineTo(cx + s * 0.05f, cy + s * 0.05f)
        close()
    }
    drawPath(stem, color = color)
}

private fun DrawScope.drawDiamond(cx: Float, cy: Float, s: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy - s * 0.45f)
        cubicTo(cx + s * 0.08f, cy - s * 0.2f, cx + s * 0.35f, cy - s * 0.08f, cx + s * 0.3f, cy)
        cubicTo(cx + s * 0.35f, cy + s * 0.08f, cx + s * 0.08f, cy + s * 0.2f, cx, cy + s * 0.45f)
        cubicTo(cx - s * 0.08f, cy + s * 0.2f, cx - s * 0.35f, cy + s * 0.08f, cx - s * 0.3f, cy)
        cubicTo(cx - s * 0.35f, cy - s * 0.08f, cx - s * 0.08f, cy - s * 0.2f, cx, cy - s * 0.45f)
        close()
    }
    drawPath(path, color = color)
}

// ========== FACE CARD RENDERING (Bicycle-style court cards) ==========

private fun DrawScope.drawBicycleFaceCard(
    card: PlayingCard,
    suitColor: Color,
    w: Float,
    h: Float
) {
    val cx = w / 2f
    val cy = h / 2f

    // Face card area with decorative border
    val faceLeft = w * 0.17f
    val faceTop = h * 0.16f
    val faceRight = w * 0.83f
    val faceBottom = h * 0.84f
    val faceWidth = faceRight - faceLeft
    val faceHeight = faceBottom - faceTop
    val faceCorner = w * 0.025f

    // Decorative background
    drawRoundRect(
        color = FACE_CREAM,
        topLeft = Offset(faceLeft, faceTop),
        size = Size(faceWidth, faceHeight),
        cornerRadius = CornerRadius(faceCorner)
    )

    // Double border around face area
    drawRoundRect(
        color = suitColor.copy(alpha = 0.5f),
        topLeft = Offset(faceLeft, faceTop),
        size = Size(faceWidth, faceHeight),
        cornerRadius = CornerRadius(faceCorner),
        style = Stroke(width = w * 0.004f)
    )
    val inset2 = w * 0.012f
    drawRoundRect(
        color = suitColor.copy(alpha = 0.3f),
        topLeft = Offset(faceLeft + inset2, faceTop + inset2),
        size = Size(faceWidth - inset2 * 2, faceHeight - inset2 * 2),
        cornerRadius = CornerRadius(faceCorner * 0.7f),
        style = Stroke(width = w * 0.002f)
    )

    // Horizontal center dividing line (classic Bicycle style — mirrored halves)
    drawLine(
        color = suitColor.copy(alpha = 0.25f),
        start = Offset(faceLeft + w * 0.02f, cy),
        end = Offset(faceRight - w * 0.02f, cy),
        strokeWidth = w * 0.003f
    )

    // Draw the upper court figure half
    clipRect(
        left = faceLeft,
        top = faceTop,
        right = faceRight,
        bottom = cy
    ) {
        drawCourtFigure(card, suitColor, faceLeft, faceTop, faceWidth, faceHeight / 2f, w)
    }

    // Draw the lower court figure half (mirrored)
    clipRect(
        left = faceLeft,
        top = cy,
        right = faceRight,
        bottom = faceBottom
    ) {
        rotate(180f, pivot = Offset(cx, cy)) {
            drawCourtFigure(card, suitColor, faceLeft, faceTop, faceWidth, faceHeight / 2f, w)
        }
    }

    // Suit symbol in center of dividing line
    val centerSuitSize = w * 0.1f
    drawSuitSymbol(card.suit, cx, cy, centerSuitSize, suitColor)

    // Corner suit symbols inside the face card area
    val smallSuitSize = w * 0.065f
    drawSuitSymbol(card.suit, faceLeft + w * 0.06f, faceTop + h * 0.04f, smallSuitSize, suitColor)
    rotate(180f, pivot = Offset(cx, cy)) {
        drawSuitSymbol(card.suit, faceLeft + w * 0.06f, faceTop + h * 0.04f, smallSuitSize, suitColor)
    }
}

private fun DrawScope.drawCourtFigure(
    card: PlayingCard,
    suitColor: Color,
    areaLeft: Float,
    areaTop: Float,
    areaWidth: Float,
    areaHeight: Float,
    w: Float
) {
    val cx = areaLeft + areaWidth / 2f
    val figureTop = areaTop + areaHeight * 0.08f

    val robeColor = if (card.suit.isRed) FACE_BLUE else FACE_RED_ROBE
    val accentColor = FACE_GOLD

    // === Crown / hat area ===
    val crownCy = figureTop + areaHeight * 0.08f
    val crownSize = areaWidth * 0.22f

    when (card.rank) {
        Rank.KING -> {
            // Crown
            val crownPath = Path().apply {
                moveTo(cx - crownSize, crownCy + crownSize * 0.3f)
                lineTo(cx - crownSize, crownCy - crownSize * 0.15f)
                lineTo(cx - crownSize * 0.5f, crownCy + crownSize * 0.05f)
                lineTo(cx, crownCy - crownSize * 0.4f)
                lineTo(cx + crownSize * 0.5f, crownCy + crownSize * 0.05f)
                lineTo(cx + crownSize, crownCy - crownSize * 0.15f)
                lineTo(cx + crownSize, crownCy + crownSize * 0.3f)
                close()
            }
            drawPath(crownPath, color = accentColor)
            drawPath(crownPath, color = suitColor.copy(alpha = 0.4f), style = Stroke(width = w * 0.003f))

            // Crown jewels
            drawCircle(color = suitColor, radius = w * 0.008f, center = Offset(cx, crownCy - crownSize * 0.25f))
            drawCircle(color = BICYCLE_RED, radius = w * 0.006f, center = Offset(cx - crownSize * 0.5f, crownCy + crownSize * 0.02f))
            drawCircle(color = BICYCLE_RED, radius = w * 0.006f, center = Offset(cx + crownSize * 0.5f, crownCy + crownSize * 0.02f))
        }
        Rank.QUEEN -> {
            // Tiara-style crown
            val tiaraPath = Path().apply {
                moveTo(cx - crownSize * 0.8f, crownCy + crownSize * 0.2f)
                cubicTo(
                    cx - crownSize * 0.8f, crownCy - crownSize * 0.2f,
                    cx - crownSize * 0.3f, crownCy - crownSize * 0.5f,
                    cx, crownCy - crownSize * 0.35f
                )
                cubicTo(
                    cx + crownSize * 0.3f, crownCy - crownSize * 0.5f,
                    cx + crownSize * 0.8f, crownCy - crownSize * 0.2f,
                    cx + crownSize * 0.8f, crownCy + crownSize * 0.2f
                )
                close()
            }
            drawPath(tiaraPath, color = accentColor)
            drawPath(tiaraPath, color = suitColor.copy(alpha = 0.3f), style = Stroke(width = w * 0.003f))

            // Jewel at center
            drawCircle(color = suitColor, radius = w * 0.009f, center = Offset(cx, crownCy - crownSize * 0.15f))
        }
        Rank.JACK -> {
            // Beret / cap
            val capPath = Path().apply {
                moveTo(cx - crownSize * 0.9f, crownCy + crownSize * 0.15f)
                cubicTo(
                    cx - crownSize * 0.9f, crownCy - crownSize * 0.3f,
                    cx + crownSize * 0.3f, crownCy - crownSize * 0.5f,
                    cx + crownSize * 0.9f, crownCy - crownSize * 0.1f
                )
                lineTo(cx + crownSize * 0.9f, crownCy + crownSize * 0.15f)
                close()
            }
            drawPath(capPath, color = robeColor)
            drawPath(capPath, color = suitColor.copy(alpha = 0.3f), style = Stroke(width = w * 0.003f))

            // Feather
            val featherPath = Path().apply {
                moveTo(cx + crownSize * 0.7f, crownCy - crownSize * 0.1f)
                cubicTo(
                    cx + crownSize * 1.1f, crownCy - crownSize * 0.6f,
                    cx + crownSize * 0.6f, crownCy - crownSize * 0.8f,
                    cx + crownSize * 0.3f, crownCy - crownSize * 0.4f
                )
            }
            drawPath(featherPath, color = accentColor, style = Stroke(width = w * 0.005f))
        }
        else -> {}
    }

    // === Head (face) ===
    val headCy = crownCy + crownSize * 0.55f
    val headRadius = areaWidth * 0.1f
    drawCircle(color = FACE_CREAM, radius = headRadius, center = Offset(cx, headCy))
    drawCircle(
        color = suitColor.copy(alpha = 0.3f),
        radius = headRadius,
        center = Offset(cx, headCy),
        style = Stroke(width = w * 0.003f)
    )

    // Eyes
    val eyeY = headCy - headRadius * 0.15f
    val eyeSpacing = headRadius * 0.4f
    drawCircle(color = suitColor.copy(alpha = 0.7f), radius = w * 0.005f, center = Offset(cx - eyeSpacing, eyeY))
    drawCircle(color = suitColor.copy(alpha = 0.7f), radius = w * 0.005f, center = Offset(cx + eyeSpacing, eyeY))

    // Mouth
    val mouthPath = Path().apply {
        moveTo(cx - headRadius * 0.25f, headCy + headRadius * 0.3f)
        cubicTo(
            cx - headRadius * 0.1f, headCy + headRadius * 0.45f,
            cx + headRadius * 0.1f, headCy + headRadius * 0.45f,
            cx + headRadius * 0.25f, headCy + headRadius * 0.3f
        )
    }
    drawPath(mouthPath, color = suitColor.copy(alpha = 0.4f), style = Stroke(width = w * 0.003f))

    // === Body / robes ===
    val bodyTop = headCy + headRadius * 0.8f
    val bodyBottom = areaTop + areaHeight
    val bodyWidth = areaWidth * 0.55f

    // Main robe
    val robePath = Path().apply {
        moveTo(cx - bodyWidth * 0.35f, bodyTop)
        lineTo(cx - bodyWidth * 0.5f, bodyBottom)
        lineTo(cx + bodyWidth * 0.5f, bodyBottom)
        lineTo(cx + bodyWidth * 0.35f, bodyTop)
        close()
    }
    drawPath(robePath, color = robeColor)
    drawPath(robePath, color = suitColor.copy(alpha = 0.25f), style = Stroke(width = w * 0.003f))

    // Collar / neckline
    val collarPath = Path().apply {
        moveTo(cx - bodyWidth * 0.3f, bodyTop)
        cubicTo(
            cx - bodyWidth * 0.15f, bodyTop + areaHeight * 0.05f,
            cx + bodyWidth * 0.15f, bodyTop + areaHeight * 0.05f,
            cx + bodyWidth * 0.3f, bodyTop
        )
    }
    drawPath(collarPath, color = accentColor, style = Stroke(width = w * 0.006f))

    // Center robe decoration — vertical stripe
    drawLine(
        color = accentColor.copy(alpha = 0.6f),
        start = Offset(cx, bodyTop + areaHeight * 0.03f),
        end = Offset(cx, bodyBottom),
        strokeWidth = w * 0.008f
    )

    // Gold buttons
    val buttonCount = 3
    val buttonSpacing = (bodyBottom - bodyTop - areaHeight * 0.06f) / (buttonCount + 1)
    for (i in 1..buttonCount) {
        val by = bodyTop + areaHeight * 0.03f + buttonSpacing * i
        drawCircle(color = accentColor, radius = w * 0.007f, center = Offset(cx, by))
        drawCircle(
            color = suitColor.copy(alpha = 0.3f),
            radius = w * 0.007f,
            center = Offset(cx, by),
            style = Stroke(width = w * 0.002f)
        )
    }

    // === Held item (varies by rank) ===
    val handX = if (card.rank == Rank.JACK) cx + bodyWidth * 0.3f else cx - bodyWidth * 0.35f
    val handY = bodyTop + (bodyBottom - bodyTop) * 0.3f

    when (card.rank) {
        Rank.KING -> {
            // Sword
            drawLine(
                color = BORDER_COLOR,
                start = Offset(handX, bodyTop - areaHeight * 0.02f),
                end = Offset(handX, bodyBottom),
                strokeWidth = w * 0.007f
            )
            // Sword guard
            drawLine(
                color = accentColor,
                start = Offset(handX - w * 0.03f, handY),
                end = Offset(handX + w * 0.03f, handY),
                strokeWidth = w * 0.008f
            )
            // Pommel
            drawCircle(color = accentColor, radius = w * 0.009f, center = Offset(handX, handY + w * 0.02f))
        }
        Rank.QUEEN -> {
            // Scepter with flower
            drawLine(
                color = accentColor,
                start = Offset(handX, bodyTop),
                end = Offset(handX, bodyBottom),
                strokeWidth = w * 0.006f
            )
            // Flower at top
            val flowerSize = w * 0.025f
            for (i in 0 until 6) {
                val angle = Math.toRadians(i * 60.0)
                val fx = handX + flowerSize * cos(angle).toFloat()
                val fy = (bodyTop - areaHeight * 0.01f) + flowerSize * sin(angle).toFloat()
                drawCircle(color = suitColor.copy(alpha = 0.5f), radius = flowerSize * 0.5f, center = Offset(fx, fy))
            }
            drawCircle(color = accentColor, radius = flowerSize * 0.4f, center = Offset(handX, bodyTop - areaHeight * 0.01f))
        }
        Rank.JACK -> {
            // Leaf / branch
            val leafPath = Path().apply {
                moveTo(handX, bodyTop)
                cubicTo(
                    handX + w * 0.04f, bodyTop + areaHeight * 0.1f,
                    handX + w * 0.02f, bodyTop + areaHeight * 0.25f,
                    handX, bodyBottom
                )
            }
            drawPath(leafPath, color = Color(0xFF4A7C3F), style = Stroke(width = w * 0.005f))
            // Small leaves
            drawCircle(color = Color(0xFF4A7C3F).copy(alpha = 0.5f), radius = w * 0.012f, center = Offset(handX + w * 0.02f, bodyTop + areaHeight * 0.1f))
            drawCircle(color = Color(0xFF4A7C3F).copy(alpha = 0.5f), radius = w * 0.01f, center = Offset(handX + w * 0.015f, bodyTop + areaHeight * 0.2f))
        }
        else -> {}
    }

    // === Decorative corner flourishes in face area ===
    val flourishSize = w * 0.04f
    val fLeft = areaLeft + w * 0.04f
    val fTop = areaTop + areaHeight * 0.06f
    drawFaceCornerFlourish(fLeft, fTop, flourishSize, suitColor.copy(alpha = 0.2f), w)
    drawFaceCornerFlourish(areaLeft + areaWidth - w * 0.04f, fTop, flourishSize, suitColor.copy(alpha = 0.2f), w)
}

private fun DrawScope.drawFaceCornerFlourish(x: Float, y: Float, size: Float, color: Color, w: Float) {
    // Small decorative swirl
    val path = Path().apply {
        moveTo(x, y)
        cubicTo(x + size * 0.5f, y, x + size, y + size * 0.5f, x + size * 0.5f, y + size)
    }
    drawPath(path, color = color, style = Stroke(width = w * 0.003f))
}
