package kr.hayuni.chagokchagok.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kr.hayuni.chagokchagok.data.Palette
import kr.hayuni.chagokchagok.data.Scenario

@Composable
fun ChagokTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Amber,
            background = Palette.Bg,
            surface = Palette.Surface,
            onSurface = Palette.Text,
            onBackground = Palette.Text,
        ),
        content = content,
    )
}

private enum class Stage { Start, Camera, Main }

private val tabs = listOf("오늘의 기록", "갤러리", "설정")

@Composable
fun ChagokApp(onBackgroundRecord: () -> Unit = {}) {
    var stage by remember { mutableStateOf(Stage.Start) }
    var cameFromMain by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var parked by remember { mutableStateOf(true) }          // 주차 브레이크 (데모용 토글)
    var shown by remember { mutableIntStateOf(0) }
    var replayKey by remember { mutableIntStateOf(0) }
    var replaying by remember { mutableStateOf(false) }

    // 카메라 셔터 → 오늘의 기록 시나리오가 자동으로 흘러간다
    LaunchedEffect(replayKey) {
        if (replayKey == 0) return@LaunchedEffect
        replaying = true
        parked = false
        shown = 0
        for (i in 1..Scenario.events.size) {
            delay(1500)
            shown = i
        }
        delay(900)
        parked = true
        replaying = false
    }

    BackHandler(enabled = stage != Stage.Start) {
        stage = when (stage) {
            Stage.Camera -> if (cameFromMain) Stage.Main else Stage.Start
            else -> Stage.Start
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when (stage) {
            Stage.Start -> StartScreen(onStart = { cameFromMain = false; stage = Stage.Camera })
            Stage.Camera -> CameraScreen(
                onBack = { stage = if (cameFromMain) Stage.Main else Stage.Start },
                onToday = { tab = 0; cameFromMain = true; stage = Stage.Main },
                // 촬영해도 화면은 넘어가지 않는다. 오늘의 기록에 한 칸씩 쌓이고, 저장됐다는 안내만 돌려준다.
                onCaptured = {
                    val n = (shown + 1).coerceAtMost(Scenario.events.size)
                    shown = n
                    if (n >= Scenario.events.size) "오늘의 기록 ${n}개가 모두 쌓였어요 ✓" else "오늘의 기록 ${n}번째 순간이 저장됐어요 ✓"
                },
                onBackground = onBackgroundRecord,
                count = shown,
            )
            Stage.Main -> Column(Modifier.fillMaxSize()) {
                TopBar(
                    tab = tab,
                    onTab = { tab = it },
                    parked = parked,
                    onParkedChange = { parked = it },
                    onCamera = { cameFromMain = true; stage = Stage.Camera },
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        0 -> TodayScreen(shown, replaying, onReplay = { replayKey++ }, onCamera = { cameFromMain = true; stage = Stage.Camera })
                        1 -> GalleryScreen(parked)
                        else -> SettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    tab: Int,
    onTab: (Int) -> Unit,
    parked: Boolean,
    onParkedChange: (Boolean) -> Unit,
    onCamera: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 44.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            Text("차곡차곡", color = Palette.Amber, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Palette.Surface)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tabs.forEachIndexed { i, label ->
                val on = i == tab
                Text(
                    label,
                    color = if (on) Palette.Text else Palette.TextDim,
                    fontSize = 24.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) Palette.SurfaceHi else Palette.Surface)
                        .clickable { onTab(i) }
                        .padding(horizontal = 36.dp, vertical = 14.dp),
                )
            }
        }

        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChip(
                text = if (parked) "주차 중" else "주행 중 · 음성 모드",
                color = if (parked) Palette.Mint else Palette.Amber,
            )
            Spacer(Modifier.width(14.dp))
            Switch(
                checked = parked,
                onCheckedChange = onParkedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Palette.Mint, checkedThumbColor = Palette.Bg),
            )
            Spacer(Modifier.width(22.dp))
            BigButton("● 기록하기", onCamera)
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(11.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(text, color = color, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}
