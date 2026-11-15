package kr.hayuni.chagokchagok.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.sin

/**
 * 카메라 화면 뒷배경: 차 앞유리 너머의 길 사진을 천천히 앞으로 달리는 것처럼 확대해 보여 준다.
 * (실제 카메라 영상이 없는 에뮬레이터/시안용. 카메라가 있으면 CameraScreen 이 영상으로 대신한다.)
 */
@Composable
fun CameraRoadBackdrop(modifier: Modifier = Modifier) {
    val zoom by rememberInfiniteTransition(label = "roadzoom").animateFloat(
        1.0f, 1.12f, infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "z",
    )
    PhotoArt(Photos.camera, modifier.graphicsLayer { scaleX = zoom; scaleY = zoom })
}

/** 소리만 저장된 기록(m4a)용 카드 배경: 파형 */
@Composable
fun AudioArt(modifier: Modifier = Modifier) {
    Canvas(modifier.clipToBounds()) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF243646), Color(0xFF17212C))))
        val n = 31
        val gap = w / (n + 1)
        val cy = h * 0.42f
        for (i in 0 until n) {
            val u = i / (n - 1f)
            val env = sin(PI * u).toFloat()
            val bump = 0.45f + 0.55f * (0.5f + 0.5f * sin(i * 1.9).toFloat())
            val bar = (h * 0.34f * env * bump).coerceAtLeast(h * 0.03f)
            val c = lerp(Color(0xFF6FCFB3), Color(0xFFF2B35E), u)
            drawLine(c, Offset(gap * (i + 1), cy - bar / 2f), Offset(gap * (i + 1), cy + bar / 2f), strokeWidth = gap * 0.42f, cap = StrokeCap.Round)
        }
    }
}
