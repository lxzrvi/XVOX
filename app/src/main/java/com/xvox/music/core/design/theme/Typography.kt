package com.xvox.music.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
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

val XvoxUiFont = FontFamily(
    Font(
        resId = R.font.xvoxplusjakartasans,
        weight = FontWeight.Normal
    )
)

val XvoxItalicFont = FontFamily(
    Font(
        resId = R.font.xvoxplusjakartasansitalic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic
    )
)

val XvoxTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 35.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 27.sp
    ),
    titleMedium = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
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
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = XvoxUiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)
