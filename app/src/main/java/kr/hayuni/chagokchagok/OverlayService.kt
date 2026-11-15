package kr.hayuni.chagokchagok

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kr.hayuni.chagokchagok.data.VideoStore
import kotlin.math.abs

/**
 * 다른 앱을 쓰는 동안에도 화면 위에 떠 있는 "차곡차곡 기록 중" 동그라미.
 * 동그라미를 누르면 지금 화면(내비·음악 등 다른 앱 포함)을 5초 동안 영상으로 저장한다.
 * 화면 녹화(MediaProjection)를 쓰며 소리는 1차에서는 넣지 않았다. (소리는 이후 유지/삭제 결정)
 * 저장 위치는 영상 보관함(VideoStore)과 같다.
 */
class OverlayService : Service() {

    companion object {
        private const val ACTION_STOP = "kr.hayuni.chagokchagok.STOP_OVERLAY"
        private const val EXTRA_CODE = "code"
        private const val EXTRA_DATA = "data"
        private const val CHANNEL = "overlay"
        const val RECORD_MS = 5000L

        @Volatile
        var running = false
            private set

        fun start(ctx: Context, code: Int, data: Intent) {
            val i = Intent(ctx, OverlayService::class.java).putExtra(EXTRA_CODE, code).putExtra(EXTRA_DATA, data)
            ctx.startForegroundService(i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, OverlayService::class.java))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private var label: TextView? = null
    private var bubble: BubbleView? = null

    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var vw = 0
    private var vh = 0
    private var recording = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForeground()
        @Suppress("DEPRECATION")
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)
        val code = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        if (data == null || running) {
            if (!running) stopSelf()
            return START_NOT_STICKY
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        try {
            setupProjection(code, data)
            addBubble()
            running = true
        } catch (e: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "차곡차곡 기록", NotificationManager.IMPORTANCE_LOW))
        val stopPi = PendingIntent.getService(
            this, 1, Intent(this, OverlayService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        @Suppress("DEPRECATION")
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("차곡차곡 기록 중")
            .setContentText("동그라미를 누르면 지금 화면이 5초 동안 저장돼요")
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "중지", stopPi)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, n)
        }
    }

    private fun setupProjection(code: Int, data: Intent) {
        val mpm = getSystemService(MediaProjectionManager::class.java)
        val p = mpm.getMediaProjection(code, data)
        p.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { handler.post { stopSelf() } }
        }, handler)
        projection = p

        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        // 인코더 부담을 줄이려고 절반 해상도로 녹화 (짝수로 맞춤)
        vw = (dm.widthPixels / 2) / 2 * 2
        vh = (dm.heightPixels / 2) / 2 * 2
        // Android 14: 토큰당 VirtualDisplay 는 한 번만 만들고, 녹화할 때마다 surface 만 바꿔 끼운다.
        display = p.createVirtualDisplay(
            "chagok-record", vw, vh, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, null, null, handler,
        )
    }

    private fun addBubble() {
        val d = resources.displayMetrics.density
        val label = TextView(this).apply {
            text = "차곡차곡 기록 중"
            setTextColor(Color.WHITE)
            textSize = 20f
            setPadding((22 * d).toInt(), (10 * d).toInt(), (22 * d).toInt(), (10 * d).toInt())
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
            addView(bubble, LinearLayout.LayoutParams((112 * d).toInt(), (112 * d).toInt()).apply { topMargin = (10 * d).toInt() })
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
    }

    private fun record5s() {
        val disp = display ?: return
        if (recording) return
        recording = true
        val file = VideoStore.newClipFile(this)
        val rec = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            rec.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setOutputFile(file.absolutePath)
            rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            rec.setVideoSize(vw, vh)
            rec.setVideoFrameRate(30)
            rec.setVideoEncodingBitRate(6_000_000)
            rec.prepare()
            disp.surface = rec.surface
            rec.start()
        } catch (e: Exception) {
            rec.release()
            recording = false
            flash("기록하지 못했어요")
            return
        }

        val startAt = System.currentTimeMillis()
        bubble?.setRecording(true, 0f)
        val tick = object : Runnable {
            override fun run() {
                val el = System.currentTimeMillis() - startAt
                if (el >= RECORD_MS) {
                    try { rec.stop() } catch (_: Exception) { }
                    rec.release()
                    disp.surface = null
                    recording = false
                    bubble?.setRecording(false, 0f)
                    flash("저장됨 ✓  갤러리 > 영상 보관함")
                } else {
                    bubble?.setRecording(true, el / RECORD_MS.toFloat())
                    label?.text = "녹화 중 ${((RECORD_MS - el) / 1000) + 1}초"
                    handler.postDelayed(this, 80)
                }
            }
        }
        handler.post(tick)
    }

    private fun flash(msg: String) {
        label?.text = msg
        handler.postDelayed({ if (!recording) label?.text = "차곡차곡 기록 중" }, 2800)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        root?.let { runCatching { wm.removeView(it) } }
        runCatching { display?.release() }
        runCatching { projection?.stop() }
        running = false
        super.onDestroy()
    }

    /** 흰 테두리 + 빨간 점. 녹화 중에는 둘레에 진행 호가 돈다. */
    private class BubbleView(ctx: Context) : View(ctx) {
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.WHITE }
        private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE5574B.toInt() }
        private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0xFFF2B35E.toInt(); strokeCap = Paint.Cap.ROUND }
        private val back = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x990E151D.toInt() }
        private var rec = false
        private var progress = 0f

        fun setRecording(r: Boolean, p: Float) { rec = r; progress = p; invalidate() }

        override fun onDraw(c: Canvas) {
            val d = resources.displayMetrics.density
            val cx = width / 2f
            val cy = height / 2f
            val r = minOf(width, height) / 2f - 6 * d
            ring.strokeWidth = 6 * d
            arc.strokeWidth = 8 * d
            c.drawCircle(cx, cy, r + 4 * d, back)
            c.drawCircle(cx, cy, r, ring)
            if (rec) {
                val s = r * 0.5f
                c.drawRoundRect(RectF(cx - s, cy - s, cx + s, cy + s), 10 * d, 10 * d, dot)
                c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, 360f * progress, false, arc)
            } else {
                c.drawCircle(cx, cy, r * 0.62f, dot)
            }
        }
    }
}
