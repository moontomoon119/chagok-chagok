package kr.hayuni.chagokchagok

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kr.hayuni.chagokchagok.data.Prefs
import kr.hayuni.chagokchagok.ui.ChagokApp
import kr.hayuni.chagokchagok.ui.ChagokTheme

/**
 * 홈에서도 살아 있는 기록 버튼:
 *  - 앱을 켜면 오버레이 서비스를 시작해 두고(권한이 있을 때),
 *  - 앱이 화면에서 사라지면(홈으로 나가면) 기록 버튼이 떠 있고, 앱으로 돌아오면 숨긴다.
 */
class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startQuickRecord(prompt = false) }

    /** 앱이 다시 보일 때마다 올라가는 값: 홈 기록 버튼으로 찍은 기록을 오늘의 기록에 다시 읽어 오게 한다 */
    private var refreshTick by androidx.compose.runtime.mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChagokTheme {
                ChagokApp(
                    refreshTick = refreshTick,
                    onQuickRecordChange = ::applyQuickRecord,
                    onStartPressed = { if (Prefs.quickRecord(this)) startQuickRecord(prompt = true) },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshTick++
        OverlayService.setBubbleVisible(false)                       // 앱이 보이는 동안은 숨김
        if (Prefs.quickRecord(this)) startQuickRecord(prompt = false)   // 권한이 이미 있으면 조용히 시작
    }

    override fun onStop() {
        super.onStop()
        OverlayService.setBubbleVisible(true)                        // 홈으로 나가면 기록 버튼 표시
    }

    private fun granted(p: String) = checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    /** 설정의 "홈에서도 기록 버튼 유지" 스위치 */
    private fun applyQuickRecord(enabled: Boolean) {
        if (enabled) startQuickRecord(prompt = true) else OverlayService.stop(this)
    }

    /** 필요한 권한(다른 앱 위에 표시, 카메라, 마이크)이 갖춰지면 서비스를 시작한다. prompt=true 이면 권한을 요청한다. */
    private fun startQuickRecord(prompt: Boolean) {
        if (OverlayService.running) return
        if (!Settings.canDrawOverlays(this)) {
            if (prompt) {
                Toast.makeText(this, "홈에서도 기록하려면 '다른 앱 위에 표시'를 켜 주세요", Toast.LENGTH_LONG).show()
                runCatching {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
            }
            return
        }
        val missing = buildList {
            if (!granted(Manifest.permission.CAMERA)) add(Manifest.permission.CAMERA)
            if (!granted(Manifest.permission.RECORD_AUDIO)) add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33 && !granted(Manifest.permission.POST_NOTIFICATIONS)) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (prompt && missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
            return
        }
        // 카메라 또는 마이크 중 하나만 있어도 시작한다 (알림 권한은 없어도 됨)
        if (!granted(Manifest.permission.CAMERA) && !granted(Manifest.permission.RECORD_AUDIO)) return
        OverlayService.start(this)
    }
}
