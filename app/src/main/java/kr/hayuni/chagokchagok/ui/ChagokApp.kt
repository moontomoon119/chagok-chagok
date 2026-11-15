package kr.hayuni.chagokchagok.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

private val tabs = listOf("오늘의 기록", "한마디", "귀갓길 낭독", "가족지도", "회상 · 이사")

@Composable
fun ChagokApp() {
    var tab by remember { mutableIntStateOf(0) }
    var parked by remember { mutableStateOf(true) }          // 주차 브레이크 (데모용 토글)
    var shown by remember { mutableIntStateOf(Scenario.events.size) }
    var replayKey by remember { mutableIntStateOf(0) }
    var replaying by remember { mutableStateOf(false) }

    // 시나리오 재생: 출발(주행 중) → 이벤트가 시간 순으로 쌓임 → 귀갓길 도착(주차)
    LaunchedEffect(replayKey) {
        if (replayKey == 0) return@LaunchedEffect
        replaying = true
        tab = 0
        parked = false
        shown = 0
        for (i in 1..Scenario.events.size) {
            delay(1300)
            shown = i
        }
        delay(900)
        parked = true
        replaying = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing),
    ) {
        TopBar(
            parked = parked,
            onParkedChange = { parked = it },
            clock = if (shown > 0) Scenario.events[shown - 1].time else "--:--",
            replaying = replaying,
            onReplay = { replayKey++ },
        )
        Row(Modifier.fillMaxSize()) {
            NavRail(tab) { tab = it }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                when (tab) {
                    0 -> HomeScreen(shown = shown)
                    1 -> VoiceScreen()
                    2 -> StoryScreen()
                    3 -> MapScreen(parked = parked)
                    else -> RecallScreen()
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    parked: Boolean,
    onParkedChange: (Boolean) -> Unit,
    clock: String,
    replaying: Boolean,
    onReplay: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Palette.Surface)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("차곡차곡", color = Palette.Amber, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        Text("차가 기억하는 우리 가족 지도", color = Palette.TextDim, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))

        StatusChip(
            text = if (parked) "주차 중 · 전체 기능" else "주행 중 · 음성 모드",
            color = if (parked) Palette.Mint else Palette.Amber,
        )
        Spacer(Modifier.width(20.dp))
        Text(clock, color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(20.dp))
        Text("주차 브레이크", color = Palette.TextDim, fontSize = 13.sp)
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = parked,
            onCheckedChange = onParkedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Palette.Mint,
                checkedThumbColor = Palette.Bg,
            ),
        )
        Spacer(Modifier.width(20.dp))
        Button(
            onClick = onReplay,
            enabled = !replaying,
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Amber, contentColor = Palette.Bg),
        ) {
            Text(if (replaying) "재생 중…" else "시나리오 재생", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NavRail(selected: Int, onSelect: (Int) -> Unit) {
    Column(
        Modifier
            .width(190.dp)
            .fillMaxHeight()
            .background(Palette.Surface)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEachIndexed { i, label ->
            val on = i == selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) Palette.SurfaceHi else Palette.Surface)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(width = 5.dp, height = 28.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (on) Palette.Amber else Palette.Surface),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    label,
                    color = if (on) Palette.Text else Palette.TextDim,
                    fontSize = 17.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "예시 데이터로 구성한\n1차 시연 버전입니다.\n(영상 속 가족과 수치는\n설명용으로 지어낸 값)",
            color = Palette.TextDim,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}
