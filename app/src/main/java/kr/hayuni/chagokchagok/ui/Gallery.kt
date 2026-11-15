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
                2 -> RoadMapPage(parked)
                else -> RecallPage()
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
