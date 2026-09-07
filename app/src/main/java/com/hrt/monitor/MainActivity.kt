package com.hrt.monitor

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hrt.monitor.data.AppRepo
import com.hrt.monitor.data.Prefs
import com.hrt.monitor.ui.App
import com.hrt.monitor.ui.HrtTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        Prefs.init(newBase)
        val wrapped = when (Prefs.language) {
            Prefs.LANG_ZH_CN -> newBase.wrapLocale(Locale.SIMPLIFIED_CHINESE)
            Prefs.LANG_ZH_TW -> newBase.wrapLocale(Locale.TRADITIONAL_CHINESE)
            else -> newBase
        }
        super.attachBaseContext(wrapped)
    }

    private fun Context.wrapLocale(locale: Locale): Context {
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        return createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRepo.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            HrtTheme {
                App()
            }
        }
    }
}
