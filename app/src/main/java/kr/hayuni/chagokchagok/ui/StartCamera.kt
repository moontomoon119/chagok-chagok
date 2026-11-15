package kr.hayuni.chagokchagok.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kr.hayuni.chagokchagok.data.VideoStore
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Shadow
import kr.hayuni.chagokchagok.data.Palette
import kr.hayuni.chagokchagok.data.SceneKind
import kr.hayuni.chagokchagok.data.Scenario
import kotlinx.coroutines.delay

private val softShadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 3f), 12f)

// ───────────────────────── 시작 화면 ─────────────────────────

@Composable
fun StartScreen(onStart: () -> Unit) {
    // 천천히 확대되는 배경 (켄 번즈 효과)
    val zoom by rememberInfiniteTransition(label = "zoom").animateFloat(
        1.04f, 1.14f, infiniteRepeatable(tween(14000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "z",
    )
    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        0.0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "g",
    )
    Box(Modifier.fillMaxSize().background(Palette.Bg)) {
        // 갤러리에 들어 있는 기록 이미지 하나를 배경으로
        SceneArt(
            SceneKind.LakeFog,
            Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = zoom; scaleY = zoom },
        )
        // 위/아래를 어둡게 눌러 흰 글씨가 읽히게
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.68f)),
                    ),
                ),
        )

        Row(Modifier.align(Alignment.TopStart).padding(start = 32.dp, top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource(kr.hayuni.chagokchagok.R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(72.dp).offset(x = (-12).dp),
            )
            Text(
                "차곡차곡", color = Color.White, fontSize = Type.Subtitle, fontWeight = FontWeight.Bold,
                style = TextStyle(shadow = softShadow), modifier = Modifier.offset(x = (-18).dp),
            )
        }

        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "달린 길마다,\n우리 가족 이야기가 차곡차곡.",
                color = Color.White,
                fontSize = Type.Display,
                lineHeight = 92.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = softShadow),
            )
            Spacer(Modifier.height(22.dp))
            Text(
                "문이 열린 곳, 창문을 내린 길까지 차가 알아서 기억해요.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = Type.Subtitle,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = softShadow),
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .width(440.dp)
                .height(72.dp)
                .graphicsLayer {
                    val s = 1f + 0.015f * glow
                    scaleX = s; scaleY = s
                }
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .clickable(onClick = onStart),
            contentAlignment = Alignment.Center,
        ) {
            Text("차곡차곡 쌓으러 가기", color = Color(0xFF16202B), fontSize = Type.Subtitle, fontWeight = FontWeight.Bold)
        }
    }
}

// ───────────────────────── 카메라 화면 ─────────────────────────

