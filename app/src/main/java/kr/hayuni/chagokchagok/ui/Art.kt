package kr.hayuni.chagokchagok.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kr.hayuni.chagokchagok.data.SceneKind
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * 실제 사진이 없는 1차 시안용 "장면 이미지".
 * 하늘 그라데이션 + 능선 + 물/들판 + 날씨 효과를 Canvas 로 그려서 사진처럼 쓴다.
 * (나중에 팀이 만든 일러스트/사진으로 교체할 수 있게 같은 자리에서 쓰도록 분리)
 */
private class Spec(
    val skyTop: Color,
    val skyBottom: Color,
    val horizon: Float,
    val far: Color,
    val near: Color,
    val waterTop: Color? = null,
    val waterBottom: Color? = null,
    val ground: Color? = null,
    val sun: Color? = null,
    val sunX: Float = 0.5f,
    val sunY: Float = 0.5f,
)

private fun specOf(k: SceneKind): Spec = when (k) {
    SceneKind.LakeFog -> Spec(
        Color(0xFFB2C2D0), Color(0xFFEDE7DA), 0.56f,
        Color(0xFF9DB1A8), Color(0xFF5F7B6D),
        waterTop = Color(0xFFA6B8C3), waterBottom = Color(0xFF7592A3),
    )
    SceneKind.Cable -> Spec(
        Color(0xFF3F86DA), Color(0xFFC4E3F7), 0.62f,
        Color(0xFF82A892), Color(0xFF4F7C5E),
        waterTop = Color(0xFF4A92C0), waterBottom = Color(0xFF28608C),
    )
    SceneKind.Highway -> Spec(
        Color(0xFF2E78D4), Color(0xFFAAD4F3), 0.64f,
        Color(0xFF7D95AD), Color(0xFF4A515B),
        ground = Color(0xFF59616B),
    )
    SceneKind.Sunset -> Spec(
        Color(0xFF3A2A5C), Color(0xFFF4A860), 0.62f,
        Color(0xFF4C3661), Color(0xFF1F1830),
        waterTop = Color(0xFF8C5A74), waterBottom = Color(0xFF2E2347),
        sun = Color(0xFFFFE4AE), sunX = 0.52f, sunY = 0.55f,
    )
    SceneKind.Rain -> Spec(
        Color(0xFF515C6A), Color(0xFF909BA7), 0.6f,
        Color(0xFF5C6C67), Color(0xFF3A4946),
        waterTop = Color(0xFF4C5A66), waterBottom = Color(0xFF2C3843),
    )
    SceneKind.Meadow -> Spec(
        Color(0xFF7AC2F2), Color(0xFFE8F5FA), 0.5f,
        Color(0xFF8FBF87), Color(0xFF5E9F58),
        ground = Color(0xFF86C26B),
    )
    SceneKind.Autumn -> Spec(
        Color(0xFFF1BB74), Color(0xFFFCE9C8), 0.6f,
        Color(0xFFCC8B42), Color(0xFF9B4E2B),
        ground = Color(0xFFB9722F),
    )
}

private fun DrawScope.ridge(baseY: Float, amp: Float, cycles: Float, phase: Float, color: Color) {
    val f = (2 * PI * cycles / size.width).toFloat()
    val p = Path()
    p.moveTo(0f, size.height)
    var x = 0f
    while (x <= size.width + 8f) {
        val y = baseY + amp * sin(x * f + phase) + amp * 0.4f * sin(x * f * 2.3f + phase * 1.7f)
        p.lineTo(x, y)
        x += 8f
    }
    p.lineTo(size.width, size.height)
    p.close()
    drawPath(p, color)
}

private fun DrawScope.cloud(cx: Float, cy: Float, r: Float, alpha: Float) {
    val c = Color.White.copy(alpha = alpha)
    drawCircle(c, r, Offset(cx, cy))
    drawCircle(c, r * 0.8f, Offset(cx - r * 0.9f, cy + r * 0.2f))
    drawCircle(c, r * 0.9f, Offset(cx + r, cy + r * 0.15f))
    drawCircle(c, r * 0.6f, Offset(cx + r * 1.8f, cy + r * 0.3f))
}

