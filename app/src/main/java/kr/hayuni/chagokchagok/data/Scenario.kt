package kr.hayuni.chagokchagok.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * 1차 제출용 시나리오 데이터.
 * 접수양식 4장 "주요 사용 시나리오"(가을 토요일 춘천 당일치기)를 그대로 옮긴 예시 데이터이며,
 * 실제 차량 신호가 아니라 설명을 위해 지어낸 값이다. (2차에서 Vehicle SDK 공급자로 교체 예정)
 */
object Palette {
    val Bg = Color(0xFF0E151D)
    val Surface = Color(0xFF17212C)
    val SurfaceHi = Color(0xFF1F2C3A)
    val Line = Color(0xFF2B3A4B)
    val Text = Color(0xFFEAF0F6)
    val TextDim = Color(0xFF8DA0B3)
    val Amber = Color(0xFFF2B35E)
    val Mint = Color(0xFF6FCFB3)
    val Sky = Color(0xFF7FB6FF)
    val Lilac = Color(0xFFB9A4F2)
    val Danger = Color(0xFFE57373)
}

data class TimelineEvent(
    val time: String,
    val place: String,
    val title: String,
    val detail: String,
    val tag: String,
    val color: Color,
)

data class StoryLine(
    val text: String,
    val evidence: List<String>,
    val daughterVoice: Boolean = false,
)

data class Route(
    val name: String,
    val points: List<Offset>,   // 0..1 정규화 좌표
    val color: Color,
    val width: Float,           // 자주 다닌 길일수록 굵게
    val era: String,
    val rainy: Boolean,
    val trips: Int,
)

data class Pin(val label: String, val pos: Offset, val route: String, val color: Color)

object Scenario {
    val events = listOf(
        TimelineEvent("08:42", "우리 집", "출발", "주차 브레이크를 풀자 기록이 시작됐어요", "기록 시작", Palette.Mint),
        TimelineEvent("09:25", "가평휴게소", "18분 쉬어 감", "문 네 개가 열렸다가 18분 뒤에 닫혔어요", "문 4개", Palette.Sky),
        TimelineEvent("10:05", "의암호 길", "안개 속을 달림", "안개등이 켜지고 뒷창문 두 개가 3분쯤 내려갔어요", "안개등 · 창문", Palette.Amber),
        TimelineEvent("12:45", "춘천 케이블카", "한마디 남김", "다시 탑승하자 차가 물었어요. “여기 어땠어요?”", "한마디", Palette.Lilac),
        TimelineEvent("15:20", "귀갓길", "집으로 출발", "목적지를 집으로 바꾸자 차가 하루를 읽어 줄 준비를 해요", "귀갓길", Palette.Mint),
    )

    const val QUESTION = "여기 어땠어요?"
    const val ANSWER = "케이블카가 호수 위를 날았어. 물고기가 우리 봤을까?"

    const val STORY_TITLE = "안개가 먼저 와 있던 날"
    val story = listOf(
        StoryLine(
            "춘천이 가까워지자 저는 안개등을 켰어요.",
            listOf("안개등 10:05", "의암호 길"),
        ),
        StoryLine(
            "가평휴게소에서 문 네 개가 열렸다 닫히기까지 18분, 저는 조용히 기다렸어요.",
            listOf("문 4개 · 18분", "가평휴게소 09:25"),
        ),
        StoryLine(
            "뒷창문 두 개가 3분 동안 내려가자 호수 바람이 차 안으로 들어왔어요.",
            listOf("창문 2개 · 3분", "의암호 길"),
        ),
        StoryLine(
            "케이블카에서 돌아온 딸이 말했어요. “케이블카가 호수 위를 날았어. 물고기가 우리 봤을까?”",
            listOf("한마디 12:45", "딸 목소리"),
            daughterVoice = true,
        ),
        StoryLine(
            "이제 집까지 1시간 35분. 오늘의 길은 가족지도에 남았어요.",
            listOf("귀갓길 15:20", "가족지도"),
        ),
    )

    val filters = listOf("전체", "유치원 시절", "초등 시절", "비 오던 날")

    val routes = listOf(
        Route(
            "등하원 길",
            listOf(Offset(0.18f, 0.78f), Offset(0.22f, 0.68f), Offset(0.15f, 0.58f)),
            Palette.TextDim, 15f, "유치원 시절", false, 212,
        ),
        Route(
            "춘천 가는 길",
            listOf(
                Offset(0.18f, 0.78f), Offset(0.28f, 0.71f), Offset(0.38f, 0.53f),
                Offset(0.49f, 0.43f), Offset(0.60f, 0.31f), Offset(0.70f, 0.22f),
            ),
            Palette.Amber, 13f, "유치원 시절", false, 9,
        ),
        Route(
            "송도 가는 길",
            listOf(Offset(0.18f, 0.78f), Offset(0.32f, 0.87f), Offset(0.47f, 0.90f), Offset(0.62f, 0.84f)),
            Palette.Mint, 9f, "유치원 시절", true, 4,
        ),
        Route(
            "양평 가는 길",
            listOf(Offset(0.18f, 0.78f), Offset(0.27f, 0.62f), Offset(0.34f, 0.42f), Offset(0.30f, 0.20f)),
            Palette.Lilac, 8f, "초등 시절", false, 3,
        ),
    )

    val pins = listOf(
        Pin("우리 집", Offset(0.18f, 0.78f), "등하원 길", Palette.Text),
        Pin("가평휴게소 · 18분", Offset(0.38f, 0.53f), "춘천 가는 길", Palette.Sky),
        Pin("의암호 길 · 안개", Offset(0.60f, 0.31f), "춘천 가는 길", Palette.Amber),
        Pin("케이블카 · 한마디", Offset(0.70f, 0.22f), "춘천 가는 길", Palette.Lilac),
        Pin("송도 · 비 오던 날", Offset(0.62f, 0.84f), "송도 가는 길", Palette.Mint),
        Pin("양평 · 초등 시절", Offset(0.30f, 0.20f), "양평 가는 길", Palette.Lilac),
    )
}
