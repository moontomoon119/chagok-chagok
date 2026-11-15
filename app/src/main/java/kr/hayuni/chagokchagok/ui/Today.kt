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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import kr.hayuni.chagokchagok.data.TodayClip
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** 탭 1: 오늘의 기록 = 오늘 기록 / 한마디 / 귀갓길 낭독 (기록이 하나도 없으면 "오늘 기록"만 보인다) */
@Composable
fun TodayScreen(
    clips: List<TodayClip>,
    limit: Int?,
    replaying: Boolean,
    onReplay: () -> Unit,
    onCamera: () -> Unit,
    onRename: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    onRestoreDemo: () -> Unit,
) {
    var sub by remember { mutableIntStateOf(0) }
    val labels = if (clips.isEmpty()) listOf("오늘 기록") else listOf("오늘 기록", "한마디", "귀갓길 낭독")
    val cur = sub.coerceIn(0, labels.lastIndex)
    val visible = if (limit != null) clips.take(limit) else clips
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 44.dp, end = 44.dp, top = 8.dp, bottom = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubTabs(labels, cur, { sub = it }, icons = listOf(Ico.Record, Ico.Mic, Ico.Book))
            Spacer(Modifier.weight(1f))
            if (replaying) {
                val a by rememberInfiniteTransition(label = "dot").animateFloat(
                    0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "a",
                )
                Box(Modifier.size(16.dp).clip(CircleShape).background(Palette.Danger.copy(alpha = a)))
                Spacer(Modifier.width(10.dp))
                Text("기록 중", color = Palette.Text, fontSize = Type.Body, fontWeight = FontWeight.Bold)
            } else if (cur == 0 && clips.isNotEmpty()) {
                Chip("오늘의 순간 ${clips.size}개", Palette.Mint, 18)
            }
            if (clips.isNotEmpty()) {
                Spacer(Modifier.width(20.dp))
                BigButton("하루 자동 재생", onReplay, enabled = !replaying, filled = false, icon = Ico.Play, height = 48.dp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (cur) {
                0 -> Filmstrip(clips, visible, replaying, onCamera, onRename, onDelete, onRestoreDemo)
                1 -> VoicePage()
                else -> StoryPage()
            }
        }
    }
}

// ───────────────────────── 오늘 기록: 1초 클립 필름 ─────────────────────────

@Composable
private fun Filmstrip(
    all: List<TodayClip>,
    visible: List<TodayClip>,
    replaying: Boolean,
    onCamera: () -> Unit,
    onRename: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    onRestoreDemo: () -> Unit,
) {
    val state = rememberLazyListState()
    var openId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(visible.size) { if (visible.isNotEmpty()) state.animateScrollToItem((visible.size - 1).coerceAtLeast(0)) }

    if (all.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("오늘의 기록이 비어 있어요.", color = Palette.Text, fontSize = Type.Title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Text("카메라에서 촬영할 때마다 오늘의 순간이 한 칸씩 쌓여요.", color = Palette.TextDim, fontSize = Type.Subtitle)
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BigButton("촬영하러 가기", onCamera)
                BigButton("예시 하루 불러오기", onRestoreDemo, filled = false)
            }
        }
        return
    }
    Box(Modifier.fillMaxSize()) {
        if (visible.isEmpty() && replaying) {
            Text("하루가 시작돼요…", color = Palette.TextDim, fontSize = Type.Subtitle, modifier = Modifier.align(Alignment.Center))
        }
        LazyRow(
            state = state,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            contentPadding = PaddingValues(end = 320.dp),
        ) {
            items(visible, key = { it.id }) { e ->
                val a = remember { Animatable(0f) }
                LaunchedEffect(Unit) { a.animateTo(1f, tween(600)) }
                val color = Color(e.color)
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(420.dp)
                        .graphicsLayer {
                            alpha = a.value
                            val sc = 0.9f + 0.1f * a.value
                            scaleX = sc; scaleY = sc
                        }
                        .clip(RoundedCornerShape(32.dp))
                        .clickable(enabled = !replaying) { openId = e.id },
                ) {
                    ClipArt(e, Modifier.fillMaxSize(), sound = false)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f))),
                    )
                    Text(
                        e.time,
                        color = Color.White, fontSize = Type.Body, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(22.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                    Text(
                        if (e.media == null) "1s" else "5s",
                        color = Color.White, fontSize = Type.Label, fontWeight = FontWeight.Bold,
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
                        Chip(e.tag, color, 18)
                        Text(e.place, color = Color.White, fontSize = Type.Title, fontWeight = FontWeight.Bold)
                        Text(e.title, color = color, fontSize = Type.Body, fontWeight = FontWeight.SemiBold)
                        Text(e.detail, color = Color.White.copy(alpha = 0.85f), fontSize = Type.Caption, lineHeight = 27.sp)
                    }
                }
            }
        }
        val open = all.firstOrNull { it.id == openId }
        if (open != null) {
            ClipViewer(open, onClose = { openId = null }, onRename = onRename, onDelete = onDelete)
        }
    }
}

