package kr.hayuni.chagokchagok.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import kotlin.math.roundToInt

/** 탭 3: 설정 = 기억 이사 / 화질 설정 / 개인정보 보호 + (맨 아래) 목업 환경 되돌리기 */
@Composable
fun SettingsScreen(quickRecord: Boolean, onQuickRecordChange: (Boolean) -> Unit, onResetMock: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 44.dp, end = 44.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            MovePanel(Modifier.weight(1f).fillMaxHeight())
            QualityPanel(Modifier.weight(1f).fillMaxHeight(), quickRecord, onQuickRecordChange)
            PrivacyPanel(Modifier.weight(1.1f).fillMaxHeight())
        }
        MockResetBar(onResetMock)
    }
}

/** 목업(데모) 제출용: 앱을 처음 상태로 되돌린다 */
@Composable
private fun MockResetBar(onReset: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Palette.Surface)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("목업 환경으로 되돌리기", color = Palette.Text, fontSize = Type.Body, fontWeight = FontWeight.Bold)
            Text(
                if (confirm) "오늘의 기록·갤러리 편집·앱이 찍은 5초 기록을 지우고 시작 화면으로 돌아가요."
                else "시연용 앱이라서, 모든 변경을 처음(여행을 마치고 귀가 중) 상태로 되돌릴 수 있어요.",
                color = if (confirm) Palette.Danger else Palette.TextDim, fontSize = Type.Label,
            )
        }
        if (!confirm) {
            BigButton("처음 상태로 되돌리기", { confirm = true }, filled = false, height = 48.dp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                BigButton("되돌리기", { confirm = false; onReset() }, height = 48.dp)
                BigButton("취소", { confirm = false }, filled = false, height = 48.dp)
            }
        }
    }
}

@Composable
private fun PanelTitle(title: String, sub: String, icon: Int, tint: androidx.compose.ui.graphics.Color) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape).background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { AppIcon(icon, tint, 26.dp) }
            Spacer(Modifier.width(16.dp))
            Text(title, color = Palette.Text, fontSize = Type.Subtitle, fontWeight = FontWeight.Bold)
        }
        Text(sub, color = Palette.TextDim, fontSize = Type.Caption, lineHeight = 27.sp)
    }
}

@Composable
private fun MovePanel(modifier: Modifier) {
    var moving by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(moving) {
        if (moving) {
            progress = 0f
            for (i in 1..60) { delay(50); progress = i / 60f }
        }
    }
    val done = progress >= 1f

    Panel(modifier) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            PanelTitle("기억 이사", "기록은 차가 아니라 가족 계정에 쌓여요. 새 차에 로그인하면 지도가 그대로 따라와요.", Ico.Map, Palette.Amber)
            Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Stat("누적 거리", "%,d km".format((87412 * progress).roundToInt()), Palette.Amber)
                Stat("이야기", "%d편".format((214 * progress).roundToInt()), Palette.Mint)
                Stat("핀", "%,d개".format((1038 * progress).roundToInt()), Palette.Sky)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = Palette.Amber, trackColor = Palette.Line,
                )
            }
            when {
                !moving -> BigButton("새 현대차에 로그인 (데모)", { moving = true }, Modifier.fillMaxWidth(), height = 52.dp)
                !done -> Text("가족지도를 새 차로 옮기는 중…", color = Palette.Mint, fontSize = Type.Body, fontWeight = FontWeight.SemiBold)
                else -> Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Chip("이사 완료", Palette.Mint, 20)
                    BigButton("다시 보기", { moving = false; progress = 0f }, filled = false, height = 48.dp)
                }
            }
        }
    }
}

@Composable
private fun QualityPanel(modifier: Modifier, quickRecord: Boolean, onQuickRecordChange: (Boolean) -> Unit) {
    val options = listOf(
        Triple("표준", "720p", "저장 공간을 아껴요"),
        Triple("고화질", "1080p", "차 화면에 선명하게 (추천)"),
        Triple("최고화질", "4K", "저장 공간을 가장 많이 써요"),
    )
    var sel by remember { mutableStateOf(1) }
    Panel(modifier) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            PanelTitle("화질 설정", "1초 기록 영상의 화질을 골라요.", Ico.Vault, Palette.Sky)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                options.forEachIndexed { i, (name, res, desc) ->
                    val on = i == sel
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (on) Palette.Amber.copy(alpha = 0.18f) else Palette.SurfaceHi)
                            .clickable { sel = i }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(name, color = if (on) Palette.Amber else Palette.Text, fontSize = Type.Body, fontWeight = FontWeight.Bold)
                            Text(desc, color = Palette.TextDim, fontSize = Type.Caption)
                        }
                        Text(res, color = if (on) Palette.Amber else Palette.TextDim, fontSize = Type.Body, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            ToggleRow("홈에서도 기록 버튼 유지", "앱을 나가도 화면 위에 기록 버튼이 떠 있어요. 누르면 카메라·소리 5초 저장", quickRecord, onChange = onQuickRecordChange)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("저장 공간 사용 ${listOf(31, 48, 82)[sel]}%", color = Palette.TextDim, fontSize = Type.Label)
                LinearProgressIndicator(
                    progress = { listOf(0.31f, 0.48f, 0.82f)[sel] },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = Palette.Mint, trackColor = Palette.Line,
                )
            }
        }
    }
}

@Composable
private fun PrivacyPanel(modifier: Modifier) {
    var enc by remember { mutableStateOf(true) }
    var voice by remember { mutableStateOf(false) }
    var wipe by remember { mutableStateOf(true) }
    Panel(modifier) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            PanelTitle("개인정보 보호", "위치와 목소리는 가족 계정 단위로만 보관해요.", Ico.Lock, Palette.Mint)
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                ToggleRow("위치·음성 암호화", "가족 계정 단위로 암호화해서 보관해요", enc) { enc = it }
                ToggleRow("아이 목소리 원본 보관", "보호자가 켜야만 원본을 보관해요", voice) { voice = it }
                ToggleRow("차를 팔 때 데이터 삭제", "차 안에 남은 데이터를 모두 지워요", wipe) { wipe = it }
                ToggleRow("한마디는 물었을 때만", "늘 녹음하지 않아요 (항상 켜짐)", true, locked = true) {}
            }
            Spacer(Modifier.height(1.dp))
        }
    }
}

@Composable
private fun ToggleRow(title: String, desc: String, checked: Boolean, locked: Boolean = false, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Palette.Text, fontSize = Type.Body, fontWeight = FontWeight.SemiBold)
            Text(desc, color = Palette.TextDim, fontSize = Type.Caption)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = !locked,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Palette.Mint, checkedThumbColor = Palette.Bg,
                disabledCheckedTrackColor = Palette.Mint.copy(alpha = 0.5f), disabledCheckedThumbColor = Palette.Bg,
            ),
        )
    }
}
