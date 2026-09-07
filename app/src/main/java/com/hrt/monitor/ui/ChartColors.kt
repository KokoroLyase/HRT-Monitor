package com.hrt.monitor.ui

import androidx.compose.ui.graphics.Color
import com.hrt.monitor.data.RefRange

/** 参考范围在图例与图表中的配色（按 id 稳定取色） */
private val bandPalette = listOf(
    Color(0xFF8E5BD7),
    Color(0xFFE8639F),
    Color(0xFF2E9E8F),
    Color(0xFFE8963C),
    Color(0xFF5C8FDE),
    Color(0xFF7BA23F)
)

fun bandColorOf(range: RefRange): Color =
    bandPalette[Math.floorMod(range.id.hashCode(), bandPalette.size)]
