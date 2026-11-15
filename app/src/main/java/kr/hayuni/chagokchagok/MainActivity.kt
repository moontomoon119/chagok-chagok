package kr.hayuni.chagokchagok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kr.hayuni.chagokchagok.ui.ChagokApp
import kr.hayuni.chagokchagok.ui.ChagokTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChagokTheme {
                ChagokApp()
            }
        }
    }
}