private val signals = listOf(
    Triple(Ico.Door, "문", "모두 닫힘"),
    Triple(Ico.Window, "창문", "닫힘"),
    Triple(Ico.Fog, "안개등", "꺼짐"),
    Triple(Ico.Wiper, "와이퍼", "꺼짐"),
    Triple(Ico.Pin, "위치", "가평 방면"),
)

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onToday: () -> Unit,
    onCaptured: () -> String,
    count: Int,
    lastScene: SceneKind?,
    lastSeed: Int,
) {
    var toast by remember { mutableStateOf<String?>(null) }
    var dur by remember { mutableIntStateOf(1) }
    var capturing by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }

    LaunchedEffect(capturing) {
        if (capturing) {
            val ms = when (dur) { 1 -> 1000; 2 -> 1500; else -> 2300 }
            progress.snapTo(0f)
            progress.animateTo(1f, tween(ms, easing = LinearEasing))
            flash.snapTo(0.9f)
            flash.animateTo(0f, tween(260))
            capturing = false
            progress.snapTo(0f)
            toast = onCaptured()
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2600)
            toast = null
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // 영상 보관함에 road.mp4 가 있으면 그 영상을, 없으면 그려 둔 도로를 배경으로
        val ctx = LocalContext.current
        val roadFile = remember { VideoStore.roadVideo(ctx) }
        if (roadFile != null) LoopingVideo(roadFile, Modifier.fillMaxSize()) else RoadViewfinder(Modifier.fillMaxSize())

        // 상단
        CircleButton(Ico.Back, Modifier.align(Alignment.TopStart).padding(36.dp), onBack)
        Row(
            Modifier.align(Alignment.TopEnd).padding(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (capturing) {
                val pulse by rememberInfiniteTransition(label = "rec").animateFloat(
                    0.3f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "p",
                )
                Box(Modifier.size(16.dp).clip(CircleShape).background(Palette.Danger.copy(alpha = pulse)))
                Spacer(Modifier.width(10.dp))
                Text("기록 중", color = Color.White, fontSize = Type.Body, fontWeight = FontWeight.Bold, style = TextStyle(shadow = softShadow))
            } else {
                Text(
                    "차 앞 카메라 · 자동 기록 켜짐", color = Color.White, fontSize = Type.Body,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF0E151D).copy(alpha = 0.55f))
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                )
            }
        }

        // 왼쪽: 차량 신호 목록 (참고 화면의 모드 목록 자리)
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 40.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF0E151D).copy(alpha = 0.55f))
                .padding(horizontal = 32.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("차가 지금 보고 있는 것", color = Color.White.copy(alpha = 0.75f), fontSize = Type.Caption)
            signals.forEach { (ic, k, v) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(ic, Palette.Mint, 38.dp)
                    Spacer(Modifier.width(16.dp))
                    Text(k, color = Color.White, fontSize = Type.Subtitle, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(14.dp))
                    Text(v, color = Color.White.copy(alpha = 0.7f), fontSize = Type.Body)
                }
            }
        }

        // 하단: 길이 선택 + 셔터
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "버튼을 누르면 오늘의 기록이 쌓여요",
                color = Color.White, fontSize = Type.Body, style = TextStyle(shadow = softShadow),
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(5, 1, 2).forEach { d ->
                    val on = d == dur
                    Text(
                        "${d}s",
                        color = if (on) Color.Black else Color.White,
                        fontSize = Type.Body,
                        fontWeight = FontWeight.Bold,
                        style = if (on) TextStyle.Default else TextStyle(shadow = softShadow),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (on) Color.White else Color.Transparent)
                            .clickable(enabled = !capturing) { dur = d }
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Shutter(capturing, progress.value) { if (!capturing) capturing = true }
        }

        // 하단 왼쪽: 오늘의 기록 썸네일 (누르면 오늘의 기록으로)
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 52.dp)
                .size(84.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(2.5.dp, Color.White, RoundedCornerShape(18.dp))
                .clickable(enabled = !capturing, onClick = onToday),
        ) {
            if (count > 0 && lastScene != null) {
                SceneArt(lastScene, Modifier.fillMaxSize(), seed = lastSeed)
                Text(
                    "$count", color = Color.White, fontSize = Type.Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Palette.Danger)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                    Text("오늘의\n기록", color = Color.White, fontSize = Type.Caption, textAlign = TextAlign.Center)
                }
            }
        }

        // 저장 안내 (화면은 그대로)
        toast?.let {
            Text(
                it, color = Palette.Mint, fontSize = Type.Subtitle, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 34.dp, vertical = 16.dp),
            )
        }

        // 플래시
        if (flash.value > 0f) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
        }
    }
}

@Composable
private fun CircleButton(icon: Int, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { AppIcon(icon, Color.White, 32.dp) }
}

@Composable
private fun Shutter(capturing: Boolean, progress: Float, onClick: () -> Unit) {
    Box(
        Modifier
            .size(136.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 10.dp.toPx()
            drawCircle(Color.White, (size.minDimension - sw) / 2f, style = Stroke(sw))
            if (capturing) {
                drawArc(
                    Palette.Danger, -90f, 360f * progress, false,
                    topLeft = Offset(sw / 2, sw / 2),
                    size = Size(size.width - sw, size.height - sw),
                    style = Stroke(sw, cap = StrokeCap.Round),
                )
            }
        }
        Box(
            Modifier
                .size(if (capturing) 62.dp else 100.dp)
                .clip(if (capturing) RoundedCornerShape(18.dp) else CircleShape)
                .background(if (capturing) Palette.Danger else Color.White),
        )
    }
}
