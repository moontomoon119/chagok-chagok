package kr.hayuni.chagokchagok.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import kr.hayuni.chagokchagok.R
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalContext
import kr.hayuni.chagokchagok.data.Palette
import kr.hayuni.chagokchagok.data.Prefs
import kr.hayuni.chagokchagok.data.SceneKind
import kr.hayuni.chagokchagok.data.TodayClip
import kr.hayuni.chagokchagok.data.VideoStore
import androidx.compose.ui.graphics.toArgb
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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


@Composable
fun ChagokApp(refreshTick: Int = 0, onQuickRecordChange: (Boolean) -> Unit = {}, onStartPressed: () -> Unit = {}) {
    var stage by remember { mutableStateOf(Stage.Start) }
    var cameFromMain by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(1) }          // 홈 = 갤러리 (오늘의 기록은 거기서 들어간다)
    var parked by remember { mutableStateOf(true) }          // 주차 브레이크 (데모용 토글)
    val ctx = LocalContext.current
    // 오늘의 기록: 처음에는 시나리오(여행을 마치고 귀가 중)의 기록이 들어 있고, 촬영하면 늘어난다.
    val clips = remember { mutableStateListOf<TodayClip>().apply { addAll(Prefs.loadClips(ctx)) } }
    fun persist() = Prefs.saveClips(ctx, clips)
    var quickRecord by remember { mutableStateOf(Prefs.quickRecord(ctx)) }   // 홈에서도 기록 버튼 유지
    var limit by remember { mutableStateOf<Int?>(null) }          // 자동 재생 중에만 사용
    var resetKey by remember { mutableIntStateOf(0) }             // 목업 되돌리기 때 화면 상태를 새로 만들기 위한 키
    var replayKey by remember { mutableIntStateOf(0) }
    var replaying by remember { mutableStateOf(false) }

    // 앱 밖(홈 기록 버튼)에서 늘어난 기록을 돌아왔을 때 다시 읽어 온다. (자동 재생 중에는 건드리지 않음)
    LaunchedEffect(refreshTick) {
        if (refreshTick == 0 || replaying) return@LaunchedEffect
        val fresh = Prefs.loadClips(ctx)
        if (fresh != clips.toList()) { clips.clear(); clips.addAll(fresh) }
    }

    // "하루 자동 재생": 오늘의 기록이 시간 순서대로 하나씩 나타난다
    LaunchedEffect(replayKey) {
        if (replayKey == 0 || clips.isEmpty()) return@LaunchedEffect
        replaying = true
        parked = false
        for (i in 0..clips.size) {
            limit = i
            delay(if (i == 0) 600 else 1500)
        }
        delay(600)
        limit = null
        parked = true
        replaying = false
    }

    // 목업(데모) 환경으로 되돌리기: 저장된 상태와 앱이 찍은 기록을 지우고 처음(귀가 중 시나리오)으로
    fun resetMock() {
        Prefs.resetAll(ctx)
        quickRecord = Prefs.quickRecord(ctx)
        VideoStore.deleteRecorded(ctx)
        clips.clear()
        clips.addAll(Scenario.defaultClips())
        limit = null
        replaying = false
        replayKey = 0            // 진행 중인 자동 재생 취소
        parked = true
        tab = 1
        cameFromMain = false
        resetKey++
        stage = Stage.Start
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
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))   // 키보드(ime) 여백은 제외: 키보드가 떠도 화면이 밀리지 않게,
    ) {
        when (stage) {
            Stage.Start -> StartScreen(onStart = { onStartPressed(); cameFromMain = false; stage = Stage.Camera })
            Stage.Camera -> CameraScreen(
                onBack = { stage = if (cameFromMain) Stage.Main else Stage.Start },
                onToday = { tab = 0; cameFromMain = true; stage = Stage.Main },
                // 촬영해도 화면은 넘어가지 않는다. 오늘의 기록에 한 칸 추가되고, 저장됐다는 안내만 돌려준다.
                onCaptured = {
                    val id = (clips.maxOfOrNull { it.id } ?: -1) + 1
                    val scenes = SceneKind.values()
                    clips.add(
                        TodayClip(
                            id, SimpleDateFormat("HH:mm", Locale.KOREA).format(Date()), "방금 이 길", "5초 기록",
                            "방금 찍은 순간이에요. 눌러서 제목을 바꿔 보세요.", "방금", Palette.Mint.toArgb(),
                            scenes[id % scenes.size], id,
                        ),
                    )
                    persist()
                    "오늘의 기록에 저장됐어요 ✓ (총 ${clips.size}개)"
                },
                count = clips.size,
                lastScene = clips.lastOrNull()?.scene,
                lastSeed = clips.lastOrNull()?.seed ?: 0,
            )
            Stage.Main -> Column(Modifier.fillMaxSize()) {
                TopBar(
                    tab = tab,
                    onTab = { tab = it },
                    parked = parked,
                    onParkedChange = { parked = it },
                    onCamera = { cameFromMain = true; stage = Stage.Camera },
                    todayCount = clips.size,
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                  key(resetKey) {
                    when (tab) {
                        0 -> TodayScreen(
                            clips = clips,
                            limit = limit,
                            replaying = replaying,
                            onReplay = { replayKey++ },
                            onCamera = { cameFromMain = true; stage = Stage.Camera },
                            onRename = { id, name ->
                                val i = clips.indexOfFirst { it.id == id }
                                if (i >= 0) { clips[i] = clips[i].copy(place = name); persist() }
                            },
                            onDelete = { id ->
                                clips.firstOrNull { it.id == id }?.media?.let { runCatching { java.io.File(it).delete() } }
                                clips.removeAll { it.id == id }; persist()
                            },
                            onRestoreDemo = { clips.clear(); clips.addAll(Scenario.defaultClips()); persist() },
                        )
                        1 -> GalleryScreen(parked)
                        else -> SettingsScreen(
                            quickRecord = quickRecord,
                            onQuickRecordChange = { quickRecord = it; Prefs.setQuickRecord(ctx, it); onQuickRecordChange(it) },
                            onResetMock = ::resetMock,
                        )
                    }
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
    todayCount: Int,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 44.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 왼쪽: 글자 로고 + 화면 이동 (갤러리가 홈, 오늘의 기록은 거기서 들어가는 보조 화면)
        if (tab == 1) {
            HeaderChip(Ico.Today, "오늘의 기록", badge = "$todayCount") { onTab(0) }
        } else {
            HeaderChip(Ico.Back, "갤러리") { onTab(1) }
        }

        Spacer(Modifier.weight(1f))

        // 오른쪽: 보조(상태·설정)는 작게, 핵심 행동(기록하기) 하나만 강조
        StatusChip(
            text = if (parked) "주차 중" else "주행 중 · 음성 모드",
            color = if (parked) Palette.Mint else Palette.Amber,
            icon = if (parked) Ico.Parking else Ico.Wheel,
            onClick = { onParkedChange(!parked) },
        )
        Spacer(Modifier.width(14.dp))
        IconCircle(Ico.Settings, selected = tab == 2) { onTab(if (tab == 2) 1 else 2) }
        Spacer(Modifier.width(18.dp))
        BigButton("기록하기", onCamera, icon = Ico.Record, height = 52.dp)
    }
}

/** 화면 이동 버튼: 테두리·색 없이 옅은 바탕에 흰 글자 */
@Composable
private fun HeaderChip(icon: Int, text: String, badge: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon, Color.White.copy(alpha = 0.85f), 24.dp)
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color.White, fontSize = Type.Label, fontWeight = FontWeight.SemiBold)
        if (badge != null) {
            Spacer(Modifier.width(10.dp))
            Text(badge, color = Color.White.copy(alpha = 0.55f), fontSize = Type.Label, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun IconCircle(icon: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { AppIcon(icon, Color.White.copy(alpha = if (selected) 1f else 0.7f), 28.dp) }
}

/** 주차/주행 표시: 색 없이 흐린 흰색 (탭하면 전환 - 데모용) */
@Composable
private fun StatusChip(text: String, color: Color, icon: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon, Color.White.copy(alpha = 0.6f), 22.dp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White.copy(alpha = 0.6f), fontSize = Type.Caption, fontWeight = FontWeight.Medium)
    }
}
