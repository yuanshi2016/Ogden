package com.example.ogdenkids

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal class Palette(
    val paper: Color,
    val paperElevated: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val line: Color,
    val primary: Color,
    val accent: Color,
    val error: Color,
    val listening: Color,
    val adult: Boolean,
    val dark: Boolean
)

// 儿童：主色/强调色保持高饱和、背景偏暖；成人：整体降饱和，中性色偏冷一点
internal val ChildPalette = Palette(
    paper = Color(0xFFF7F4EE),
    paperElevated = Color(0xFFFFFFFF),
    ink = Color(0xFF1C2430),
    inkSoft = Color(0xFF6B7380),
    inkFaint = Color(0xFF9AA3B0),
    line = Color(0xFFE7E4DE),
    primary = Color(0xFF2BB673),
    accent = Color(0xFFFF8A3D),
    error = Color(0xFFE85D4C),
    listening = Color(0xFF4C8DFF),
    adult = false,
    dark = false
)

internal val AdultLightPalette = Palette(
    paper = Color(0xFFF4F2EF),
    paperElevated = Color(0xFFFFFFFF),
    ink = Color(0xFF1C2430),
    inkSoft = Color(0xFF6B7380),
    inkFaint = Color(0xFF9AA3B0),
    line = Color(0xFFE6E3DC),
    primary = Color(0xFF4CA07A),
    accent = Color(0xFFC98A52),
    error = Color(0xFFC5675A),
    listening = Color(0xFF6484C4),
    adult = true,
    dark = false
)

// 深色只给成人：底偏冷蓝黑，主色提亮保对比度
internal val AdultDarkPalette = Palette(
    paper = Color(0xFF161B22),
    paperElevated = Color(0xFF1E242E),
    ink = Color(0xFFE6EAF0),
    inkSoft = Color(0xFFA8B1BD),
    inkFaint = Color(0xFF6B7380),
    line = Color(0xFF2E3540),
    primary = Color(0xFF41C98A),
    accent = Color(0xFFFFA066),
    error = Color(0xFFF0816F),
    listening = Color(0xFF74A5FF),
    adult = true,
    dark = true
)

internal val LocalPalette = staticCompositionLocalOf { ChildPalette }

// 顶层颜色常量改成按主题取值的组合式属性，调用点写法不变
internal val Paper: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.paper
internal val PaperElevated: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.paperElevated
internal val Ink: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.ink
internal val InkSoft: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.inkSoft
internal val InkFaint: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.inkFaint
internal val Line: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.line
internal val Primary: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.primary
internal val Error: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.error
internal val Listening: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.listening
// 答对反馈复用主色（绿），避免再维护一套成功色
internal val Success: Color
    @Composable @ReadOnlyComposable get() = Primary

// 儿童热区更大，成人更紧凑
internal val TouchTarget: Dp
    @Composable @ReadOnlyComposable get() = if (LocalPalette.current.adult) 48.dp else 64.dp

// 成人模式把分类饱和色往灰度收一点；深色下统一向白提亮保可读性
internal fun Color.desaturate(amount: Float): Color {
    val g = red * 0.299f + green * 0.587f + blue * 0.114f
    return lerp(this, Color(g, g, g, alpha), amount)
}

internal fun Color.forMode(p: Palette): Color {
    val v = if (p.adult) desaturate(0.28f) else this
    return if (p.dark) lerp(v, Color.White, 0.52f) else v
}

internal val Category.tint: Color
    @Composable @ReadOnlyComposable get() = baseTint.forMode(LocalPalette.current)

internal val Category.soft: Color
    @Composable @ReadOnlyComposable get() =
        if (LocalPalette.current.dark) baseTint.forMode(LocalPalette.current).copy(alpha = 0.20f)
        else if (LocalPalette.current.adult) baseSoft.desaturate(0.28f)
        else baseSoft
