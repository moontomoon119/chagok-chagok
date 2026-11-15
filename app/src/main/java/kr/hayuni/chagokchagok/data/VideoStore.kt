package kr.hayuni.chagokchagok.data

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "동영상 저장소" (1차 시안에서는 앱 전용 Movies 폴더로 가정).
 *  - 팀이 준비한 저작권 프리 도로 영상을 이 폴더에 넣으면 갤러리 "영상 보관함"에 나타난다.
 *  - `road.mp4` 가 있으면 카메라 화면 배경으로 반복 재생된다.
 *  - 오버레이 동그라미로 찍은 5초 기록도 같은 폴더에 저장된다.
 *  경로: Android/data/kr.hayuni.chagokchagok/files/Movies
 */
object VideoStore {
    private val exts = setOf("mp4", "mov", "webm", "mkv", "3gp")

    fun dir(ctx: Context): File =
        (ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: File(ctx.filesDir, "movies")).also { it.mkdirs() }

    /** 차량(AAOS)은 다중 사용자라 외부 저장소에 파일을 넣기 어려워 앱 내부 폴더(files/movies)도 함께 본다. */
    fun internalDir(ctx: Context): File = File(ctx.filesDir, "movies").also { it.mkdirs() }

    fun list(ctx: Context): List<File> =
        listOf(dir(ctx), internalDir(ctx))
            .flatMap { d -> d.listFiles { f -> f.isFile && f.extension.lowercase() in exts }?.toList() ?: emptyList() }
            .distinctBy { it.absolutePath }
            .sortedByDescending { it.lastModified() }

    fun roadVideo(ctx: Context): File? =
        list(ctx).firstOrNull { it.nameWithoutExtension.equals("road", ignoreCase = true) }

    fun newClipFile(ctx: Context): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir(ctx), "clip_$stamp.mp4")
    }
}
