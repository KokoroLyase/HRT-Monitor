package com.hrt.monitor.data

import android.content.Context
import android.content.SharedPreferences

/** 轻量偏好设置：主题 / 语言 / AI 配置（AI 密钥仅存本机） */
object Prefs {
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2
    const val LANG_SYSTEM = 0
    const val LANG_ZH_CN = 1
    const val LANG_ZH_TW = 2

    const val DEFAULT_BASE_URL = "https://api.deepseek.com/v1"
    const val DEFAULT_MODEL = "deepseek-chat"

    private lateinit var sp: SharedPreferences

    @Synchronized
    fun init(context: Context) {
        if (!::sp.isInitialized) {
            sp = context.applicationContext.getSharedPreferences("hrt_prefs", Context.MODE_PRIVATE)
        }
    }

    var theme: Int
        get() = sp.getInt("theme", THEME_SYSTEM)
        set(v) { sp.edit().putInt("theme", v).apply() }

    var language: Int
        get() = sp.getInt("language", LANG_SYSTEM)
        set(v) { sp.edit().putInt("language", v).apply() }

    var aiBaseUrl: String
        get() = sp.getString("ai_base_url", "") ?: ""
        set(v) { sp.edit().putString("ai_base_url", v).apply() }

    var aiKey: String
        get() = sp.getString("ai_key", "") ?: ""
        set(v) { sp.edit().putString("ai_key", v).apply() }

    var aiModel: String
        get() = sp.getString("ai_model", "") ?: ""
        set(v) { sp.edit().putString("ai_model", v).apply() }

    var agreedDisclaimer: Boolean
        get() = sp.getBoolean("agreed_disclaimer", false)
        set(v) { sp.edit().putBoolean("agreed_disclaimer", v).apply() }

    /** 生日（0 = 未设置） */
    var birthday: Long
        get() = sp.getLong("birthday", 0L)
        set(v) { sp.edit().putLong("birthday", v).apply() }

    /** HRT 起始日期（0 = 未设置） */
    var hrtStart: Long
        get() = sp.getLong("hrt_start", 0L)
        set(v) { sp.edit().putLong("hrt_start", v).apply() }

    /** 是否已展示过首启个人资料引导 */
    var profileAsked: Boolean
        get() = sp.getBoolean("profile_asked", false)
        set(v) { sp.edit().putBoolean("profile_asked", v).apply() }

    fun aiConfigured(): Boolean = aiBaseUrl.isNotBlank() && aiKey.isNotBlank() && aiModel.isNotBlank()
}
