package kr.hayuni.chagokchagok.ui

import android.media.MediaMetadataRetriever
import android.widget.VideoView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.hayuni.chagokchagok.data.Moment
import kr.hayuni.chagokchagok.data.Palette
import kr.hayuni.chagokchagok.data.SceneKind
import kr.hayuni.chagokchagok.data.Scenario
import kr.hayuni.chagokchagok.data.VideoStore
import java.io.File

private val textShadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 3f), 14f)

private fun Moment.timeText() = "%02d:%02d".format(9 + seed % 9, (seed * 17) % 60)
private fun Moment.stampText(): String? = when (stamp) {
    1 -> date
    2 -> "$date  ${timeText()}"
    else -> null
}

private val captionPresets = listOf(
    null, // 원래 글귀
    "오늘도 차곡차곡",
    "이 길, 오래 기억할게요",
    "함께라서 좋았던 하루",
    "창밖 바람이 좋았어요",
)

// ───────────────────────── 모아보기: 릴스 공유방처럼 자유 배치 + 간단 편집 ─────────────────────────

@Composable
fun Collage() {
    val items = remember { mutableStateListOf<Moment>().apply { addAll(Scenario.moments) } }
    var openIdx by remember { mutableIntStateOf(-1) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
            Box(Modifier.width(2650.dp).fillMaxHeight()) {
                items.forEachIndexed { i, m -> MomentCard(m) { openIdx = i } }
            }
        }
        if (openIdx in items.indices) {
            val idx = openIdx
            MomentEditor(
                m = items[idx],
                onChange = { items[idx] = it },
                onDelete = { items.removeAt(idx); openIdx = -1 },
                onClose = { openIdx = -1 },
            )
        }
    }
}

