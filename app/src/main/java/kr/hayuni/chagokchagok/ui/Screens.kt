package kr.hayuni.chagokchagok.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kr.hayuni.chagokchagok.data.Palette
import kr.hayuni.chagokchagok.data.Scenario
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

// ───────────────────────── 공통 ─────────────────────────

@Composable
private fun Card(
    modifier: Modifier = Modifier,
    fill: Color = Palette.Surface,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(fill)
            .padding(22.dp),
    ) { content() }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

@Composable
private fun Title(text: String, sub: String) {
    Column {
        Text(text, color = Palette.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(sub, color = Palette.TextDim, fontSize = 15.sp)
    }
}

// ───────────────────────── 1. 오늘의 기록 ─────────────────────────

@Composable
fun HomeScreen(shown: Int) {
    Row(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(Modifier.weight(0.38f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Title("오늘의 기록", "시동을 걸면 시작돼요. 누를 것은 없어요.")
            Card(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val pulse = rememberInfiniteTransition(label = "rec")
                        val a by pulse.animateFloat(
                            0.25f, 1f,
                            infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a",
                        )
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Palette.Danger.copy(alpha = a)),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("기록 중", color = Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("가을 토요일 · 춘천 당일치기 (예시)", color = Palette.TextDim, fontSize = 14.sp)
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Stat("머문 곳", if (shown >= 2) "1" else "0", Palette.Sky)
                    Stat("창문 내린 구간", if (shown >= 3) "1" else "0", Palette.Amber)
                    Stat("날씨", if (shown >= 3) "안개" else "-", Palette.Mint)
                }
            }
            Card(Modifier.fillMaxWidth(), fill = Palette.SurfaceHi) {
                Text(
                    "문이 열린 곳은 ‘머문 시간’, 창문을 내린 길은 ‘바람을 맞은 순간’, 와이퍼와 안개등은 ‘그날 날씨’가 돼요.",
                    color = Palette.TextDim, fontSize = 14.sp, lineHeight = 21.sp,
                )
            }
        }

        Card(Modifier.weight(0.62f).fillMaxHeight()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                itemsIndexed(Scenario.events.take(shown)) { _, e ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 3 },
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(e.time, color = Palette.TextDim, fontSize = 16.sp, modifier = Modifier.width(62.dp))
                            Box(
                                Modifier
                                    .padding(top = 6.dp)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(e.color),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(e.place, color = Palette.Text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(10.dp))
                                    Chip(e.tag, e.color)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(e.title, color = e.color, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(e.detail, color = Palette.TextDim, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }
                if (shown == 0) {
                    item { Text("‘시나리오 재생’을 누르면 기록이 쌓이는 모습을 볼 수 있어요.", color = Palette.TextDim, fontSize = 16.sp) }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Palette.TextDim, fontSize = 13.sp)
    }
}

// ───────────────────────── 2. 한마디 ─────────────────────────

@Composable
fun VoiceScreen() {
    var phase by remember { mutableIntStateOf(0) } // 0 질문 / 1 듣는 중 / 2 저장됨
    var chars by remember { mutableIntStateOf(0) }

    LaunchedEffect(phase) {
        if (phase == 1) {
            chars = 0
            while (chars < Scenario.ANSWER.length) {
                delay(90)
                chars++
            }
            delay(500)
            phase = 2
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Title("한마디", "도착했거나 다시 출발할 때, 차가 한 번 물어봐요.")
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Card(Modifier.weight(1f).fillMaxHeight()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("12:45 · 춘천 케이블카", color = Palette.TextDim, fontSize = 14.sp)
                    Text("“${Scenario.QUESTION}”", color = Palette.Amber, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Waveform(active = phase == 1)
                    Spacer(Modifier.height(6.dp))
                    when (phase) {
                        0 -> Button(
                            onClick = { phase = 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = Palette.Amber, contentColor = Palette.Bg),
                        ) { Text("여섯 살 딸이 대답해요 (데모)", fontWeight = FontWeight.Bold) }
                        1 -> Text("듣고 있어요…", color = Palette.Mint, fontSize = 16.sp)
                        else -> OutlinedButton(onClick = { phase = 0 }) { Text("다시 보기") }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(Modifier.fillMaxWidth(), fill = Palette.SurfaceHi) {
                    Column {
                        Chip("글로 받아 적기", Palette.Sky)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (phase == 0) "대답을 기다리고 있어요." else "“${Scenario.ANSWER.take(chars)}”",
                            color = Palette.Text, fontSize = 22.sp, lineHeight = 32.sp,
                        )
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(if (phase == 2) "목소리로도 저장됨" else "목소리 저장 대기", Palette.Mint)
                        Text(
                            "늘 녹음하는 방식이 아니에요. 차가 물었을 때만 녹음하고, 아이 목소리 원본은 보호자가 켜야만 보관해요.",
                            color = Palette.TextDim, fontSize = 14.sp, lineHeight = 21.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Waveform(active: Boolean) {
    val t = rememberInfiniteTransition(label = "wave")
    val phase by t.animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "p",
    )
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(90.dp),
    ) {
        val n = 40
        val gap = size.width / n
        for (i in 0 until n) {
            val amp = if (active) 0.25f + 0.75f * abs(sin(phase + i * 0.55f)) else 0.08f
            val h = size.height * amp
            val x = gap * i + gap / 2
            drawLine(
                if (active) Palette.Amber else Palette.Line,
                Offset(x, (size.height - h) / 2),
                Offset(x, (size.height + h) / 2),
                strokeWidth = gap * 0.45f,
                cap = StrokeCap.Round,
            )
        }
    }
}

// ───────────────────────── 3. 귀갓길 낭독 ─────────────────────────

@Composable
fun StoryScreen() {
    var reading by remember { mutableStateOf(false) }
    var cur by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()

    LaunchedEffect(reading) {
        if (reading) {
            listState.scrollToItem(0)
            for (i in Scenario.story.indices) {
                cur = i
                listState.animateScrollToItem(i + 1)   // 0번 항목은 제목
                delay(2800)
            }
            reading = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Title("귀갓길 낭독", "목적지가 집으로 바뀌면, 차가 오늘 하루를 1인칭으로 읽어 줘요.")
        Card(Modifier.fillMaxWidth(), fill = Palette.SurfaceHi) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("15:20 · 집까지 1시간 35분 남았어요", color = Palette.TextDim, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("“오늘 이야기 들려드릴까요?”", color = Palette.Amber, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { cur = -1; reading = true },
                    enabled = !reading,
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Amber, contentColor = Palette.Bg),
                ) { Text(if (reading) "읽는 중…" else "들려주세요", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }
        Card(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { Text("「${Scenario.STORY_TITLE}」", color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                itemsIndexed(Scenario.story) { i, line ->
                    val on = i == cur
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (on) Palette.SurfaceHi else Color.Transparent)
                            .padding(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            Modifier
                                .padding(top = 4.dp)
                                .size(width = 4.dp, height = 40.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (on) Palette.Amber else Palette.Line),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                line.text,
                                color = if (on) Palette.Text else if (cur >= 0 && i < cur) Palette.TextDim else Palette.Text.copy(alpha = 0.85f),
                                fontSize = 19.sp, lineHeight = 27.sp,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                line.evidence.forEach { Chip("근거 · $it", if (line.daughterVoice && it == "딸 목소리") Palette.Lilac else Palette.Mint) }
                                if (on && line.daughterVoice) Chip("♪ 딸 목소리 재생", Palette.Amber)
                            }
                        }
                    }
                }
                item {
                    Text(
                        "이야기에는 기록된 것만 써요. 차량 신호, 장소, 날씨, 음악, 가족이 남긴 한마디가 전부이고 문장마다 근거가 붙어요.",
                        color = Palette.TextDim, fontSize = 13.sp, lineHeight = 19.sp,
                    )
                }
            }
        }
    }
}

// ───────────────────────── 4. 가족지도 ─────────────────────────

@Composable
fun MapScreen(parked: Boolean) {
    var filter by remember { mutableStateOf("전체") }

    fun visible(era: String, rainy: Boolean) = when (filter) {
        "전체" -> true
        "비 오던 날" -> rainy
        else -> era == filter
    }
    val routes = Scenario.routes.filter { visible(it.era, it.rainy) }
    val visibleNames = routes.map { it.name }.toSet()

    Row(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(Modifier.width(230.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Title("가족지도", "달린 길에 색이 입혀져요.")
            Spacer(Modifier.height(4.dp))
            Scenario.filters.forEach { f ->
                val on = f == filter
                Text(
                    f,
                    color = if (on) Palette.Bg else Palette.Text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) Palette.Amber else Palette.Surface)
                        .clickable { filter = f }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Card(Modifier.fillMaxWidth(), fill = Palette.SurfaceHi) {
                Text(
                    "자주 다닌 길은 굵어지고, 머문 곳에는 핀이 꽂혀요.",
                    color = Palette.TextDim, fontSize = 13.sp, lineHeight = 19.sp,
                )
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(Palette.Surface)
                .border(1.dp, Palette.Line, RoundedCornerShape(20.dp)),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(8.dp)) {
                val density = LocalDensity.current
                val wPx = with(density) { maxWidth.toPx() }
                val hPx = with(density) { maxHeight.toPx() }

                Canvas(Modifier.fillMaxSize()) {
                    // 은은한 격자
                    val step = 60.dp.toPx()
                    var x = 0f
                    while (x < size.width) { drawLine(Palette.Line.copy(alpha = 0.45f), Offset(x, 0f), Offset(x, size.height)); x += step }
                    var y = 0f
                    while (y < size.height) { drawLine(Palette.Line.copy(alpha = 0.45f), Offset(0f, y), Offset(size.width, y)); y += step }

                    routes.forEach { r ->
                        val path = Path()
                        r.points.forEachIndexed { i, p ->
                            val px = p.x * size.width
                            val py = p.y * size.height
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(
                            path, r.color.copy(alpha = 0.9f),
                            style = Stroke(r.width.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }

                Scenario.pins.filter { it.route in visibleNames }.forEach { pin ->
                    val px = (pin.pos.x * wPx).roundToInt()
                    val py = (pin.pos.y * hPx).roundToInt()
                    Row(
                        Modifier.offset { IntOffset(px - 11.dp.roundToPx(), py - 11.dp.roundToPx()) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Palette.Bg)
                                .border(4.dp, pin.color, CircleShape),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            pin.label,
                            color = Palette.Text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Palette.Bg.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            if (!parked) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Palette.Bg.copy(alpha = 0.93f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("달리는 중에는 음성으로만 동작해요", color = Palette.Amber, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("지도 탐색은 주차 브레이크를 걸었을 때만 열려요.", color = Palette.TextDim, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Chip("“작년 춘천 이야기 들려줘”  라고 말해 보세요", Palette.Mint)
                    }
                }
            }
        }
    }
}

// ───────────────────────── 5. 회상 · 기억 이사 ─────────────────────────

@Composable
fun RecallScreen() {
    var recalled by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(moving) {
        if (moving) {
            progress = 0f
            val steps = 60
            for (i in 1..steps) {
                delay(50)
                progress = i / steps.toFloat()
            }
        }
    }
    val done = progress >= 1f

    Row(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Title("회상", "같은 길에 다시 들어서면, 차가 먼저 말을 걸어요.")
            Card(Modifier.fillMaxWidth(), fill = Palette.SurfaceHi) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("3년 뒤 가을", Palette.Sky)
                        Chip("의암호 길", Palette.Amber)
                    }
                    Text(
                        "“3년 전 이 길에서 뒷창문 두 개가 동시에 내려갔어요. 그날 이야기 들려드릴까요?”",
                        color = Palette.Text, fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold,
                    )
                    if (!recalled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { recalled = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Palette.Amber, contentColor = Palette.Bg),
                            ) { Text("들려주세요", fontWeight = FontWeight.Bold) }
                            OutlinedButton(onClick = {}) { Text("다음에요") }
                        }
                    } else {
                        Text(
                            "「${Scenario.STORY_TITLE}」 — 안개등을 켠 의암호 길, 호수 바람이 들어오던 뒷창문. 그날 여섯 살 딸의 목소리가 이어서 나와요.",
                            color = Palette.Mint, fontSize = 16.sp, lineHeight = 24.sp,
                        )
                    }
                }
            }
            Text(
                "차만 아는 순간(같은 길에 다시 들어선 때)에 차가 먼저 꺼내 줘요. 앱을 열 필요가 없어요.",
                color = Palette.TextDim, fontSize = 14.sp, lineHeight = 21.sp,
            )
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Title("기억 이사", "기록은 차가 아니라 가족 계정에 쌓여요.")
            Card(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("7년 뒤, 차를 바꾸는 날", color = Palette.TextDim, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat("누적 거리", "%,d km".format((87412 * progress).roundToInt()), Palette.Amber)
                        Stat("이야기", "%d편".format((214 * progress).roundToInt()), Palette.Mint)
                        Stat("핀", "%,d개".format((1038 * progress).roundToInt()), Palette.Sky)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Palette.Amber,
                        trackColor = Palette.Line,
                    )
                    when {
                        !moving -> Button(
                            onClick = { moving = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Palette.Amber, contentColor = Palette.Bg),
                        ) { Text("새 현대차에 로그인 (데모)", fontWeight = FontWeight.Bold) }
                        !done -> Text("가족지도를 새 차로 옮기는 중…", color = Palette.Mint, fontSize = 16.sp)
                        else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Chip("이사 완료", Palette.Mint)
                            OutlinedButton(onClick = { moving = false; progress = 0f }) { Text("다시 보기") }
                        }
                    }
                }
            }
            Card(Modifier.fillMaxWidth(), fill = Palette.SurfaceHi) {
                Text(
                    "자녀가 첫 차를 살 때 물려줄 수도 있어요. 차를 팔 때는 차 안에 남은 데이터를 모두 지워요.",
                    color = Palette.TextDim, fontSize = 14.sp, lineHeight = 21.sp,
                )
            }
        }
    }
}
