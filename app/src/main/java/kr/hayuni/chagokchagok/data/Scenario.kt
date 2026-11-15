package kr.hayuni.chagokchagok.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

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

/** 장면 종류. ui/Photos.kt 에서 각각 실사풍 사진/영상에 이어진다. Road 는 새로 찍은 기록(카메라 도로) */
enum class SceneKind { LakeFog, Cable, Highway, Sunset, Rain, Meadow, Autumn, Road }

/** 갤러리에 흩뿌려 놓는 "순간" 카드 */
data class Moment(
    val scene: SceneKind,
    val seed: Int,
    val title: String,
    val date: String,
    val quote: String,
    val x: Int, val y: Int, val w: Int, val h: Int, val rot: Float,
    val badge: String? = null,
    val stamp: Int = 1,              // 0 끔 / 1 날짜 / 2 날짜+시각
    val caption: String? = null,     // 편집으로 바꾼 글귀
)

/** 오늘의 기록 한 칸. 앱을 처음 켜면 시나리오(귀가 중)의 5개가 들어 있고, 촬영하면 계속 늘어난다. */
data class TodayClip(
    val id: Int,
    val time: String,
    val place: String,       // 카드 제목 (수정 가능)
    val title: String,
    val detail: String,
    val tag: String,
    val color: Int,          // ARGB
    val scene: SceneKind,
    val seed: Int,
    val media: String? = null,   // 홈 기록 버튼으로 찍은 실제 파일(mp4/m4a) 경로. 없으면 일러스트 카드
)

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

    /** events 와 같은 순서의 장면 */
    val eventScenes = listOf(SceneKind.Meadow, SceneKind.Highway, SceneKind.LakeFog, SceneKind.Cable, SceneKind.Sunset)

    /**
     * 처음 시작할 때의 오늘의 기록 = "춘천 당일치기를 마치고 귀가 중"인 상황(15:20).
     * 한마디(12:45)와 귀갓길(15:20)까지 이미 쌓여 있어서 한마디·귀갓길 낭독을 바로 쓸 수 있다.
     */
    fun defaultClips(): List<TodayClip> = events.mapIndexed { i, e ->
        TodayClip(i, e.time, e.place, e.title, e.detail, e.tag, e.color.toArgb(), eventScenes[i], i)
    }

    val moments = listOf(
        Moment(SceneKind.LakeFog, 0, "안개가 먼저 와 있던 날", "2026.10.17", "안개등을 켠 의암호 길", 30, 40, 290, 400, -3f, "5s"),
        Moment(SceneKind.Highway, 1, "가평휴게소 18분", "2026.10.17", "문 네 개가 열렸다 닫혔어요", 330, 20, 230, 190, 4f),
        Moment(SceneKind.Cable, 2, "케이블카가 호수 위를 날았어", "2026.10.17", "“물고기가 우리 봤을까?”", 340, 250, 260, 350, 2.5f, "♪ 딸 목소리"),
        Moment(SceneKind.Sunset, 3, "집으로 가는 길", "2026.10.17", "집까지 1시간 35분", 650, 50, 330, 430, -2f),
        Moment(SceneKind.Meadow, 4, "양평 들판", "2026.05.05", "처음으로 맨발로 뛰었던 날", 1010, 20, 230, 230, -4f),
        Moment(SceneKind.Rain, 5, "비 오던 송도", "2026.07.12", "창문을 닫고 노래를 불렀어요", 1020, 280, 260, 330, 3f, "5s"),
        Moment(SceneKind.Autumn, 6, "3년 전 가을", "2023.10.28", "뒷창문 두 개가 동시에 내려갔어요", 1300, 70, 300, 400, 2f),
        Moment(SceneKind.Meadow, 7, "등원길 아침", "2026.09.02", "오늘은 손을 흔들어 줬어요", 1650, 220, 250, 340, -2.5f),
        Moment(SceneKind.Sunset, 8, "퇴근길 노을", "2026.09.20", "라디오에서 좋아하는 노래가 나왔어요", 1640, 20, 260, 190, 3f),
        Moment(SceneKind.Cable, 9, "첫 바다", "2025.08.03", "파도 소리를 처음 들은 날", 1950, 60, 300, 410, -3f, "♪ 딸 목소리"),
        Moment(SceneKind.Autumn, 10, "할머니 댁 가는 길", "2025.11.09", "감나무가 주황색이었어요", 2290, 170, 260, 350, 2.5f),
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