@Composable
fun SceneArt(kind: SceneKind, modifier: Modifier = Modifier, seed: Int = 0) {
    Canvas(modifier.clipToBounds()) {
        val w = size.width
        val h = size.height
        val s = specOf(kind)
        val rnd = Random(kind.ordinal * 101 + seed * 17)
        val hy = h * s.horizon
        val phase = rnd.nextFloat() * 6f

        // 하늘
        drawRect(Brush.verticalGradient(listOf(s.skyTop, s.skyBottom), 0f, hy))

        // 해
        s.sun?.let {
            val c = Offset(w * s.sunX, h * s.sunY)
            drawCircle(Brush.radialGradient(listOf(it, it.copy(alpha = 0f)), c, w * 0.45f), w * 0.45f, c)
            drawCircle(it, w * 0.09f, c)
        }

        // 구름
        if (kind == SceneKind.Cable || kind == SceneKind.Meadow || kind == SceneKind.Highway || kind == SceneKind.Rain) {
            val a = if (kind == SceneKind.Rain) 0.55f else 0.85f
            repeat(3) { i ->
                cloud(w * (0.15f + 0.3f * i + rnd.nextFloat() * 0.1f), hy * (0.25f + 0.2f * rnd.nextFloat()), w * 0.07f, a)
            }
        }

        if (kind == SceneKind.Highway) {
            // 도시 스카이라인
            var x = 0f
            while (x < w) {
                val bw = w * (0.04f + rnd.nextFloat() * 0.05f)
                val bh = h * (0.08f + rnd.nextFloat() * 0.3f)
                drawRect(s.far.copy(alpha = 0.55f + rnd.nextFloat() * 0.35f), Offset(x, hy - bh), Size(bw, bh))
                x += bw + w * 0.004f
            }
            // 도로
            drawRect(s.ground!!, Offset(0f, hy), Size(w, h - hy))
            val road = Path().apply {
                moveTo(w * 0.47f, hy); lineTo(w * 0.53f, hy); lineTo(w * 0.95f, h); lineTo(w * 0.05f, h); close()
            }
            drawPath(road, s.near)
            for (i in 0..4) {
                val u = (i + 0.5f) / 5f
                val y1 = hy + (h - hy) * u * u
                val y2 = hy + (h - hy) * (u + 0.08f) * (u + 0.08f)
                drawLine(Color.White.copy(alpha = 0.9f), Offset(w * 0.5f, y1), Offset(w * 0.5f, y2), strokeWidth = 2f + 14f * u * u)
            }
            return@Canvas
        }

        // 먼 능선
        ridge(hy - h * 0.03f, h * 0.035f, 2.2f, phase, s.far)

        // 물 / 들판
        if (s.waterTop != null && s.waterBottom != null) {
            drawRect(Brush.verticalGradient(listOf(s.waterTop, s.waterBottom), hy, h), Offset(0f, hy), Size(w, h - hy))
            val streak = (s.sun ?: Color.White).copy(alpha = 0.28f)
            repeat(14) {
                val y = hy + (h - hy) * (0.06f + 0.9f * rnd.nextFloat())
                val len = w * (0.08f + 0.2f * rnd.nextFloat())
                val x = w * (0.1f + 0.8f * rnd.nextFloat())
                drawLine(streak, Offset(x, y), Offset(x + len, y), strokeWidth = 3f, cap = StrokeCap.Round)
            }
        } else if (s.ground != null) {
            drawRect(
                Brush.verticalGradient(listOf(s.ground, s.near), hy, h),
                Offset(0f, hy), Size(w, h - hy),
            )
        }

        // 가까운 능선
        ridge(h * 0.9f, h * 0.045f, 1.4f, phase + 2f, s.near)

        when (kind) {
            SceneKind.LakeFog -> {
                for (i in 0..3) {
                    val y = hy - h * 0.06f + i * h * 0.07f
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0f)),
                            y - h * 0.08f, y + h * 0.08f,
                        ),
                        Offset(0f, y - h * 0.08f), Size(w, h * 0.16f),
                    )
                }
            }
            SceneKind.Cable -> {
                drawLine(Color(0xFF2B3440), Offset(0f, h * 0.12f), Offset(w, h * 0.34f), strokeWidth = 3f)
                val cx = w * 0.62f
                val cy = h * 0.12f + (h * 0.22f) * 0.62f
                drawLine(Color(0xFF2B3440), Offset(cx, cy), Offset(cx, cy + h * 0.05f), strokeWidth = 3f)
                drawRect(Color(0xFFE5574B), Offset(cx - w * 0.04f, cy + h * 0.05f), Size(w * 0.08f, h * 0.065f))
                drawRect(Color.White.copy(alpha = 0.8f), Offset(cx - w * 0.03f, cy + h * 0.06f), Size(w * 0.06f, h * 0.03f))
            }
            SceneKind.Rain -> {
                repeat(90) {
                    val x = w * rnd.nextFloat()
                    val y = h * rnd.nextFloat()
                    drawLine(Color.White.copy(alpha = 0.32f), Offset(x, y), Offset(x - w * 0.015f, y + h * 0.05f), strokeWidth = 2f)
                }
            }
            SceneKind.Meadow -> {
                val cols = listOf(Color(0xFFF59FC0), Color(0xFFFFE066), Color.White, Color(0xFFD5A6F5))
                repeat(70) {
                    val u = rnd.nextFloat()
                    val y = hy + (h - hy) * (0.12f + 0.85f * u)
                    drawCircle(cols[rnd.nextInt(cols.size)], 3f + 8f * u, Offset(w * rnd.nextFloat(), y))
                }
            }
            SceneKind.Autumn -> {
                val cols = listOf(Color(0xFFE2762F), Color(0xFFC8402A), Color(0xFFF0B432), Color(0xFFD9892F))
                repeat(26) {
                    val x = w * rnd.nextFloat()
                    val y = hy + (h - hy) * (0.05f + 0.6f * rnd.nextFloat())
                    val r = w * (0.03f + 0.04f * rnd.nextFloat())
                    drawRect(Color(0xFF5A3720), Offset(x - r * 0.1f, y), Size(r * 0.2f, r * 1.2f))
                    drawCircle(cols[rnd.nextInt(cols.size)].copy(alpha = 0.95f), r, Offset(x, y))
                    drawCircle(cols[rnd.nextInt(cols.size)].copy(alpha = 0.9f), r * 0.7f, Offset(x + r * 0.5f, y + r * 0.2f))
                }
            }
            else -> {}
        }
    }
}

