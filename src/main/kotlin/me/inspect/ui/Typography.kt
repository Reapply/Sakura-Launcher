package me.inspect.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

val GoogleSans = FontFamily(
    Font(resource = "fonts/GoogleSans-Regular.ttf", weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resource = "fonts/GoogleSans-Italic.ttf", weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resource = "fonts/GoogleSans-Medium.ttf", weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resource = "fonts/GoogleSans-MediumItalic.ttf", weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(resource = "fonts/GoogleSans-SemiBold.ttf", weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resource = "fonts/GoogleSans-SemiBoldItalic.ttf", weight = FontWeight.SemiBold, style = FontStyle.Italic),
    Font(resource = "fonts/GoogleSans-Bold.ttf", weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resource = "fonts/GoogleSans-BoldItalic.ttf", weight = FontWeight.Bold, style = FontStyle.Italic),
)

val SakuraTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)
