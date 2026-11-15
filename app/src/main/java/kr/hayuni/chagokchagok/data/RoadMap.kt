package kr.hayuni.chagokchagok.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.DataInputStream
import kotlin.math.cos

/**
 * 도로망 지도 데이터 (assets/roadmap.bin + roadmap_meta.json).
 * 원본: chagok_road_memory_map.geojson (KTDB 2022 도로 링크, 서울·인천·경기·춘천 주변 시연용).
 * 앱에서 빠르게 읽도록 변환했다. 좌표는 "동쪽 +x, 남쪽 +y" 의 평면 단위(경도 보정 포함)로 바꿔 Path 로 만든다.
 */
class MapRoute(
    val id: String,
    val label: String,       // 춘천의 기억
    val tag: String,         // 유치원 시절
    val color: Color,
    val km: Double,
    val stops: List<String>,
    val path: Path,
) {
    val shortName: String get() = label.removeSuffix("의 기억")
}

class MapPlace(val id: String, val label: String, val color: Color, val x: Float, val y: Float)

class RoadMapData(
    /** 도로 등급(1=고속·간선 … 5=소로)별 Path. 없는 등급은 null */
    val roads: Map<Int, Path>,
    val routes: List<MapRoute>,
    val places: List<MapPlace>,
    val width: Float,
    val height: Float,
    private val minLon: Double,
    private val maxLat: Double,
    private val k: Double,
) {
    fun x(lon: Double) = ((lon - minLon) * k).toFloat()
    fun y(lat: Double) = (maxLat - lat).toFloat()
}

object RoadMapLoader {
    fun load(ctx: Context): RoadMapData {
        val meta = JSONObject(ctx.assets.open("roadmap_meta.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
        val bb = meta.getJSONArray("bbox")
        val minLon = bb.getDouble(0)
        val minLat = bb.getDouble(1)
        val maxLon = bb.getDouble(2)
        val maxLat = bb.getDouble(3)
        val k = cos(Math.toRadians((minLat + maxLat) / 2))
        val width = ((maxLon - minLon) * k).toFloat()
        val height = (maxLat - minLat).toFloat()

        // 도로
        val roads = HashMap<Int, Path>()
        DataInputStream(BufferedInputStream(ctx.assets.open("roadmap.bin"), 1 shl 16)).use { s ->
            val nRanks = s.readUnsignedByte()
            val lon0 = s.readInt() / 1e5
            val lat0 = s.readInt() / 1e5
            repeat(nRanks) {
                val rank = s.readUnsignedByte()
                val count = s.readInt()
                val path = Path()
                repeat(count) {
                    val n = s.readUnsignedShort()
                    for (i in 0 until n) {
                        val lon = lon0 + s.readInt() / 1e5
                        val lat = lat0 + s.readInt() / 1e5
                        val px = ((lon - minLon) * k).toFloat()
                        val py = (maxLat - lat).toFloat()
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                }
                roads[rank] = path
            }
        }

        // 경로
        val routes = ArrayList<MapRoute>()
        val ra = meta.getJSONArray("routes")
        for (i in 0 until ra.length()) {
            val r = ra.getJSONObject(i)
            val pts = r.getJSONArray("pts")
            val path = Path()
            for (j in 0 until pts.length()) {
                val p = pts.getJSONArray(j)
                val px = ((p.getDouble(0) - minLon) * k).toFloat()
                val py = (maxLat - p.getDouble(1)).toFloat()
                if (j == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            val stops = r.getJSONArray("stops").let { a -> List(a.length()) { a.getString(it) } }
            routes += MapRoute(
                r.getString("id"), r.getString("label"), r.getString("tag"),
                Color(android.graphics.Color.parseColor(r.getString("color"))),
                r.getDouble("km"), stops, path,
            )
        }

        // 장소
        val places = ArrayList<MapPlace>()
        val pa = meta.getJSONArray("places")
        for (i in 0 until pa.length()) {
            val p = pa.getJSONObject(i)
            places += MapPlace(
                p.getString("id"), p.getString("label"),
                Color(android.graphics.Color.parseColor(p.getString("color"))),
                ((p.getDouble("lon") - minLon) * k).toFloat(), (maxLat - p.getDouble("lat")).toFloat(),
            )
        }
        return RoadMapData(roads, routes, places, width, height, minLon, maxLat, k)
    }
}
