package kr.hayuni.chagokchagok

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Size
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kr.hayuni.chagokchagok.data.Prefs
import kr.hayuni.chagokchagok.data.VideoStore
import java.io.File
import kotlin.math.abs

/**
 * 다른 앱을 쓰는 동안에도 화면 위에 떠 있는 "눌러서 5초 기록" 동그라미.
 * 동그라미를 누르면 **카메라 영상 + 차 안의 소리(마이크)** 를 5초 동안 mp4 로 저장한다.
 *  - 카메라가 없는 기기(예: 일부 차량 에뮬레이터)에서는 소리만 m4a 로 저장한다.
 *  - 저장 위치는 영상 보관함(VideoStore)과 같다.
 * 소리는 일단 포함해 두었고, 이후 유지/삭제를 정한다.(RECORD_WITH_SOUND)
 */
class OverlayService : Service() {

    companion object {
        private const val ACTION_STOP = "kr.hayuni.chagokchagok.STOP_OVERLAY"
        private const val CHANNEL = "overlay"
        const val RECORD_MS = 5000L

        /** 소리를 함께 기록할지 (이후 유지/삭제 결정) */
        const val RECORD_WITH_SOUND = true

        @Volatile
        var running = false
            private set

        @Volatile
        private var starting = false

        @Volatile
        private var instance: OverlayService? = null

        /** 앱이 화면에 보이면 false(숨김), 앱에서 나가면 true(표시) */
        @Volatile
        private var wantVisible = false

        fun start(ctx: Context) {
            if (running || starting) return
            starting = true
            ctx.startForegroundService(Intent(ctx, OverlayService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, OverlayService::class.java))
        }

        /** 앱이 보이는 동안은 숨기고, 홈으로 나가면 기록 버튼을 띄운다 */
        fun setBubbleVisible(v: Boolean) {
            wantVisible = v
            instance?.let { svc -> svc.handler.post { svc.applyVisible() } }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val camThread = HandlerThread("chagok-camera").apply { start() }
    private val camHandler by lazy { Handler(camThread.looper) }

    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private var label: TextView? = null
    private var bubble: BubbleView? = null

    private var cameraId: String? = null
    private var sensorOrientation = 0
    private var videoSize = Size(1280, 720)
    private var hasMic = false
    private var recording = false

    private var activeRecorder: MediaRecorder? = null
    private var activeCamera: CameraDevice? = null
    private var activeSession: CameraCaptureSession? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Prefs.setQuickRecord(this, false)   // 알림의 "끄기": 기능을 끈 것으로 저장
            stopSelf()
            return START_NOT_STICKY
        }
        if (running) return START_NOT_STICKY

        val hasCamPerm = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasMic = RECORD_WITH_SOUND && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        cameraId = if (hasCamPerm) pickCamera() else null

        var type = 0
        if (Build.VERSION.SDK_INT >= 30) {
            if (cameraId != null) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            if (hasMic) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }
        startAsForeground(type)
        if (cameraId == null && !hasMic) {
            starting = false
            stopSelf()
            return START_NOT_STICKY
        }

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        try {
            addBubble()
            instance = this
            running = true
            applyVisible()
        } catch (e: Exception) {
            stopSelf()
        }
        starting = false
        return START_NOT_STICKY
    }

