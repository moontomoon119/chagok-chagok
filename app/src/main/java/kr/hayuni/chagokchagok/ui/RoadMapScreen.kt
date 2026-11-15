package kr.hayuni.chagokchagok.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.hayuni.chagokchagok.data.MapRoute
import kr.hayuni.chagokchagok.data.Palette
import kr.hayuni.chagokchagok.data.RoadMapData
import kr.hayuni.chagokchagok.data.RoadMapLoader
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

private class RoadStyle(val color: Color, val px: Float)

// 도로 등급별 스타일 (1=고속·간선, 2=국도급, 3=지방도, 5=소로). 높은 등급일수록 밝고 굵게.
private val roadOrder = listOf(
    5 to RoadStyle(Color(0xFF53697C).copy(alpha = 0.60f), 1.1f),
    3 to RoadStyle(Color(0xFF627B90).copy(alpha = 0.72f), 1.6f),
    2 to RoadStyle(Color(0xFF8099AE).copy(alpha = 0.85f), 2.6f),
    1 to RoadStyle(Color(0xFFA9BCCB).copy(alpha = 0.95f), 3.8f),
)

// 지역 이름표 (장식용, 대략 위치)
private val regionLabels = listOf(
    Triple("강원도", 127.93, 37.76),
    Triple("경기도", 127.25, 37.93),
    Triple("인천", 126.60, 37.50),
    Triple("서울", 127.00, 37.63),
)

