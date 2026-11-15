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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.input.pointer.pointerInput
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
            SubTabs(labels, cur, { sub = it })
            Spacer(Modifier.weight(1f))
            if (replaying) {
                val a by rememberInfiniteTransition(label = "dot").animateFloat(
                    0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "a",
                )
                Box(Modifier.size(16.dp).clip(CircleShape).background(Palette.Danger.copy(alpha = a)))
                Spacer(Modifier.width(10.dp))
                Text("기록 중", color = Palette.Text, fontSize = Type.Body, fontWeight = FontWeight.Bold)
            } else if (clips.isNotEmpty()) {
                if (cur == 0) {
                    Text("카드를 길게 누르면 삭제", color = Color.White.copy(alpha = 0.4f), fontSize = Type.Caption)
                    Spacer(Modifier.width(24.dp))
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onReplay)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(Ico.Play, Color.White.copy(alpha = 0.75f), 22.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("하루 자동 재생", color = Color.White.copy(alpha = 0.75f), fontSize = Type.Label, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (cur) {
                0 -> Filmstrip(clips, visible, replaying, onCamera, onRename, onDelete, onRestoreDemo)
                1 -> VoicePage(clips)
                else -> StoryPage()
            }
        }
    }
}

// ───────────────────────── 오늘 기록: 1초 클립 필름 ─────────────────────────

@OptIn(ExperimentalFoundationApi::class)
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
    var deleteId by remember { mutableStateOf<Int?>(null) }
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
                        .combinedClickable(enabled = !replaying, onClick = { openId = e.id }, onLongClick = { deleteId = e.id }),
                ) {
                    ClipArt(e, Modifier.fillMaxSize(), sound = false)
                    // 글자는 모두 흰색. 그림과 글자는 그라데이션으로만 나눈다 (위는 시각, 아래는 내용)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.38f),
                                    0.28f to Color.Transparent,
                                    0.5f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.86f),
                                ),
                            ),
                    )
                    Text(
                        e.time,
                        color = Color.White.copy(alpha = 0.9f), fontSize = Type.Label, fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.TopStart).padding(start = 30.dp, top = 28.dp),
                    )
                    Column(
                        Modifier.align(Alignment.BottomStart).padding(horizontal = 30.dp, vertical = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(e.tag, color = Color.White.copy(alpha = 0.7f), fontSize = Type.Caption)
                        Text(e.place, color = Color.White, fontSize = Type.Title, fontWeight = FontWeight.Bold)
                        Text(e.title, color = Color.White.copy(alpha = 0.92f), fontSize = Type.Body, fontWeight = FontWeight.Medium)
                        Text(e.detail, color = Color.White.copy(alpha = 0.65f), fontSize = Type.Caption, lineHeight = Type.line(27))
                    }
                }
            }
        }
        if (openId != null) {
            ClipViewer(all, openId!!, onClose = { openId = null }, onRename = onRename)
        }
        val del = all.firstOrNull { it.id == deleteId }
        if (del != null) {
            // 삭제는 보기 화면이 아니라 카드를 길게 눌렀을 때만 나온다
            Box(
                Modifier.fillMaxSize().background(Palette.Bg.copy(alpha = 0.92f)).clickable { deleteId = null },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.clip(RoundedCornerShape(32.dp)).background(Palette.Surface).clickable(enabled = false) {}.padding(horizontal = 64.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("이 순간을 삭제할까요?", color = Color.White, fontSize = Type.Subtitle, fontWeight = FontWeight.Bold)
                    Text("${del.time} · ${del.place} · 되돌릴 수 없어요", color = Color.White.copy(alpha = 0.6f), fontSize = Type.Label)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            Modifier.height(52.dp).clip(RoundedCornerShape(50)).background(Palette.Danger)
                                .clickable { onDelete(del.id); deleteId = null }.padding(horizontal = 40.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("삭제", color = Color.White, fontSize = Type.Body, fontWeight = FontWeight.Bold) }
                        BigButton("취소", { deleteId = null }, filled = false, height = 52.dp)
                    }
                }
            }
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
        file == null || !file.exists() -> PhotoArt(Photos.of(clip.scene), modifier)
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

/** 좌우로 밀어서 넘기기 */
private fun Modifier.swipeNav(key: Any, onPrev: () -> Unit, onNext: () -> Unit): Modifier = pointerInput(key) {
    var total = 0f
    detectHorizontalDragGestures(
        onDragStart = { total = 0f },
        onDragEnd = { if (total > 140f) onPrev() else if (total < -140f) onNext() },
    ) { _, dx -> total += dx }
}

