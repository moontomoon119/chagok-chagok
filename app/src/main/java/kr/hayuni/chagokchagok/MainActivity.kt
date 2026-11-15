package kr.hayuni.chagokchagok

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
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

    // 화면 녹화 동의 결과 → 오버레이 서비스 시작 → 앱을 뒤로 보내 다른 앱을 쓸 수 있게 한다
    private val projectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val data = r.data
            if (r.resultCode == RESULT_OK && data != null) {
                OverlayService.start(this, r.resultCode, data)
                Toast.makeText(this, "차곡차곡 기록 중 · 동그라미를 누르면 5초가 저장돼요", Toast.LENGTH_LONG).show()
                moveTaskToBack(true)
            } else {
                Toast.makeText(this, "화면 녹화를 허용해야 기록할 수 있어요", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChagokTheme {
                ChagokApp(onBackgroundRecord = ::startBackgroundRecording)
            }
        }
    }

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
        val mpm = getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(mpm.createScreenCaptureIntent())
    }
}
