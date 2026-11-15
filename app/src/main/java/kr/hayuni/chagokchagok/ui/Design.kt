package kr.hayuni.chagokchagok.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.hayuni.chagokchagok.R

/**
 * 타입 스케일 — 화면별로 제각각이던 글자 크기를 이 일곱 단계로 모은다.
 * (2560x1440 @160dpi 차량 화면 기준. 새 화면/수정 시 숫자 대신 여기 값을 쓴다.)
 */
object Type {
    val Display = 72.sp    // 시작 슬로건, 귀갓길 낭독 첫 문장, 한마디 질문
    val Headline = 52.sp   // 화면 속 큰 문장 (회상, 낭독 본문, 카드 큰 장소명)
    val Title = 40.sp      // 화면·카드 제목, 빈 화면 안내
    val Subtitle = 28.sp   // 소제목, 강조 문장, 패널 제목
    val Body = 22.sp       // 본문, 버튼, 탭
    val Label = 20.sp      // 보조 문장, 헤더 칩
    val Caption = 18.sp    // 작은 칩, 부가 설명
}

/** 아이콘 모음 (res/drawable/ic_*.xml, 24dp 격자 · 선 2 · 둥근 끝). 색은 tint로 입힌다. */
object Ico {
    // 차량 신호
    @DrawableRes val Door = R.drawable.ic_door
    @DrawableRes val Window = R.drawable.ic_window
    @DrawableRes val Fog = R.drawable.ic_fog
    @DrawableRes val Wiper = R.drawable.ic_wiper
    @DrawableRes val Pin = R.drawable.ic_pin
    // 탭
    @DrawableRes val Today = R.drawable.ic_today
    @DrawableRes val Gallery = R.drawable.ic_gallery
    @DrawableRes val Settings = R.drawable.ic_settings
    // 동작
    @DrawableRes val Record = R.drawable.ic_record
    @DrawableRes val Back = R.drawable.ic_back
    @DrawableRes val Play = R.drawable.ic_play
    @DrawableRes val Plus = R.drawable.ic_plus
    @DrawableRes val Minus = R.drawable.ic_minus
    @DrawableRes val Fit = R.drawable.ic_fit
    @DrawableRes val Check = R.drawable.ic_check
    @DrawableRes val Mic = R.drawable.ic_mic
    @DrawableRes val Book = R.drawable.ic_book
    @DrawableRes val Lock = R.drawable.ic_lock
    @DrawableRes val Trash = R.drawable.ic_trash
    @DrawableRes val Clock = R.drawable.ic_clock
    @DrawableRes val Edit = R.drawable.ic_edit
    @DrawableRes val Text = R.drawable.ic_text
    // 하위 탭
    @DrawableRes val Collage = R.drawable.ic_collage
    @DrawableRes val Vault = R.drawable.ic_vault
    @DrawableRes val Map = R.drawable.ic_map
    @DrawableRes val Recall = R.drawable.ic_recall
    // 상태
    @DrawableRes val Parking = R.drawable.ic_parking
    @DrawableRes val Wheel = R.drawable.ic_wheel
}

@Composable
fun AppIcon(@DrawableRes res: Int, tint: Color, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    Icon(painterResource(res), contentDescription = null, tint = tint, modifier = modifier.size(size))
}
