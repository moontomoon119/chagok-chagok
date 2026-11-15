package kr.hayuni.chagokchagok

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kr.hayuni.chagokchagok.data.SceneKind
import kr.hayuni.chagokchagok.data.Scenario
import kr.hayuni.chagokchagok.ui.RoadViewfinder
import kr.hayuni.chagokchagok.ui.SceneArt
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * [디버그 전용] 앱 속 일러스트를 UI(글자·카드·그라데이션) 없이 이미지로 내보낸다.
 * 앱의 `SceneArt` / `RoadViewfinder` 를 그대로 그리므로 앱에서 보이는 일러스트와 똑같다.
 *
 * 실행: adb shell am start -n kr.hayuni.chagokchagok/.ExportActivity
 * 결과: files/export/<폴더>/<파일명>.png  (로그 태그 EXPORT 에 DONE 이 찍히면 끝)
 */
class ExportActivity : ComponentActivity() {

    private class Job(val folder: String, val name: String, val w: Int, val h: Int, val kind: SceneKind?, val seed: Int)

    private fun even(v: Double) = (v / 2.0).roundToInt() * 2

    private fun safe(s: String) = s.replace(Regex("[\\\\/:*?\"<>|]"), "").trim()

    private fun buildJobs(): List<Job> {
        val jobs = ArrayList<Job>()
        // 오늘의 기록 카드와 같은 세로 비율 (카드 420 x 928)
        Scenario.defaultClips().forEachIndexed { i, c ->
            jobs += Job("오늘의기록", "%02d_%s".format(i + 1, safe(c.place)), 1080, even(1080.0 * 928 / 420), c.scene, c.seed)
        }
        // 갤러리 카드와 같은 비율 (카드마다 다름)
        Scenario.moments.forEachIndexed { i, m ->
            jobs += Job("갤러리", "%02d_%s".format(i + 1, safe(m.title)), 1080, even(1080.0 * m.h / m.w), m.scene, m.seed)
        }
        // 기타: 시작 화면 배경, 카메라 화면의 도로 (가로 16:9)
        jobs += Job("기타", "시작화면_배경_안개낀호수", 1920, 1080, SceneKind.LakeFog, 0)
        jobs += Job("기타", "카메라화면_도로", 1920, 1080, null, 0)
        return jobs
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = File(filesDir, "export").apply { deleteRecursively(); mkdirs() }
        setContent {
            val jobs = remember { buildJobs() }
            var idx by remember { mutableIntStateOf(0) }
            val layer = rememberGraphicsLayer()
            val density = LocalDensity.current
            val job = jobs.getOrNull(idx)

            Box(Modifier.fillMaxSize().background(Color.Black)) {
                if (job != null) {
                    key(idx) {
                        Box(
                            Modifier
                                .requiredSize(with(density) { job.w.toDp() }, with(density) { job.h.toDp() })
                                .drawWithContent {
                                    layer.record { this@drawWithContent.drawContent() }
                                    drawLayer(layer)
                                },
                        ) {
                            Scene(job)
                        }
                    }
                }
            }

            LaunchedEffect(idx) {
                if (job == null) {
                    Log.i("EXPORT", "DONE ${jobs.size}")
                    finish()
                    return@LaunchedEffect
                }
                delay(700) // 그려질 때까지 잠깐 기다림 (카메라 도로 등 애니메이션 한 프레임)
                runCatching {
                    val bmp = layer.toImageBitmap().asAndroidBitmap()
                    val dir = File(root, job.folder).apply { mkdirs() }
                    FileOutputStream(File(dir, job.name + ".png")).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                    Log.i("EXPORT", "saved ${job.folder}/${job.name} ${bmp.width}x${bmp.height}")
                }.onFailure { Log.e("EXPORT", "fail ${job.name}: $it") }
                idx++
            }
        }
    }

    @Composable
    private fun Scene(job: Job) {
        if (job.kind == null) RoadViewfinder(Modifier.fillMaxSize()) else SceneArt(job.kind, Modifier.fillMaxSize(), job.seed)
    }
}
