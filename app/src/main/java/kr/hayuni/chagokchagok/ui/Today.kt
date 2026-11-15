package kr.hayuni.chagokchagok.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kr.hayuni.chagokchagok.data.Palette
import kr.hayuni.chagokchagok.data.SceneKind
import kr.hayuni.chagokchagok.data.Scenario
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** 탭 1: 오늘의 기록 = 오늘 기록 / 한마디 / 귀갓길 낭독 */
@Composable
fun TodayScreen(shown: Int, replaying: Boolean, onReplay: () -> Unit, onCamera: () -> Unit) {
    var sub by remember { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 44.dp, end = 44.dp, top = 8.dp, bottom = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubTabs(listOf("오늘 기록", "한마디", "귀갓길 낭독"), sub) { sub = it }
            Spacer(Modifier.weight(1f))
            if (replaying) {
                val a by rememberInfiniteTransition(label = "dot").animateFloat(
                    0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "a",
                )
                Box(Modifier.size(16.dp).clip(CircleShape).background(Palette.Danger.copy(alpha = a)))
                Spacer(Modifier.width(10.dp))
                Text("기록 중", color = Palette.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            } else {
                Chip("오늘의 순간 ${shown}개", Palette.Mint, 20)
            }
            Spacer(Modifier.width(20.dp))
            BigButton("하루 자동 재생", onReplay, enabled = !replaying, filled = false)
        }
        Spacer(Modifier.height(22.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (sub) {
                0 -> Filmstrip(shown, onCamera)
                1 -> VoicePage()
                else -> StoryPage()
            }
        }
    }
}

// ───────────────────────── 오늘 기록: 1초 클립 필름 ─────────────────────────

@Composable
private fun Filmstrip(shown: Int, onCamera: () -> Unit) {
    val state = rememberLazyListState()
    LaunchedEffect(shown) { if (shown > 0) state.animateScrollToItem((shown - 1).coerceAtLeast(0)) }

    if (shown == 0) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("아직 오늘의 기록이 없어요.", color = Palette.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Text("카메라에서 촬영할 때마다 오늘의 순간이 한 칸씩 쌓여요.", color = Palette.TextDim, fontSize = 26.sp)
            Spacer(Modifier.height(30.dp))
            BigButton("촬영하러 가기", onCamera)
        }
        return
    }
    LazyRow(
        state = state,
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(26.dp),
        contentPadding = PaddingValues(end = 320.dp),
    ) {
        itemsIndexed(Scenario.events.take(shown)) { i, e ->
            val a = remember { Animatable(0f) }
            LaunchedEffect(Unit) { a.animateTo(1f, tween(600)) }
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(420.dp)
                    .graphicsLayer {
                        alpha = a.value
                        val s = 0.9f + 0.1f * a.value
                        scaleX = s; scaleY = s
                    }
                    .clip(RoundedCornerShape(32.dp)),
            ) {
                SceneArt(Scenario.eventScenes[i], Modifier.fillMaxSize(), seed = i)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.45f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.78f),
                            ),
                        ),
                )
                Text(
                    e.time,
                    color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(22.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                )
                Text(
                    "1s",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(22.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Column(
                    Modifier.align(Alignment.BottomStart).padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Chip(e.tag, e.color, 18)
                    Text(e.place, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                    Text(e.title, color = e.color, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text(e.detail, color = Color.White.copy(alpha = 0.85f), fontSize = 19.sp, lineHeight = 27.sp)
                }
            }
        }
    }
}

// ───────────────────────── 한마디 ─────────────────────────

@Composable
private fun VoicePage() {
    var phase by remember { mutableIntStateOf(0) } // 0 질문 / 1 듣는 중 / 2 저장됨
    var chars by remember { mutableIntStateOf(0) }
    LaunchedEffect(phase) {
        if (phase == 1) {
            chars = 0
            while (chars < Scenario.ANSWER.length) { delay(90); chars++ }
            delay(500)
            phase = 2
        }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Box(
            Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(32.dp)),
        ) {
            SceneArt(SceneKind.Cable, Modifier.fillMaxSize(), seed = 3)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))
            Column(
                Modifier.fillMaxSize().padding(40.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("12:45 · 춘천 케이블카", color = Color.White.copy(alpha = 0.85f), fontSize = 24.sp)
                Column {
                    Text("“${Scenario.QUESTION}”", color = Color.White, fontSize = 76.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(28.dp))
                    Wave(active = phase == 1)
                }
                when (phase) {
                    0 -> BigButton("여섯 살 딸이 대답해요", { phase = 1 })
                    1 -> Text("듣고 있어요…", color = Palette.Mint, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    else -> BigButton("다시 보기", { phase = 0 }, filled = false)
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Panel(Modifier.fillMaxWidth().weight(1f), fill = Palette.SurfaceHi) {
                Column {
                    Chip("글로 받아 적기", Palette.Sky, 18)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        if (phase == 0) "대답을 기다리고 있어요." else "“${Scenario.ANSWER.take(chars)}”",
                        color = Palette.Text, fontSize = 34.sp, lineHeight = 48.sp,
                    )
                }
            }
            Panel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Chip(if (phase == 2) "목소리로도 저장됨" else "목소리 저장 대기", Palette.Mint, 18)
                    Text(
                        "늘 녹음하지 않아요. 차가 물었을 때만 녹음하고, 아이 목소리 원본은 보호자가 켜야만 보관해요.",
                        color = Palette.TextDim, fontSize = 20.sp, lineHeight = 30.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun Wave(active: Boolean) {
    val p by rememberInfiniteTransition(label = "wave").animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "p",
    )
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        val n = 44
        val gap = size.width / n
        for (i in 0 until n) {
            val amp = if (active) 0.25f + 0.75f * abs(sin(p + i * 0.55f)) else 0.07f
            val hh = size.height * amp
            val x = gap * i + gap / 2
            drawLine(
                if (active) Palette.Amber else Color.White.copy(alpha = 0.45f),
                Offset(x, (size.height - hh) / 2), Offset(x, (size.height + hh) / 2),
                strokeWidth = gap * 0.45f, cap = StrokeCap.Round,
            )
        }
    }
}

// ───────────────────────── 귀갓길 낭독 ─────────────────────────

@Composable
private fun StoryPage() {
    var reading by remember { mutableStateOf(false) }
    var cur by remember { mutableIntStateOf(-1) }
    LaunchedEffect(reading) {
        if (reading) {
            for (i in Scenario.story.indices) { cur = i; delay(3200) }
            reading = false
        }
    }

    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp))) {
        SceneArt(SceneKind.Sunset, Modifier.fillMaxSize(), seed = 1)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

        Column(Modifier.align(Alignment.TopStart).padding(40.dp)) {
            Text("15:20 · 집까지 1시간 35분 남았어요", color = Color.White.copy(alpha = 0.85f), fontSize = 24.sp)
            Spacer(Modifier.height(6.dp))
            Text("「${Scenario.STORY_TITLE}」", color = Palette.Amber, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }

        Crossfade(targetState = cur, modifier = Modifier.align(Alignment.Center), label = "story") { idx ->
            Column(
                Modifier.widthIn(max = 1100.dp).padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (idx < 0) {
                    Text(
                        "“오늘 이야기\n들려드릴까요?”",
                        color = Color.White, fontSize = 72.sp, lineHeight = 90.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    )
                } else {
                    val line = Scenario.story[idx]
                    if (idx > 0) {
                        Text(
                            Scenario.story[idx - 1].text,
                            color = Color.White.copy(alpha = 0.45f), fontSize = 26.sp, textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        line.text,
                        color = Color.White, fontSize = 50.sp, lineHeight = 70.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        line.evidence.forEach {
                            Chip("근거 · $it", if (line.daughterVoice && it == "딸 목소리") Palette.Lilac else Palette.Mint, 18)
                        }
                        if (line.daughterVoice) Chip("♪ 딸 목소리 재생", Palette.Amber, 18)
                    }
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BigButton(if (reading) "읽는 중…" else if (cur >= 0) "처음부터 다시 듣기" else "들려주세요", { cur = -1; reading = true }, enabled = !reading)
            Text(
                "이야기에는 기록된 것만 써요. 문장마다 근거가 붙어요.",
                color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp,
            )
        }
    }
}