@Composable
private fun NavArrow(icon: Int, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { AppIcon(icon, Color.White, 32.dp) }
}

/**
 * 카드를 누르면 열리는 보기. 갤러리처럼 좌우로 밀거나 화살표로 하나씩 넘겨 본다.
 * 화면을 누르면 처음부터 다시 보기, 오른쪽 위 ×(또는 바깥)로 닫기. 삭제는 카드를 길게 눌러서만 한다.
 */
@Composable
private fun ClipViewer(clips: List<TodayClip>, startId: Int, onClose: () -> Unit, onRename: (Int, String) -> Unit) {
    var index by remember { mutableIntStateOf(clips.indexOfFirst { it.id == startId }.coerceAtLeast(0)) }
    val i = index.coerceIn(0, clips.lastIndex)
    val clip = clips[i]
    var playKey by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    val prog = remember { Animatable(0f) }
    // 모든 오늘 기록은 5초
    LaunchedEffect(clip.id, playKey) {
        prog.snapTo(0f)
        prog.animateTo(1f, tween(5000, easing = LinearEasing))
    }
    LaunchedEffect(clip.id) { editing = false }
    fun go(d: Int) { val n = i + d; if (n in clips.indices) index = n }

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
            Box(
                Modifier
                    .width(880.dp)
                    .fillMaxHeight()
                    .clipToBounds()
                    .swipeNav(i, { go(-1) }, { go(1) })
                    .clickable { playKey++ },
            ) {
                if (clip.media != null) {
                    key(clip.id, playKey) { ClipArt(clip, Modifier.fillMaxSize(), sound = true) }
                } else {
                    // 실사 영상(3초, 천천히 확대)을 반복 재생. 영상이 없는 장면은 사진을 천천히 확대
                    val pic = Photos.of(clip.scene)
                    if (pic.vid != null) key(clip.id, playKey) { PhotoVideo(pic, Modifier.fillMaxSize()) }
                    else PhotoArt(pic, Modifier.fillMaxSize().graphicsLayer { val sc = 1f + 0.12f * prog.value; scaleX = sc; scaleY = sc })
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))))
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.25f)),
                ) {
                    // 진행 막대는 그리기 단계에서만 값을 읽어, 프레임마다 화면 전체를 다시 구성하지 않게 한다
                    Box(
                        Modifier.fillMaxSize()
                            .graphicsLayer { scaleX = prog.value; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }
                            .background(Color.White),
                    )
                }
                Text(
                    clip.time, color = Color.White.copy(alpha = 0.9f), fontSize = Type.Body, fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 36.dp, top = 60.dp),
                )
                Text(
                    "${i + 1} / ${clips.size}", color = Color.White.copy(alpha = 0.7f), fontSize = Type.Body,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 36.dp, top = 60.dp),
                )
                Column(Modifier.align(Alignment.BottomStart).padding(36.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(clip.place, color = Color.White, fontSize = Type.Headline, fontWeight = FontWeight.Bold)
                    Text(clip.title, color = Color.White.copy(alpha = 0.92f), fontSize = Type.Subtitle, fontWeight = FontWeight.Medium)
                }
                if (i > 0) NavArrow(Ico.Back, Modifier.align(Alignment.CenterStart).padding(start = 20.dp)) { go(-1) }
                if (i < clips.lastIndex) NavArrow(Ico.Next, Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)) { go(1) }
            }
            // 오른쪽: 정보와 편집
            Column(
                Modifier.width(560.dp).fillMaxHeight().padding(40.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(clip.tag, color = Color.White.copy(alpha = 0.6f), fontSize = Type.Label, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClose),
                            contentAlignment = Alignment.Center,
                        ) { AppIcon(Ico.Close, Color.White.copy(alpha = 0.8f), 28.dp) }
                    }
                    Text(clip.detail, color = Palette.TextDim, fontSize = Type.Body, lineHeight = Type.line(34))
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
                    // TODO(추후 개발): "편집"은 이 앱의 메인 기능으로 키울 자리다. 지금은 제목 수정만 된다.
                    //  - 5초 중 보여 줄 구간 고르기(자르기)
                    //  - 글귀·스티커·타임스탬프 얹기
                    //  - 배경 음악 / 아이 목소리 더하기
                    //  - 가족에게 공유, 갤러리(모아보기)로 보내기
                    // 그래서 이 화면의 버튼은 "편집" 하나만 크게 둔다 (삭제는 카드 길게 누르기, 닫기는 ×).
                    BigButton("편집", { editing = true }, Modifier.fillMaxWidth(), icon = Ico.Edit, height = 68.dp)
                }
            }
        }
    }
}