/** 갤러리 > 지도: 도로망 위에 가족의 기억 경로를 얹은 지도 */
@Composable
fun RoadMapPage(parked: Boolean) {
    val ctx = LocalContext.current
    val data by produceState<RoadMapData?>(null) {
        value = withContext(Dispatchers.Default) { RoadMapLoader.load(ctx) }
    }
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF0F1923))
            .border(1.dp, Palette.Line, RoundedCornerShape(32.dp)),
    ) {
        val map = data
        if (map == null) {
            Text("도로망을 불러오는 중…", color = Palette.TextDim, fontSize = 28.sp, modifier = Modifier.align(Alignment.Center))
        } else {
            RoadMapContent(map)
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

@Composable
private fun RoadMapContent(map: RoadMapData) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var visible by remember { mutableStateOf(map.routes.map { it.id }.toSet()) }
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) { draw.animateTo(1f, tween(3000, easing = LinearOutSlowInEasing)) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        // 처음에는 서울~춘천~인천 경로 일대(위도 37.30~37.95)가 화면 높이에 꽉 차게 맞춘다.
        val focusH = 0.66f
        val fit = hPx * 0.92f / focusH
        val s = fit * zoom
        val cx = wPx / 2f + pan.x
        val cy = hPx / 2f + pan.y
        val wcx = map.width / 2f
        val wcy = map.y(37.625)
        fun sx(x: Float) = cx + (x - wcx) * s
        fun sy(y: Float) = cy + (y - wcy) * s

        fun zoomAround(pivot: Offset, factor: Float) {
            val nz = (zoom * factor).coerceIn(0.5f, 28f)
            val k = nz / zoom
            val center = Offset(wPx / 2f, hPx / 2f)
            pan = (pivot - center) - ((pivot - center) - pan) * k
            zoom = nz
        }

        val measure = remember { PathMeasure() }
        val seg = remember { Path() }

        Canvas(
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(wPx, hPx) {
                    detectTransformGestures { centroid, p, z, _ ->
                        val nz = (zoom * z).coerceIn(0.5f, 28f)
                        val k = nz / zoom
                        val center = Offset(wPx / 2f, hPx / 2f)
                        pan = (centroid - center) - ((centroid - center) - pan) * k + p
                        zoom = nz
                    }
                },
        ) {
            val zf = zoom.pow(0.35f)
            withTransform({
                translate(cx - wcx * s, cy - wcy * s)
                scale(s, s, Offset.Zero)
            }) {
                for ((rank, st) in roadOrder) {
                    val path = map.roads[rank] ?: continue
                    drawPath(path, st.color, style = Stroke(st.px * zf / s, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                for (r in map.routes) {
                    if (r.id !in visible) continue
                    measure.setPath(r.path, false)
                    seg.rewind()
                    measure.getSegment(0f, measure.length * draw.value, seg, true)
                    drawPath(seg, r.color.copy(alpha = 0.22f), style = Stroke(15f * zf / s, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(seg, r.color, style = Stroke(5.5f * zf / s, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }

        // 지역 이름표
        regionLabels.forEach { (name, lon, lat) ->
            val x = sx(map.x(lon)).roundToInt()
            val y = sy(map.y(lat)).roundToInt()
            Text(
                name, color = Color(0xFFB6C3CF), fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .offset { IntOffset(x, y) }
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF55657A).copy(alpha = 0.75f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }

        // 장소 핀
        map.places.forEach { pl ->
            val show = pl.id == "home" || map.routes.any { it.id in visible && pl.id in it.stops }
            if (!show) return@forEach
            val x = sx(pl.x).roundToInt()
            val y = sy(pl.y).roundToInt()
            val home = pl.id == "home"
            val labelText = @Composable {
                Text(
                    pl.label, color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Palette.Bg.copy(alpha = 0.88f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            val dot = @Composable {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Palette.Bg)
                        .border(if (home) 7.dp else 5.dp, if (home) Color.White else pl.color, CircleShape),
                )
            }
            if (pl.id == "lake") {
                // 가까이 붙은 두 핀(의암호/케이블카) 라벨이 겹치지 않게 왼쪽·아래로 뺀다
                Row(
                    Modifier.offset { IntOffset(x - 14.dp.roundToPx() - 330.dp.roundToPx(), y - 14.dp.roundToPx() + 16.dp.roundToPx()) }.width(330.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) { labelText() }
                Box(Modifier.offset { IntOffset(x - 14.dp.roundToPx(), y - 14.dp.roundToPx()) }) { dot() }
            } else {
                Row(
                    Modifier.offset { IntOffset(x - 14.dp.roundToPx(), y - 14.dp.roundToPx()) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dot()
                    Spacer(Modifier.width(10.dp))
                    labelText()
                }
            }
        }

        // 상단 왼쪽: 제목 + 범례(탭하면 경로 켜고 끔)
        Row(
            Modifier.align(Alignment.TopStart).padding(28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text("차곡차곡 · 도로 위의 기억", color = Palette.Amber, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            map.routes.forEach { r -> LegendItem(r, r.id in visible) {
                visible = if (r.id in visible) visible - r.id else visible + r.id
            } }
        }

        // 상단 오른쪽: 확대/축소/전체
        Row(
            Modifier.align(Alignment.TopEnd).padding(28.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MapButton("확대 +") { zoomAround(Offset(wPx / 2f, hPx / 2f), 1.6f) }
            MapButton("축소 −") { zoomAround(Offset(wPx / 2f, hPx / 2f), 1f / 1.6f) }
            MapButton("전체") { zoom = 1f; pan = Offset.Zero; visible = map.routes.map { it.id }.toSet() }
        }

        // 하단: 경로 요약 + 출처
        Column(
            Modifier.align(Alignment.BottomStart).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            map.routes.filter { it.id in visible }.forEach { r ->
                Text(
                    "${r.label} · ${r.tag} · ${"%.1f".format(r.km)}km",
                    color = r.color, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Palette.Bg.copy(alpha = 0.8f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
            Text("국토교통 KTDB 도로망 기반 시연 데이터 · 실제 주행 기록 아님", color = Palette.TextDim, fontSize = 16.sp)
        }
    }
}

@Composable
private fun LegendItem(r: MapRoute, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 34.dp, height = 5.dp).background(if (on) r.color else r.color.copy(alpha = 0.25f)))
        Spacer(Modifier.width(10.dp))
        Text(
            r.shortName, color = if (on) r.color else Palette.TextDim.copy(alpha = 0.6f),
            fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MapButton(text: String, onClick: () -> Unit) {
    Text(
        text, color = Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF243646))
            .border(1.dp, Palette.Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 14.dp),
    )
}
