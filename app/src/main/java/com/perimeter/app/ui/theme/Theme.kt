package com.perimeter.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AppColors(
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val border: Color,
)

data class AppTypography(val title: TextStyle, val body: TextStyle, val label: TextStyle)

data class AppSpacing(val xs: Dp, val sm: Dp, val md: Dp, val lg: Dp)

private val Dark = AppColors(
    background = Color(0xFF0E0F12),
    surface = Color(0xFF1A1C20),
    onSurface = Color(0xFFF2F3F5),
    muted = Color(0xFF8A8F98),
    accent = Color(0xFF5B8CFF),
    onAccent = Color(0xFFFFFFFF),
    border = Color(0xFF2A2D33),
)

private val Light = AppColors(
    background = Color(0xFFF6F7F9),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF15171A),
    muted = Color(0xFF6B7078),
    accent = Color(0xFF2F6BFF),
    onAccent = Color(0xFFFFFFFF),
    border = Color(0xFFE2E5EA),
)

private val typography = AppTypography(
    title = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    body = TextStyle(fontSize = 15.sp),
    label = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
)

private val spacing = AppSpacing(xs = 4.dp, sm = 8.dp, md = 16.dp, lg = 24.dp)

val LocalColors = staticCompositionLocalOf { Dark }
val LocalTypography = staticCompositionLocalOf { typography }
val LocalSpacing = staticCompositionLocalOf { spacing }

object AppTheme {
    val colors: AppColors @Composable get() = LocalColors.current
    val type: AppTypography @Composable get() = LocalTypography.current
    val space: AppSpacing @Composable get() = LocalSpacing.current
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColors provides if (isSystemInDarkTheme()) Dark else Light,
        LocalTypography provides typography,
        LocalSpacing provides spacing,
        content = content,
    )
}