@Composable
private fun MomentCard(m: Moment, onClick: () -> Unit) {
    Box(
        Modifier
            .offset(m.x.dp, m.y.dp)
            .size(m.w.dp, m.h.dp)
            .graphicsLayer { rotationZ = m.rot }
            .clip(RoundedCornerShape(24.dp))
            .border(5.dp, Color.White.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        SceneArt(m.scene, Modifier.fillMaxSize(), seed = m.seed)
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))),
        )
        m.badge?.let {
            Text(
                it, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text(m.caption ?: m.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
            Text(m.stampText() ?: "", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
        }
    }
}

@Composable
private fun MomentEditor(m: Moment, onChange: (Moment) -> Unit, onDelete: () -> Unit, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg.copy(alpha = 0.94f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(Palette.Surface)
                .clickable(enabled = false) {},
        ) {
            // 미리보기: 타임스탬프와 글귀가 사진 위에 얹힌 모습
            Box(Modifier.size(760.dp, 580.dp)) {
                SceneArt(m.scene, Modifier.fillMaxSize(), seed = m.seed)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.25f), 0.3f to Color.Transparent, 0.7f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))),
                )
                if (m.stamp > 0) {
                    Column(
                        Modifier.align(Alignment.TopCenter).padding(top = 34.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (m.stamp == 2) {
                            Text(m.timeText(), color = Color.White, fontSize = 84.sp, fontWeight = FontWeight.Bold, style = TextStyle(shadow = textShadow))
                        }
                        Text(
                            m.date, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
                            style = TextStyle(shadow = textShadow),
                            modifier = Modifier
                                .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                }
                Text(
                    m.caption ?: "“${m.quote}”",
                    color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    style = TextStyle(shadow = textShadow),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 40.dp, vertical = 40.dp),
                )
            }
            // 편집 도구
            Column(
                Modifier.width(540.dp).height(580.dp).padding(40.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(m.title, color = Palette.Text, fontSize = 36.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold)
                    Text("편집", color = Palette.TextDim, fontSize = 20.sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    EditAction(
                        "타임스탬프", listOf("끔", "날짜", "날짜 + 시각")[m.stamp], Palette.Sky,
                    ) { onChange(m.copy(stamp = (m.stamp + 1) % 3)) }
                    EditAction("글귀", "바꾸기", Palette.Amber) {
                        val cur = captionPresets.indexOf(m.caption).let { if (it < 0) 0 else it }
                        onChange(m.copy(caption = captionPresets[(cur + 1) % captionPresets.size]))
                    }
                    EditAction("이 순간", "삭제", Palette.Danger, onClick = onDelete)
                }
                BigButton("완료", onClose, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun EditAction(label: String, value: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.SurfaceHi)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Palette.Text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Chip(value, color, 20)
    }
}

// ───────────────────────── 영상 보관함 (동영상 저장소) ─────────────────────────

@Composable
fun VideoVault() {
    val ctx = LocalContext.current
    var files by remember { mutableStateOf(VideoStore.list(ctx)) }
    var playing by remember { mutableStateOf<File?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (files.isEmpty()) {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text("영상 보관함이 비어 있어요", color = Palette.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Text(
                    "저작권 프리 도로 영상(mp4)을 아래 폴더에 넣고 새로고침하세요.",
                    color = Palette.TextDim, fontSize = 24.sp,
                )
                Text(
                    "Android/data/kr.hayuni.chagokchagok/files/Movies",
                    color = Palette.Mint, fontSize = 22.sp,
                )
                Text("파일 이름을 road.mp4 로 하면 카메라 화면 배경으로 반복 재생돼요.", color = Palette.TextDim, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                BigButton("새로고침", { files = VideoStore.list(ctx) })
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("영상 ${files.size}개", color = Palette.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(16.dp))
                    Text("오버레이로 찍은 5초 기록도 여기에 쌓여요", color = Palette.TextDim, fontSize = 20.sp)
                    Spacer(Modifier.weight(1f))
                    BigButton("새로고침", { files = VideoStore.list(ctx) }, filled = false)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxSize()) {
                    items(files, key = { it.absolutePath }) { f -> VideoCard(f) { playing = f } }
                }
            }
        }
        playing?.let { VideoPlayer(it) { playing = null } }
    }
}

private data class Probe(val frame: ImageBitmap?, val durationMs: Long)

private fun probe(file: File): Probe = runCatching {
    val r = MediaMetadataRetriever()
    try {
        r.setDataSource(file.absolutePath)
        val dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        Probe(r.getFrameAtTime(500_000)?.asImageBitmap(), dur)
    } finally {
        r.release()
    }
}.getOrDefault(Probe(null, 0L))

@Composable
private fun VideoCard(f: File, onClick: () -> Unit) {
    val info by produceState(Probe(null, 0L), f) { value = withContext(Dispatchers.IO) { probe(f) } }
    Box(
        Modifier
            .fillMaxHeight()
            .width(560.dp)
            .heightIn(max = 520.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Palette.Surface)
            .clickable(onClick = onClick),
    ) {
        val frame = info.frame
        if (frame != null) {
            Image(frame, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            SceneArt(SceneKind.Highway, Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))))
        Canvas(Modifier.align(Alignment.Center).size(110.dp)) {
            drawCircle(Color.Black.copy(alpha = 0.45f))
            val p = Path().apply {
                moveTo(size.width * 0.4f, size.height * 0.3f)
                lineTo(size.width * 0.72f, size.height * 0.5f)
                lineTo(size.width * 0.4f, size.height * 0.7f)
                close()
            }
            drawPath(p, Color.White)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (f.name.startsWith("clip_")) Chip("5초 기록", Palette.Amber, 18)
            if (f.nameWithoutExtension.equals("road", true)) Chip("카메라 배경", Palette.Mint, 18)
            Text(f.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            val sec = info.durationMs / 1000
            Text("%d:%02d".format(sec / 60, sec % 60), color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp)
        }
    }
}

@Composable
private fun VideoPlayer(f: File, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxHeight(0.92f)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black),
        ) {
            LoopingVideo(f, Modifier.fillMaxSize(), muted = false)
        }
        Text(
            "닫기", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.18f))
                .clickable(onClick = onClose)
                .padding(horizontal = 30.dp, vertical = 14.dp),
        )
    }
}

/** 파일을 반복 재생하는 영상 뷰 (카메라 배경, 플레이어 공용) */
@Composable
fun LoopingVideo(file: File, modifier: Modifier = Modifier, muted: Boolean = true) {
    AndroidView(
        modifier = modifier,
        factory = { c ->
            VideoView(c).apply {
                setVideoPath(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    if (muted) mp.setVolume(0f, 0f)
                    start()
                }
            }
        },
    )
}
