package kr.hayuni.chagokchagok.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun Chip(text: String, color: Color, size: Int = 16) {
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
fun SubTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Palette.Surface)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { i, l ->
            val on = i == selected
            Text(
                l,
                color = if (on) Palette.Bg else Palette.TextDim,
                fontSize = 20.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Palette.Amber else Palette.Surface)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 28.dp, vertical = 12.dp),
            )
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
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 36.dp),
        colors = if (filled) {
            ButtonDefaults.buttonColors(containerColor = Palette.Amber, contentColor = Palette.Bg)
        } else {
            ButtonDefaults.buttonColors(containerColor = Palette.SurfaceHi, contentColor = Palette.Text)
        },
    ) { Text(text, fontSize = 21.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun Stat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Palette.TextDim, fontSize = 16.sp)
    }
}
