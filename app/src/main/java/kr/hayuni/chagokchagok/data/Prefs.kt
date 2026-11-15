package kr.hayuni.chagokchagok.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 앱을 나갔다 들어와도 남아야 하는 최소한의 상태를 SharedPreferences 에 저장한다.
 *  - 오늘의 기록 목록(제목 수정·삭제 포함, 날짜가 바뀌면 시나리오 기본값으로 다시)
 *  - 갤러리 편집 내용(타임스탬프·글귀·삭제)
 * 1차 데모라 서버 없이 기기 안에만 저장한다.
 */
object Prefs {
    private const val FILE = "chagok_state"
    private const val K_CLIPS = "today_clips_v1"
    private const val K_DAY = "today_day"
    private const val K_MOMENTS = "moments_v1"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private fun today() = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    /**
     * 오늘의 기록 목록. 저장된 게 없거나 날짜가 바뀌었으면 시나리오 기본값(귀가 중)으로 시작한다.
     */
    fun loadClips(ctx: Context): List<TodayClip> {
        val p = sp(ctx)
        val raw = p.getString(K_CLIPS, null)
        if (raw == null || p.getString(K_DAY, null) != today()) return Scenario.defaultClips()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                TodayClip(
                    o.getInt("id"), o.getString("time"), o.getString("place"), o.getString("title"),
                    o.getString("detail"), o.getString("tag"), o.getInt("color"),
                    SceneKind.valueOf(o.getString("scene")), o.getInt("seed"),
                    if (o.has("media") && !o.isNull("media")) o.getString("media") else null,
                )
            }
        }.getOrDefault(Scenario.defaultClips())
    }

    fun saveClips(ctx: Context, list: List<TodayClip>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject().put("id", c.id).put("time", c.time).put("place", c.place).put("title", c.title)
                    .put("detail", c.detail).put("tag", c.tag).put("color", c.color)
                    .put("scene", c.scene.name).put("seed", c.seed).put("media", c.media ?: JSONObject.NULL),
            )
        }
        // commit(): 오버레이 서비스가 저장한 직후 앱이 바로 읽어도 보이도록 동기 저장
        sp(ctx).edit().putString(K_DAY, today()).putString(K_CLIPS, arr.toString()).commit()
    }

    /** 홈 기록 버튼으로 찍은 5초 파일을 "오늘의 기록" 맨 뒤에 한 칸으로 등록한다 (앱이 꺼져 있어도 동작) */
    @Synchronized
    fun addRecordedClip(ctx: Context, file: java.io.File, hasVideo: Boolean) {
        val list = loadClips(ctx)
        val id = (list.maxOfOrNull { it.id } ?: -1) + 1
        val scenes = SceneKind.values()
        val clip = TodayClip(
            id, SimpleDateFormat("HH:mm", Locale.KOREA).format(Date()), "방금 이 길",
            if (hasVideo) "5초 기록" else "5초 소리 기록",
            if (hasVideo) "홈 기록 버튼으로 남긴 순간이에요. 눌러서 다시 보고 제목을 바꿔 보세요."
            else "홈 기록 버튼으로 차 안의 소리를 남겼어요. 눌러서 다시 듣고 제목을 바꿔 보세요.",
            "방금", 0xFF6FCFB3.toInt(), scenes[id % scenes.size], id, file.absolutePath,
        )
        saveClips(ctx, list + clip)
    }

    /** 갤러리 카드: 저장된 편집이 없으면 기본 목록, 있으면 삭제/편집을 반영 */
    fun loadMoments(ctx: Context): List<Moment> {
        val raw = sp(ctx).getString(K_MOMENTS, null) ?: return Scenario.moments
        return runCatching {
            val arr = JSONArray(raw)
            val saved = HashMap<Int, JSONObject>()
            for (i in 0 until arr.length()) arr.getJSONObject(i).let { saved[it.getInt("seed")] = it }
            Scenario.moments.filter { it.seed in saved }.map { m ->
                val o = saved.getValue(m.seed)
                m.copy(
                    title = o.optString("title", m.title),
                    stamp = o.optInt("stamp", m.stamp),
                    caption = if (o.has("caption") && !o.isNull("caption")) o.getString("caption") else null,
                )
            }
        }.getOrDefault(Scenario.moments)
    }

    fun saveMoments(ctx: Context, list: List<Moment>) {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(JSONObject().put("seed", m.seed).put("stamp", m.stamp).put("title", m.title).put("caption", m.caption ?: JSONObject.NULL))
        }
        sp(ctx).edit().putString(K_MOMENTS, arr.toString()).apply()
    }

    /** 홈으로 나가도 떠 있는 기록 버튼(오버레이)을 쓸지. 기본은 켜짐 */
    fun quickRecord(ctx: Context): Boolean = sp(ctx).getBoolean("quick_record", true)

    fun setQuickRecord(ctx: Context, on: Boolean) {
        sp(ctx).edit().putBoolean("quick_record", on).apply()
    }

    /** 목업(데모) 환경으로 되돌리기: 저장된 오늘의 기록·갤러리 편집을 모두 지운다 (다음에 읽을 때 기본값으로 시작) */
    fun resetAll(ctx: Context) {
        sp(ctx).edit().clear().apply()
    }
}