/** 카메라 화면 뒷배경: 차 앞유리 너머로 길이 흘러가는 모습 (시안용 가짜 뷰파인더) */
@Composable
fun RoadViewfinder(modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "road").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "t",
    )
    Canvas(modifier.clipToBounds()) {
        val w = size.width
        val h = size.height
        val hy = h * 0.52f
        val cx = w * 0.5f
        val rnd = Random(7)

        drawRect(Brush.verticalGradient(listOf(Color(0xFF1B69CF), Color(0xFF8FC8F2)), 0f, hy))
        cloud(w * 0.3f, hy * 0.62f, w * 0.1f, 0.9f)
        cloud(w * 0.62f, hy * 0.78f, w * 0.12f, 0.85f)
        cloud(w * 0.82f, hy * 0.45f, w * 0.07f, 0.8f)

        // 스카이라인
        var x = 0f
        while (x < w) {
            val bw = w * (0.025f + rnd.nextFloat() * 0.035f)
            val bh = h * (0.05f + rnd.nextFloat() * 0.2f)
            drawRect(Color(0xFF6E8CA8).copy(alpha = 0.5f + rnd.nextFloat() * 0.4f), Offset(x, hy - bh), Size(bw, bh))
            x += bw + w * 0.003f
        }
        // 멀리 보이는 타워
        val tower = Path().apply {
            moveTo(w * 0.31f, h * 0.04f); lineTo(w * 0.325f, hy); lineTo(w * 0.295f, hy); close()
        }
        drawPath(tower, Color(0xFF53687F))

        // 땅 + 도로
        drawRect(Brush.verticalGradient(listOf(Color(0xFF7C8189), Color(0xFF3F444B)), hy, h), Offset(0f, hy), Size(w, h - hy))
        val road = Path().apply {
            moveTo(cx - w * 0.015f, hy); lineTo(cx + w * 0.015f, hy)
            lineTo(cx + w * 0.46f, h); lineTo(cx - w * 0.46f, h); close()
        }
        drawPath(road, Color(0xFF353A41))
        // 차선 가장자리
        drawLine(Color.White.copy(alpha = 0.85f), Offset(cx - w * 0.012f, hy), Offset(cx - w * 0.43f, h), strokeWidth = 5f)
        drawLine(Color.White.copy(alpha = 0.85f), Offset(cx + w * 0.012f, hy), Offset(cx + w * 0.43f, h), strokeWidth = 5f)

        // 흘러가는 중앙선
        val n = 9
        for (i in 0 until n) {
            val u = ((i + t) / n) % 1f
            val u2 = (u + 0.045f).coerceAtMost(1f)
            val y1 = hy + (h - hy) * u * u
            val y2 = hy + (h - hy) * u2 * u2
            drawLine(Color.White.copy(alpha = 0.95f), Offset(cx, y1), Offset(cx, y2), strokeWidth = 2f + 20f * u * u)
        }

        // 길가 나무가 지나감
        val trees = 7
        for (side in listOf(-1f, 1f)) {
            for (i in 0 until trees) {
                val u = (((i + t + (if (side > 0) 0.5f else 0f)) / trees) % 1f)
                val ty = hy + (h - hy) * u * u * 0.92f
                val tx = cx + side * (w * 0.05f + w * 0.5f * u * u)
                val r = 6f + w * 0.07f * u * u
                drawRect(Color(0xFF4B3A2A), Offset(tx - r * 0.12f, ty - r * 0.3f), Size(r * 0.24f, r * 1.1f))
                drawCircle(Color(0xFF2F6B35), r, Offset(tx, ty - r))
                drawCircle(Color(0xFF3F8A45), r * 0.7f, Offset(tx - r * 0.35f, ty - r * 1.15f))
            }
        }

        // 오른쪽 큰 건물
        drawRect(Color(0xFFCDBFAE), Offset(w * 0.9f, 0f), Size(w * 0.1f, h * 0.72f))
        for (gx in 0..5) for (gy in 0..16) {
            drawRect(
                Color(0xFF7E8EA0).copy(alpha = 0.55f),
                Offset(w * 0.905f + gx * w * 0.0155f, h * 0.02f + gy * h * 0.04f),
                Size(w * 0.01f, h * 0.026f),
            )
        }
    }
}