/**
 * 카드 그림. 일러스트 기록은 장면, 홈 기록 버튼으로 찍은 기록은 실제 영상(mp4) 또는 소리 파형(m4a).
 * sound=true 면 보기 화면처럼 소리까지 재생한다.
 */
@Composable
private fun ClipArt(clip: TodayClip, modifier: Modifier, sound: Boolean) {
    val path = clip.media
    val file = remember(path) { path?.let { java.io.File(it) } }
    when {
        file == null || !file.exists() -> SceneArt(clip.scene, modifier, seed = clip.seed)
        file.extension.lowercase() == "m4a" -> {
            Box(modifier) {
                AudioArt(Modifier.fillMaxSize())
                if (sound) AudioPlayback(file)
            }
        }
        else -> {
            // 가로(16:9) 영상을 세로 카드에 꽉 채워 보여 준다 (가운데를 기준으로 자르기)
            BoxWithConstraints(modifier.clipToBounds()) {
                val s = (maxHeight.value / (maxWidth.value * 9f / 16f)).coerceAtLeast(1f)
                LoopingVideo(file, Modifier.fillMaxSize().graphicsLayer { scaleX = s; scaleY = s }, muted = !sound)
            }
        }
    }
}

/** 소리만 있는 기록: 한 번 재생 (파형 그림은 위에서 보여 줌) */
@Composable
private fun AudioPlayback(file: java.io.File) {
    DisposableEffect(file) {
        val mp = android.media.MediaPlayer()
        runCatching {
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            mp.start()
        }
        onDispose { runCatching { mp.stop() }; runCatching { mp.release() } }
    }
}

