package com.xvox.music.core.design.theme

import androidx.compose.material3.Typography
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

val XvoxUiFont = FontFamily(
    Font(resId = R.font.xvox_inter_regular, weight = FontWeight.Normal),
    Font(resId = R.font.xvox_inter_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.xvox_inter_bold, weight = FontWeight.Bold)
)

val XvoxItalicFont = XvoxUiFont

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
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 30.sp
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
    )
)
