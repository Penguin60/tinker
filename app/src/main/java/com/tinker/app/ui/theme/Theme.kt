package com.tinker.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinker.app.R

/** Warm, PostHog-inspired palette. Committed light theme — the warm cream is the brand. */
data class AppColors(
    val canvas: Color,
    val surface: Color,
    val surfaceSoft: Color,
    val ink: Color,
    val body: Color,
    val mute: Color,
    val hairline: Color,
    val primary: Color,
    val primaryEdge: Color,
    val onPrimary: Color,
    val live: Color,
    val liveEdge: Color,
    val liveSoft: Color,
    val onLive: Color,
    val danger: Color,
)

data class AppTypography(
    val title: TextStyle,
    val heading: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val overline: TextStyle,
    val caption: TextStyle,
)

data class AppSpacing(val xs: Dp, val sm: Dp, val md: Dp, val lg: Dp)
data class AppRadius(val sm: Dp, val md: Dp, val lg: Dp)

private val Warm = AppColors(
    canvas = Color(0xFFEEEFE9),
    surface = Color(0xFFFFFFFF),
    surfaceSoft = Color(0xFFE5E7E0),
    ink = Color(0xFF23251D),
    body = Color(0xFF4D4F46),
    mute = Color(0xFF6C6E63),
    hairline = Color(0xFFBFC1B7),
    primary = Color(0xFFF7A501),
    primaryEdge = Color(0xFFB17816),
    onPrimary = Color(0xFF23251D),
    live = Color(0xFFF54E00),
    liveEdge = Color(0xFFC43E00),
    liveSoft = Color(0xFFF7DED0),
    onLive = Color(0xFFFFFFFF),
    danger = Color(0xFFCD4239),
)

private val Plex = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_bold, FontWeight.Bold),
)

private val typography = AppTypography(
    title = TextStyle(fontFamily = Plex, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    heading = TextStyle(fontFamily = Plex, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    body = TextStyle(fontFamily = Plex, fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    label = TextStyle(fontFamily = Plex, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    overline = TextStyle(fontFamily = Plex, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
    caption = TextStyle(fontFamily = Plex, fontSize = 13.sp, fontWeight = FontWeight.Medium),
)

private val spacing = AppSpacing(xs = 4.dp, sm = 8.dp, md = 16.dp, lg = 24.dp)
private val radius = AppRadius(sm = 10.dp, md = 14.dp, lg = 18.dp)

val LocalColors = staticCompositionLocalOf { Warm }
val LocalTypography = staticCompositionLocalOf { typography }
val LocalSpacing = staticCompositionLocalOf { spacing }
val LocalRadius = staticCompositionLocalOf { radius }

object AppTheme {
    val colors: AppColors @Composable get() = LocalColors.current
    val type: AppTypography @Composable get() = LocalTypography.current
    val space: AppSpacing @Composable get() = LocalSpacing.current
    val radius: AppRadius @Composable get() = LocalRadius.current
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColors provides Warm,
        LocalTypography provides typography,
        LocalSpacing provides spacing,
        LocalRadius provides radius,
        content = content,
    )
}