/** 카드를 누르면 열리는 보기: 클립 다시보기 + 제목 수정 + 삭제 */
@Composable
private fun ClipViewer(clip: TodayClip, onClose: () -> Unit, onRename: (Int, String) -> Unit, onDelete: (Int) -> Unit) {
    var playKey by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val prog = remember { Animatable(0f) }
    LaunchedEffect(playKey) {
        playing = true
        prog.snapTo(0f)
        prog.animateTo(1f, tween(if (clip.media != null) 5000 else 1000, easing = LinearEasing))
        playing = false
    }
    val color = Color(clip.color)

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg.copy(alpha = 0.95f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .fillMaxHeight(0.98f)
                .clip(RoundedCornerShape(32.dp))
                .background(Palette.Surface)
                .clickable(enabled = false) {},
        ) {
            // 1초 클립 재생: 장면이 천천히 다가오고 진행 막대가 찬다
            Box(Modifier.width(880.dp).fillMaxHeight().clipToBounds()) {
                if (clip.media != null) {
                    key(playKey) { ClipArt(clip, Modifier.fillMaxSize(), sound = true) }
                } else {
                    SceneArt(
                        clip.scene,
                        Modifier.fillMaxSize().graphicsLayer { val sc = 1f + 0.12f * prog.value; scaleX = sc; scaleY = sc },
                        seed = clip.seed,
                    )
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))))
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.25f)),
                ) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(prog.value).background(Color.White))
                }
                Text(
                    clip.time, color = Color.White, fontSize = Type.Subtitle, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 28.dp, top = 56.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
                Column(Modifier.align(Alignment.BottomStart).padding(36.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(clip.place, color = Color.White, fontSize = Type.Headline, fontWeight = FontWeight.Bold)
                    Text(clip.title, color = color, fontSize = Type.Subtitle, fontWeight = FontWeight.SemiBold)
                }
            }
            // 오른쪽: 정보와 도구
            Column(
                Modifier.width(560.dp).fillMaxHeight().padding(40.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Chip(clip.tag, color, 20)
                    Text(clip.detail, color = Palette.TextDim, fontSize = Type.Body, lineHeight = 34.sp)
                    if (editing) {
                        Spacer(Modifier.height(6.dp))
                        RenameBox(
                            initial = clip.place,
                            presets = listOf("우리 가족 웃음 터진 곳", "아이가 좋아한 곳", "다시 오고 싶은 길"),
                            onSave = { onRename(clip.id, it); editing = false },
                            onCancel = { editing = false },
                        )
                    }
                }
                if (!editing) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ViewerAction("다시보기", if (playing) "재생 중…" else if (clip.media != null) "▶ 5초" else "▶ 1초", Palette.Mint, enabled = !playing) { playKey++ }
                        ViewerAction("제목", "수정", Palette.Amber) { editing = true; confirmDelete = false }
                        if (!confirmDelete) {
                            ViewerAction("이 순간", "삭제", Palette.Danger) { confirmDelete = true }
                        } else {
                            Text("정말 삭제할까요? 되돌릴 수 없어요.", color = Palette.Danger, fontSize = Type.Body, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                BigButton("삭제", { onDelete(clip.id); onClose() })
                                BigButton("취소", { confirmDelete = false }, filled = false)
                            }
                        }
                        BigButton("닫기", onClose, Modifier.fillMaxWidth(), filled = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerAction(label: String, value: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.SurfaceHi)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Palette.Text, fontSize = Type.Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Chip(value, color, 20)
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
                Text("12:45 · 춘천 케이블카", color = Color.White.copy(alpha = 0.85f), fontSize = Type.Body)
                Column {
                    Text("“${Scenario.QUESTION}”", color = Color.White, fontSize = Type.Display, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(28.dp))
                    Wave(active = phase == 1)
                }
                when (phase) {
                    0 -> BigButton("여섯 살 딸이 대답해요", { phase = 1 }, icon = Ico.Mic, height = 52.dp)
                    1 -> Text("듣고 있어요…", color = Palette.Mint, fontSize = Type.Subtitle, fontWeight = FontWeight.SemiBold)
                    else -> BigButton("다시 보기", { phase = 0 }, filled = false, height = 48.dp)
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
                        color = Palette.Text, fontSize = Type.Title, lineHeight = 48.sp,
                    )
                }
            }
            Panel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Chip(if (phase == 2) "목소리로도 저장됨" else "목소리 저장 대기", Palette.Mint, 18)
                    Text(
                        "늘 녹음하지 않아요. 차가 물었을 때만 녹음하고, 아이 목소리 원본은 보호자가 켜야만 보관해요.",
                        color = Palette.TextDim, fontSize = Type.Label, lineHeight = 30.sp,
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
            Text("15:20 · 집까지 1시간 35분 남았어요", color = Color.White.copy(alpha = 0.85f), fontSize = Type.Body)
            Spacer(Modifier.height(6.dp))
            Text("「${Scenario.STORY_TITLE}」", color = Palette.Amber, fontSize = Type.Title, fontWeight = FontWeight.Bold)
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
                        color = Color.White, fontSize = Type.Display, lineHeight = 90.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    )
                } else {
                    val line = Scenario.story[idx]
                    if (idx > 0) {
                        Text(
                            Scenario.story[idx - 1].text,
                            color = Color.White.copy(alpha = 0.45f), fontSize = Type.Subtitle, textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        line.text,
                        color = Color.White, fontSize = Type.Headline, lineHeight = 70.sp,
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
            if (cur >= 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Scenario.story.indices.forEach { i ->
                        Box(
                            Modifier
                                .size(if (i == cur) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (i <= cur) Palette.Amber else Color.White.copy(alpha = 0.3f)),
                        )
                    }
                }
            }
            BigButton(
                if (reading) "읽는 중…" else if (cur >= 0) "처음부터 다시 듣기" else "들려주세요",
                { cur = -1; reading = true }, enabled = !reading, icon = Ico.Play, height = 52.dp,
            )
            Text(
                "이야기에는 기록된 것만 써요. 문장마다 근거가 붙어요.",
                color = Color.White.copy(alpha = 0.7f), fontSize = Type.Caption,
            )
        }
    }
}