    private fun startAsForeground(type: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "차곡차곡 기록", NotificationManager.IMPORTANCE_LOW))
        val stopPi = PendingIntent.getService(
            this, 1, Intent(this, OverlayService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        @Suppress("DEPRECATION")
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("차곡차곡 기록 버튼 켜짐")
            .setContentText("홈으로 나가면 기록 버튼이 떠요 · 누르면 카메라와 소리가 5초 저장돼요")
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "끄기", stopPi)
            .build()
        if (Build.VERSION.SDK_INT >= 29 && type != 0) startForeground(1, n, type) else startForeground(1, n)
    }

    /** 뒷면 카메라 우선, 없으면 첫 번째 카메라. 카메라가 없으면 null (소리만 기록) */
    @SuppressLint("MissingPermission")
    private fun pickCamera(): String? = runCatching {
        val cm = getSystemService(CameraManager::class.java)
        val ids = cm.cameraIdList
        val id = ids.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK }
            ?: ids.firstOrNull()
        if (id != null) {
            val ch = cm.getCameraCharacteristics(id)
            sensorOrientation = ch.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            val sizes = ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.getOutputSizes(MediaRecorder::class.java)?.toList().orEmpty()
            videoSize = sizes.filter { it.width <= 1280 }.maxByOrNull { it.width * it.height }
                ?: sizes.minByOrNull { it.width * it.height } ?: videoSize
        }
        id
    }.getOrNull()

    private fun addBubble() {
        val d = resources.displayMetrics.density
        val label = TextView(this).apply {
            text = "눌러서 5초 기록"
            setTextColor(Color.WHITE)
            textSize = 17f
            setPadding((18 * d).toInt(), (8 * d).toInt(), (18 * d).toInt(), (8 * d).toInt())
            background = GradientDrawable().apply {
                cornerRadius = 100f * d
                setColor(0xE60E151D.toInt())
            }
        }
        val bubble = BubbleView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(label, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(bubble, LinearLayout.LayoutParams((92 * d).toInt(), (92 * d).toInt()).apply { topMargin = (8 * d).toInt() })
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (60 * d).toInt()
            y = (260 * d).toInt()
        }

        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        box.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = lp.x; startY = lp.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (abs(dx) > 12 * d || abs(dy) > 12 * d) moved = true
                    if (moved) { lp.x = (startX + dx).toInt(); lp.y = (startY + dy).toInt(); wm.updateViewLayout(box, lp) }
                    true
                }
                MotionEvent.ACTION_UP -> { if (!moved) record5s(); true }
                else -> false
            }
        }
        wm.addView(box, lp)
        this.root = box
        this.label = label
        this.bubble = bubble
        label.text = idleLabel()
    }

    private fun idleLabel() = if (hasMic) "눌러서 5초 기록" else "눌러서 5초 기록 · 소리 꺼짐"

    // ───────────────────────── 5초 기록 ─────────────────────────

    private fun record5s() {
        if (recording) return
        recording = true
        // 항상 영상(mp4)으로 저장한다. 카메라가 없는 기기(Pleos 에뮬레이터 등)에서는
        // 카메라 대신 앞유리 풍경 화면을 그려 넣은 영상 + 마이크 소리로 기록한다.
        val file = VideoStore.newClipFile(this)
        val rec = buildRecorder(file, withVideo = true)
        if (rec == null) { fail(file, "기록하지 못했어요"); return }
        activeRecorder = rec
        if (cameraId != null) openCameraAndRecord(rec, file) else {
            try {
                val surface = rec.surface
                rec.start()
                startTimer(file)
                drawSyntheticFrames(surface, System.currentTimeMillis())
            } catch (e: Exception) { fail(file, "기록하지 못했어요"); return }
        }
    }

    /** 카메라가 없을 때: 녹화 surface 에 앞유리 풍경(흐르는 도로)을 30fps 로 그려 영상 프레임으로 쓴다 */
    private fun drawSyntheticFrames(surface: android.view.Surface, startAt: Long) {
        val w = videoSize.width.toFloat()
        val h = videoSize.height.toFloat()
        val horizon = h * 0.42f
        val sky = Paint().apply {
            shader = android.graphics.LinearGradient(0f, 0f, 0f, horizon, 0xFF3F86DA.toInt(), 0xFFC4E3F7.toInt(), android.graphics.Shader.TileMode.CLAMP)
        }
        val hill = Paint().apply { color = 0xFF82A892.toInt(); isAntiAlias = true }
        val grass = Paint().apply { color = 0xFF5E9F58.toInt() }
        val road = Paint().apply { color = 0xFF4A515B.toInt(); isAntiAlias = true }
        val dash = Paint().apply { color = 0xFFF5F0E0.toInt(); isAntiAlias = true }
        val tree = Paint().apply { color = 0xFF3F7D4E.toInt(); isAntiAlias = true }
        val path = android.graphics.Path()
        val cx = w / 2f
        val frame = object : Runnable {
            override fun run() {
                if (!recording || activeRecorder == null) return
                val t = (System.currentTimeMillis() - startAt) / 1000f
                try {
                    val c = surface.lockCanvas(null)
                    c.drawRect(0f, 0f, w, horizon, sky)
                    c.drawCircle(w * 0.78f, horizon * 0.55f, h * 0.07f, Paint().apply { color = 0xFFFFE4AE.toInt(); isAntiAlias = true })
                    path.reset(); path.moveTo(0f, horizon)
                    path.cubicTo(w * 0.2f, horizon - h * 0.12f, w * 0.4f, horizon - h * 0.02f, w * 0.6f, horizon - h * 0.09f)
                    path.cubicTo(w * 0.8f, horizon - h * 0.14f, w * 0.9f, horizon - h * 0.04f, w, horizon - h * 0.06f)
                    path.lineTo(w, horizon); path.close()
                    c.drawPath(path, hill)
                    c.drawRect(0f, horizon, w, h, grass)
                    path.reset(); path.moveTo(cx - w * 0.012f, horizon); path.lineTo(cx + w * 0.012f, horizon)
                    path.lineTo(w * 0.9f, h); path.lineTo(w * 0.1f, h); path.close()
                    c.drawPath(path, road)
                    for (k in 0 until 9) {
                        val z = ((k + t * 2.2f) % 9f) / 9f           // 0(멀리) → 1(가까이)
                        val p = z * z
                        val y = horizon + (h - horizon) * p
                        val half = (w * 0.012f) + (w * 0.012f) * p * 3f
                        val len = (h - horizon) * 0.07f * (0.3f + p)
                        c.drawRect(cx - half, y, cx + half, y + len, dash)
                        for (side in intArrayOf(-1, 1)) {      // 길가의 나무
                            val tx = cx + side * (w * 0.06f + (w * 0.46f) * p)
                            val tr = h * 0.012f + h * 0.11f * p
                            c.drawCircle(tx, y - tr * 1.2f, tr, tree)
                        }
                    }
                    surface.unlockCanvasAndPost(c)
                } catch (e: Exception) {
                    return
                }
                camHandler.postDelayed(this, 33)
            }
        }
        camHandler.post(frame)
    }

    private fun buildRecorder(file: File, withVideo: Boolean): MediaRecorder? {
        val rec = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            if (hasMic) rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            if (withVideo) rec.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            if (withVideo) {
                rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                rec.setVideoSize(videoSize.width, videoSize.height)
                rec.setVideoFrameRate(30)
                rec.setVideoEncodingBitRate(8_000_000)
                rec.setOrientationHint(sensorOrientation)
            }
            if (hasMic) {
                rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                rec.setAudioEncodingBitRate(128_000)
                rec.setAudioSamplingRate(44_100)
            }
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec
        } catch (e: Exception) {
            rec.release()
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCameraAndRecord(rec: MediaRecorder, file: File) {
        val id = cameraId ?: return fail(file, "카메라를 열지 못했어요")
        val cm = getSystemService(CameraManager::class.java)
        try {
            cm.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    activeCamera = camera
                    try {
                        val surface = rec.surface
                        @Suppress("DEPRECATION")
                        camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                activeSession = session
                                try {
                                    val req = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply { addTarget(surface) }.build()
                                    session.setRepeatingRequest(req, null, camHandler)
                                    rec.start()
                                    handler.post { startTimer(file) }
                                } catch (e: Exception) {
                                    handler.post { fail(file, "기록하지 못했어요") }
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                handler.post { fail(file, "카메라 설정에 실패했어요") }
                            }
                        }, camHandler)
                    } catch (e: Exception) {
                        handler.post { fail(file, "기록하지 못했어요") }
                    }
                }

                override fun onDisconnected(camera: CameraDevice) { camera.close() }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    handler.post { fail(file, "카메라를 열지 못했어요") }
                }
            }, camHandler)
        } catch (e: Exception) {
            fail(file, "카메라를 열지 못했어요")
        }
    }

    private fun startTimer(file: File) {
        val startAt = System.currentTimeMillis()
        bubble?.setRecording(true, 0f)
        val tick = object : Runnable {
            override fun run() {
                if (!recording) return
                val el = System.currentTimeMillis() - startAt
                if (el >= RECORD_MS) {
                    finishRecording(file)
                } else {
                    bubble?.setRecording(true, el / RECORD_MS.toFloat())
                    label?.text = "녹화 중 ${((RECORD_MS - el) / 1000) + 1}초"
                    handler.postDelayed(this, 80)
                }
            }
        }
        handler.post(tick)
    }

    private fun finishRecording(file: File) {
        releaseRecording(stopRecorder = true)
        bubble?.setRecording(false, 0f)
        recording = false
        runCatching { Prefs.addRecordedClip(this, file, hasVideo = true) }
        flash("저장됨 ✓ 오늘의 기록에 담았어요")
    }

    private fun fail(file: File, msg: String) {
        releaseRecording(stopRecorder = false)
        runCatching { file.delete() }
        bubble?.setRecording(false, 0f)
        recording = false
        flash(msg)
    }

    private fun releaseRecording(stopRecorder: Boolean) {
        val rec = activeRecorder
        activeRecorder = null
        if (rec != null) {
            if (stopRecorder) runCatching { rec.stop() }
            runCatching { rec.release() }
        }
        runCatching { activeSession?.close() }
        runCatching { activeCamera?.close() }
        activeSession = null
        activeCamera = null
    }

    private fun flash(msg: String) {
        label?.text = msg
        handler.postDelayed({
            if (!recording) label?.text = idleLabel()
        }, 2800)
    }

    private fun applyVisible() {
        root?.visibility = if (wantVisible) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        instance = null
        starting = false
        handler.removeCallbacksAndMessages(null)
        recording = false
        releaseRecording(stopRecorder = false)
        root?.let { runCatching { wm.removeView(it) } }
        camThread.quitSafely()
        running = false
        super.onDestroy()
    }

    /** 앱 로고와 같은 12칸 셔터 링 + 가운데 원. 녹화 중에는 칸이 하나씩 채워지고 가운데가 빨간 사각형이 된다. */
    private class BubbleView(ctx: Context) : View(ctx) {
        private val seg = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val core = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF4EFE6.toInt() }
        private val stop = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE5574B.toInt() }
        private val back = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE6101A24.toInt() }
        private val colors = intArrayOf(
            0xFFFFFFFF.toInt(), 0xFFFDF6EB.toInt(), 0xFFFCECD7.toInt(), 0xFFFAE2C3.toInt(), 0xFFF8D9AE.toInt(),
            0xFFF7CF9A.toInt(), 0xFFF5C686.toInt(), 0xFFF4BD72.toInt(), 0xFFF2B35E.toInt(),
        )
        private val slate = 0xFF3A4756.toInt()
        private var rec = false
        private var progress = 0f

        fun setRecording(r: Boolean, p: Float) { rec = r; progress = p; invalidate() }

        override fun onDraw(c: Canvas) {
            val d = resources.displayMetrics.density
            val cx = width / 2f
            val cy = height / 2f
            val outer = minOf(width, height) / 2f
            val r = outer - 12 * d
            seg.strokeWidth = 8 * d
            c.drawCircle(cx, cy, outer - 1 * d, back)
            val box = RectF(cx - r, cy - r, cx + r, cy + r)
            val filled = if (rec) 1 + (progress * 11f).toInt() else 9
            for (i in 0 until 12) {
                seg.color = if (i < filled && i < colors.size) colors[i] else if (rec && i < filled) colors[colors.size - 1] else slate
                c.drawArc(box, -90f + 30f * i + 3f, 24f, false, seg)
            }
            if (rec) {
                val s = r * 0.46f
                c.drawRoundRect(RectF(cx - s, cy - s, cx + s, cy + s), 8 * d, 8 * d, stop)
            } else {
                c.drawCircle(cx, cy, r * 0.56f, core)
            }
        }
    }
}
