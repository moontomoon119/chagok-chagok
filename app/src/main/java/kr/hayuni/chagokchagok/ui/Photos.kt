package kr.hayuni.chagokchagok.ui

import android.net.Uri
import android.widget.VideoView
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import kr.hayuni.chagokchagok.R
import kr.hayuni.chagokchagok.data.SceneKind

/**
 * 앱에 들어가는 실사풍 장면 사진/영상 (AI 생성 이미지 + 고정 확대 영상).
 * 사진은 res/drawable-nodpi, 3초 영상은 res/raw 에 있다. (실제 장소를 정확히 기록한 사진이 아님)
 */
class Pic(@DrawableRes val img: Int, @RawRes val vid: Int?, val aspect: Float)

object Photos {
    private const val TW = 1080f / 2386f   // 오늘의 기록: 세로 숏폼
    private const val GAPYEONG = 720f / 768f   // 가평휴게소: 직접 찍은 실제 사진(오늘의 기록·갤러리 공용)

    /** 오늘의 기록 5장 (시나리오 순서: 우리 집 → 가평휴게소 → 의암호 길 → 춘천 케이블카 → 노을). 6번째 "집 가는 중"은 카메라 도로(Road) */
    val today = listOf(
        Pic(R.drawable.today_0, R.raw.today_0, 4f / 3f),
        Pic(R.drawable.today_1, R.raw.today_1, GAPYEONG),
        Pic(R.drawable.today_2, R.raw.today_2, TW),
        Pic(R.drawable.today_3, R.raw.today_3, TW),
        Pic(R.drawable.today_4, R.raw.today_4, 1f),
    )

    /** 갤러리 11장 (Scenario.moments 순서와 같음) */
    val gallery = listOf(
        Pic(R.drawable.gallery_0, R.raw.gallery_0, 1080f / 1490f),
        Pic(R.drawable.gallery_1, R.raw.gallery_1, GAPYEONG),
        Pic(R.drawable.gallery_2, R.raw.gallery_2, 1080f / 1454f),
        Pic(R.drawable.gallery_3, R.raw.gallery_3, 1080f / 1408f),
        Pic(R.drawable.gallery_4, R.raw.gallery_4, 1f),
        Pic(R.drawable.gallery_5, R.raw.gallery_5, 1080f / 1370f),
        Pic(R.drawable.gallery_6, R.raw.gallery_6, 1080f / 1440f),
        Pic(R.drawable.gallery_7, R.raw.gallery_7, 1080f / 1468f),
        Pic(R.drawable.gallery_8, R.raw.gallery_8, 1080f / 790f),
        Pic(R.drawable.gallery_9, R.raw.gallery_9, 1080f / 1476f),
        Pic(R.drawable.gallery_10, R.raw.gallery_10, 1080f / 1454f),
    )

    val start = Pic(R.drawable.start_bg, null, 16f / 9f)
    val camera = Pic(R.drawable.camera_road, null, 16f / 9f)

    /** 장면 종류 → 사진. 시나리오 5개는 각자 사진이 있고, 새로 찍은 기록(Road)은 카메라 도로 사진 */
    fun of(kind: SceneKind): Pic = when (kind) {
        SceneKind.Meadow -> today[0]
        SceneKind.Highway -> today[1]
        SceneKind.LakeFog -> today[2]
        SceneKind.Cable -> today[3]
        SceneKind.Sunset -> today[4]
        SceneKind.Rain -> gallery[5]
        SceneKind.Autumn -> gallery[6]
        SceneKind.Road -> camera
    }

    fun gallery(i: Int): Pic = gallery[i.coerceIn(0, gallery.lastIndex)]
}

/** 사진을 영역에 꽉 채워(가운데 기준 자르기) 보여 준다 */
@Composable
fun PhotoArt(pic: Pic, modifier: Modifier = Modifier) {
    Image(painterResource(pic.img), null, modifier.clipToBounds(), contentScale = ContentScale.Crop)
}

/**
 * 3초 영상을 영역에 꽉 채워 반복 재생. 영상이 뜨기 전에는 같은 사진을 깔아 둬서 까만 화면이 보이지 않는다.
 * VideoView 를 영상 비율대로 키운 뒤 가운데를 보여 주는 방식이라 위아래/좌우가 잘린다(center-crop).
 */
@Composable
fun PhotoVideo(pic: Pic, modifier: Modifier = Modifier, muted: Boolean = true) {
    val vid = pic.vid
    if (vid == null) { PhotoArt(pic, modifier); return }
    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val w = maxOf(maxWidth, maxHeight * pic.aspect)
        val h = maxOf(maxHeight, maxWidth / pic.aspect)
        Image(painterResource(pic.img), null, Modifier.requiredSize(w, h), contentScale = ContentScale.Crop)
        AndroidView(
            modifier = Modifier.requiredSize(w, h),
            factory = { c ->
                VideoView(c).apply {
                    setVideoURI(Uri.parse("android.resource://${c.packageName}/$vid"))
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        if (muted) mp.setVolume(0f, 0f)
                        start()
                    }
                }
            },
        )
    }
}