// ───────────────────────── 한마디 ─────────────────────────

/**
 * 한마디: 오늘의 모든 기록마다 "여기 어땠어요?"를 물을 수 있다.
 * 좌우로 밀거나 화살표로 기록을 고르고, 대답하고 싶은 기록에서만 대답한다 (건너뛰어도 된다).
 */
@Composable
private fun VoicePage(clips: List<TodayClip>) {
    if (clips.isEmpty()) return
    var index by remember { mutableIntStateOf(0) }
    val i = index.coerceIn(0, clips.lastIndex)
    val clip = clips[i]
    val answers = remember { mutableStateMapOf<Int, String>() }   // 기록 id -> 받아 적은 대답 (데모)
    var listening by remember { mutableStateOf(false) }
    var chars by remember { mutableIntStateOf(0) }
    LaunchedEffect(clip.id, listening) {
        if (listening) {
            chars = 0
            while (chars < Scenario.ANSWER.length) { delay(90); chars++ }
            delay(400)
            answers[clip.id] = Scenario.ANSWER
            listening = false
        }
    }
    LaunchedEffect(i) { listening = false }
    fun go(d: Int) { val n = i + d; if (n in clips.indices) index = n }
    val answer = answers[clip.id]

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Box(
            Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(32.dp))
                .swipeNav(i, { go(-1) }, { go(1) }),
        ) {
            ClipArt(clip, Modifier.fillMaxSize(), sound = false)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
            Column(
                Modifier.fillMaxSize().padding(horizontal = 96.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row {
                    Text("${clip.time} · ${clip.place}", color = Color.White.copy(alpha = 0.85f), fontSize = Type.Body, modifier = Modifier.weight(1f))
                    Text("${i + 1} / ${clips.size}", color = Color.White.copy(alpha = 0.6f), fontSize = Type.Body)
                }
                Column {
                    Text(Scenario.QUESTION, color = Color.White, fontSize = Type.Display, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(28.dp))
                    Wave(active = listening)
                }
                when {
                    listening -> Text("듣고 있어요…", color = Palette.Amber, fontSize = Type.Subtitle, fontWeight = FontWeight.SemiBold)
                    answer == null -> BigButton("대답하기", { listening = true }, icon = Ico.Mic, height = 52.dp)
                    else -> BigButton("다시 대답하기", { listening = true }, filled = false, icon = Ico.Mic, height = 48.dp)
                }
            }
            if (i > 0) NavArrow(Ico.Back, Modifier.align(Alignment.CenterStart).padding(start = 20.dp)) { go(-1) }
            if (i < clips.lastIndex) NavArrow(Ico.Next, Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)) { go(1) }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Panel(Modifier.fillMaxWidth().weight(1f), fill = Palette.SurfaceHi) {
                Column {
                    Chip("글로 받아 적기", Color.White, 18)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        when {
                            listening -> "“${Scenario.ANSWER.take(chars)}”"
                            answer != null -> "“$answer”"
                            else -> "대답하고 싶은 기록에서만 대답해요. 밀어서 다른 기록으로 넘어갈 수 있어요."
                        },
                        color = if (answer == null && !listening) Color.White.copy(alpha = 0.6f) else Palette.Text,
                        fontSize = Type.Title, lineHeight = Type.line(48),
                    )
                }
            }
            Panel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Chip(if (answer != null) "목소리로도 저장됨" else "목소리 저장 대기", Color.White, 18)
                    Text(
                        "늘 녹음하지 않아요. 차가 물었을 때만 녹음하고, 아이 목소리 원본은 보호자가 켜야만 보관해요.",
                        color = Palette.TextDim, fontSize = Type.Label, lineHeight = Type.line(30),
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
        PhotoArt(Photos.gallery(3), Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

        Column(Modifier.align(Alignment.TopStart).padding(40.dp)) {
            Text("15:40 · 집까지 1시간 35분 남았어요", color = Color.White.copy(alpha = 0.85f), fontSize = Type.Body)
            Spacer(Modifier.height(6.dp))
            Text("「${Scenario.STORY_TITLE}」", color = Color.White, fontSize = Type.Title, fontWeight = FontWeight.Bold)
        }

        Crossfade(targetState = cur, modifier = Modifier.align(Alignment.Center), label = "story") { idx ->
            Column(
                Modifier.widthIn(max = 1100.dp).padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (idx < 0) {
                    Text(
                        "오늘, 우리의 이야기\n들어볼까요?",
                        color = Color.White, fontSize = Type.Display, lineHeight = Type.line(90),
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
                        color = Color.White, fontSize = Type.Headline, lineHeight = Type.line(70),
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
