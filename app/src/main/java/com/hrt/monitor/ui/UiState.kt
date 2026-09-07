package com.hrt.monitor.ui

import androidx.compose.runtime.mutableIntStateOf
import com.hrt.monitor.data.Prefs

/** 可在组合中观察的 UI 偏好（写入后立即生效，无需重启） */
object UiState {
    val themeMode = mutableIntStateOf(Prefs.theme)
    val language = mutableIntStateOf(Prefs.language)
}
