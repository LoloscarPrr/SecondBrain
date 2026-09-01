package com.secondbrain.app.core.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class LayoutClass {
    COMPACT,
    NORMAL,
    LARGE
}

fun layoutClassFor(width: Dp): LayoutClass = when {
    width < 360.dp -> LayoutClass.COMPACT
    width < 840.dp -> LayoutClass.NORMAL
    else -> LayoutClass.LARGE
}
