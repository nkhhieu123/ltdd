package com.nhom4.subtrack

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nhom4.subtrack.ui.SubTrackRoot
import com.nhom4.subtrack.ui.VietnameseLocale
import com.nhom4.subtrack.ui.theme.SubTrackTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    /** Luôn hiển thị tiếng Việt, kể cả khi điện thoại đặt ngôn ngữ khác (DatePicker, định dạng ngày…). */
    override fun attachBaseContext(newBase: Context) {
        Locale.setDefault(VietnameseLocale)
        val config = Configuration(newBase.resources.configuration).apply { setLocale(VietnameseLocale) }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SubTrackTheme {
                SubTrackRoot()
            }
        }
    }
}
