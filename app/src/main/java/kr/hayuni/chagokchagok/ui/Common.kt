package kr.hayuni.chagokchagok.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.hayuni.chagokchagok.data.Palette

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    fill: Color = Palette.Surface,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(fill)
            .padding(28.dp),
        content = content,
    )
}

@Composable
fun Chip(text: String, color: Color, size: Int = 18) {
    Text(
        text,
        color = color,
        fontSize = size.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 16.dp, vertical = 7.dp),
    )
}

@Composable
fun SubTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, icons: List<Int>? = null) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Palette.Surface)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { i, l ->
            val on = i == selected
            val c = if (on) Palette.Bg else Palette.TextDim
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Palette.Amber else Palette.Surface)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 24.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icons?.getOrNull(i)?.let {
                    AppIcon(it, c, 24.dp)
                    Spacer(Modifier.width(9.dp))
                }
                Text(
                    l,
                    color = c,
                    fontSize = Type.Body,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
    icon: Int? = null,
    height: androidx.compose.ui.unit.Dp = 56.dp,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 28.dp),
        colors = if (filled) {
            ButtonDefaults.buttonColors(containerColor = Palette.Amber, contentColor = Palette.Bg)
        } else {
            ButtonDefaults.buttonColors(containerColor = Palette.SurfaceHi, contentColor = Palette.Text)
        },
    ) {
        icon?.let {
            AppIcon(it, if (filled) Palette.Bg else Palette.Text, 26.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, fontSize = Type.Body, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Stat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = Type.Title, fontWeight = FontWeight.Bold)
        Text(label, color = Palette.TextDim, fontSize = Type.Caption)
    }
}

/** 제목 수정 입력 상자 (직접 입력 + 추천 제목 탭) */
@Composable
fun RenameBox(initial: String, presets: List<String>, onSave: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    // 저장/취소로 입력 상자가 사라질 때 키보드를 확실히 닫는다 (안 닫히면 화면이 밀린 채 남음)
    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("제목 수정", color = Palette.TextDim, fontSize = Type.Label)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Palette.Bg)
                .border(2.dp, Palette.Amber, RoundedCornerShape(18.dp))
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= 24) text = it },
                singleLine = true,
                textStyle = TextStyle(color = Palette.Text, fontSize = Type.Subtitle, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(Palette.Amber),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave(text.trim().ifEmpty { initial }) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        presets.forEach { p ->
            Text(
                p, color = Palette.Sky, fontSize = Type.Label, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Palette.Sky.copy(alpha = 0.14f))
                    .clickable { text = p }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigButton("저장", { onSave(text.trim().ifEmpty { initial }) })
            BigButton("취소", onCancel, filled = false)
        }
    }
}
