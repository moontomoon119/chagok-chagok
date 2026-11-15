package kr.hayuni.chagokchagok.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.hayuni.chagokchagok.data.Moment
import kr.hayuni.chagokchagok.data.Palette
import kr.hayuni.chagokchagok.data.SceneKind
import kr.hayuni.chagokchagok.data.Scenario
import kotlin.math.roundToInt

/** 탭 2: 갤러리 = 모아보기(자유 배치) / 지도 / 회상 */
@Composable
fun GalleryScreen(parked: Boolean) {
    var sub by remember { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 44.dp, end = 44.dp, top = 8.dp, bottom = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubTabs(listOf("모아보기", "영상 보관함", "지도", "회상"), sub) { sub = it }
            Spacer(Modifier.weight(1f))
            if (sub == 0) Text("옆으로 밀어서 둘러보세요  →", color = Palette.TextDim, fontSize = 20.sp)
        }
        Spacer(Modifier.height(22.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (sub) {
                0 -> Collage()
                1 -> VideoVault()
                2 -> FamilyMap(parked)
                else -> RecallPage()
            }
        }
    }
}

// ───────────────────────── 지도 ─────────────────────────

@Composable
private fun FamilyMap(parked: Boolean) {
    var filter by remember { mutableStateOf("전체") }
    fun visible(era: String, rainy: Boolean) = when (filter) {
        "전체" -> true
        "비 오던 날" -> rainy
        else -> era == filter
    }
    val routes = Scenario.routes.filter { visible(it.era, it.rainy) }
    val names = routes.map { it.name }.toSet()

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Scenario.filters.forEach { f ->
                val on = f == filter
                Text(
                    f,
                    color = if (on) Palette.Bg else Palette.Text,
                    fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) Palette.Amber else Palette.Surface)
                        .clickable { filter = f }
                        .padding(horizontal = 26.dp, vertical = 12.dp),
                )
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Palette.Surface)
                .border(1.dp, Palette.Line, RoundedCornerShape(32.dp)),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(10.dp)) {
                val density = LocalDensity.current
                val wPx = with(density) { maxWidth.toPx() }
                val hPx = with(density) { maxHeight.toPx() }

                Canvas(Modifier.fillMaxSize()) {
                    val step = 80.dp.toPx()
                    var x = 0f
                    while (x < size.width) { drawLine(Palette.Line.copy(alpha = 0.4f), Offset(x, 0f), Offset(x, size.height)); x += step }
                    var y = 0f
                    while (y < size.height) { drawLine(Palette.Line.copy(alpha = 0.4f), Offset(0f, y), Offset(size.width, y)); y += step }
                    routes.forEach { r ->
                        val path = Path()
                        r.points.forEachIndexed { i, p ->
                            val px = p.x * size.width
                            val py = p.y * size.height
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(
                            path, r.color.copy(alpha = 0.92f),
                            style = Stroke(r.width.dp.toPx() * 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
                Scenario.pins.filter { it.route in names }.forEach { pin ->
                    val px = (pin.pos.x * wPx).roundToInt()
                    val py = (pin.pos.y * hPx).roundToInt()
                    Row(
                        Modifier.offset { IntOffset(px - 16.dp.roundToPx(), py - 16.dp.roundToPx()) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Palette.Bg)
                                .border(6.dp, pin.color, CircleShape),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            pin.label, color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Palette.Bg.copy(alpha = 0.85f))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            if (!parked) {
                Box(
                    Modifier.fillMaxSize().background(Palette.Bg.copy(alpha = 0.94f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("달리는 중에는 음성으로만 동작해요", color = Palette.Amber, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                        Text("지도 탐색은 주차 브레이크를 걸었을 때만 열려요.", color = Palette.TextDim, fontSize = 24.sp)
                        Spacer(Modifier.height(8.dp))
                        Chip("“작년 춘천 이야기 들려줘”  라고 말해 보세요", Palette.Mint, 22)
                    }
                }
            }
        }
    }
}

// ───────────────────────── 회상 ─────────────────────────

@Composable
private fun RecallPage() {
    var recalled by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp))) {
        SceneArt(SceneKind.LakeFog, Modifier.fillMaxSize(), seed = 5)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
        Column(
            Modifier.align(Alignment.Center).widthIn(max = 1200.dp).padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Chip("3년 뒤 가을", Palette.Sky, 22)
                Chip("의암호 길", Palette.Amber, 22)
            }
            Text(
                "“3년 전 이 길에서\n뒷창문 두 개가 동시에 내려갔어요.\n그날 이야기 들려드릴까요?”",
                color = Color.White, fontSize = 52.sp, lineHeight = 70.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            )
            if (!recalled) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BigButton("들려주세요", { recalled = true })
                    BigButton("다음에요", {}, filled = false)
                }
            } else {
                Text(
                    "「${Scenario.STORY_TITLE}」 — 안개등을 켠 의암호 길, 호수 바람이 들어오던 뒷창문. 그날 여섯 살 딸의 목소리가 이어서 나와요.",
                    color = Palette.Mint, fontSize = 26.sp, lineHeight = 38.sp, textAlign = TextAlign.Center,
                )
                Chip("♪ 딸 목소리 재생", Palette.Amber, 22)
            }
        }
    }
}
