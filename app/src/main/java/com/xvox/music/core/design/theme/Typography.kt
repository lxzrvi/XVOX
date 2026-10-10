package com.xvox.music.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.xvox.music.R

val XvoxLogoFont = FontFamily(
    Font(
        resId = R.font.xvoxcinzeldecorative,
        weight = FontWeight.Normal
    )
)

val XvoxPersonalFont = FontFamily(
    Font(
        resId = R.font.xvoxnothingyoucoulddo,
        weight = FontWeight.Normal
    )
)

/** The original XVOX UI family remains the default (and keeps M-sized installs familiar). */
val XvoxUiFont = FontFamily(
    Font(resId = R.font.xvox_inter_regular, weight = FontWeight.Normal),
    Font(resId = R.font.xvox_inter_medium, weight = FontWeight.Medium),
    Font(resId = R.font.xvox_inter_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.xvox_inter_bold, weight = FontWeight.Bold)
)

/**
 * Ten additional bundled Google Fonts choices use their upstream OFL-1.1 distributions. Variable
 * fonts deliberately use Compose's normal entry; Compose/Android synthesizes weight where a view
 * asks for it, while retaining the exact selected family across the app.
 */
data class XvoxFontOption(
    val key: String,
    val label: String,
    val fontFamily: FontFamily
)

private fun xvoxSingleFont(resourceId: Int) = FontFamily(Font(resourceId, FontWeight.Normal))

val XvoxFontOptions: List<XvoxFontOption> = listOf(
    XvoxFontOption("inter", "Inter", XvoxUiFont),
    XvoxFontOption("abeezee", "ABeeZee", xvoxSingleFont(R.font.xvox_font_abeezee)),
    XvoxFontOption("caveat", "Caveat", xvoxSingleFont(R.font.xvox_font_caveat)),
    XvoxFontOption("comfortaa", "Comfortaa", xvoxSingleFont(R.font.xvox_font_comfortaa)),
    XvoxFontOption("dmsans", "DM Sans", xvoxSingleFont(R.font.xvox_font_dmsans)),
    XvoxFontOption("josefin", "Josefin Sans", xvoxSingleFont(R.font.xvox_font_josefin)),
    XvoxFontOption("lora", "Lora", xvoxSingleFont(R.font.xvox_font_lora)),
    XvoxFontOption("manrope", "Manrope", xvoxSingleFont(R.font.xvox_font_manrope)),
    XvoxFontOption("nunito", "Nunito", xvoxSingleFont(R.font.xvox_font_nunito)),
    XvoxFontOption("outfit", "Outfit", xvoxSingleFont(R.font.xvox_font_outfit)),
    XvoxFontOption("spacegrotesk", "Space Grotesk", xvoxSingleFont(R.font.xvox_font_spacegrotesk))
)

fun xvoxUiFontFor(key: String?): FontFamily =
    XvoxFontOptions.firstOrNull { it.key == key }?.fontFamily ?: XvoxUiFont

/** Selected app text family; branded XVOX/personalization marks intentionally remain distinct. */
val LocalXvoxUiFont = staticCompositionLocalOf { XvoxUiFont }

val XvoxItalicFont = XvoxUiFont

val XvoxTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    displayMedium = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    displaySmall = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 35.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 27.sp
    ),
    titleMedium = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    )
)

/** Applies the selected family to every Material text role while preserving XVOX's size rhythm. */
fun xvoxTypographyFor(fontFamily: FontFamily): Typography = Typography(
    displayLarge = XvoxTypography.displayLarge.copy(fontFamily = fontFamily),
    displayMedium = XvoxTypography.displayMedium.copy(fontFamily = fontFamily),
    displaySmall = XvoxTypography.displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = XvoxTypography.headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = XvoxTypography.headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = XvoxTypography.headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = XvoxTypography.titleLarge.copy(fontFamily = fontFamily),
    titleMedium = XvoxTypography.titleMedium.copy(fontFamily = fontFamily),
    titleSmall = XvoxTypography.titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = XvoxTypography.bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = XvoxTypography.bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = XvoxTypography.bodySmall.copy(fontFamily = fontFamily),
    labelLarge = XvoxTypography.labelLarge.copy(fontFamily = fontFamily),
    labelMedium = XvoxTypography.labelMedium.copy(fontFamily = fontFamily),
    labelSmall = XvoxTypography.labelSmall.copy(fontFamily = fontFamily)
)
