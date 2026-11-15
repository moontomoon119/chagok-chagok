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
import kr.hayuni.chagokchagok.ui.ChagokApp
import kr.hayuni.chagokchagok.ui.ChagokTheme

class MainActivity : ComponentActivity() {

    // 카메라·마이크 권한 → 오버레이 서비스 시작 → 앱을 뒤로 보내 다른 앱을 쓸 수 있게 한다
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startOverlayIfPossible() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChagokTheme {
                ChagokApp(onBackgroundRecord = ::startBackgroundRecording)
            }
        }
    }

    private fun granted(p: String) = checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    private fun startBackgroundRecording() {
        if (OverlayService.running) {
            moveTaskToBack(true)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "다른 앱 위에 표시 권한을 켜 주세요", Toast.LENGTH_LONG).show()
            runCatching {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
            return
        }
        val need = buildList {
            if (!granted(Manifest.permission.CAMERA)) add(Manifest.permission.CAMERA)
            if (!granted(Manifest.permission.RECORD_AUDIO)) add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33 && !granted(Manifest.permission.POST_NOTIFICATIONS)) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (need.isEmpty()) startOverlayIfPossible() else permissionLauncher.launch(need.toTypedArray())
    }

    private fun startOverlayIfPossible() {
        if (!granted(Manifest.permission.CAMERA) && !granted(Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this, "카메라 또는 마이크 권한을 허용해야 기록할 수 있어요", Toast.LENGTH_LONG).show()
            return
        }
        OverlayService.start(this)
        Toast.makeText(this, "차곡차곡 기록 중 · 동그라미를 누르면 5초가 저장돼요", Toast.LENGTH_LONG).show()
        moveTaskToBack(true)
    }
}
