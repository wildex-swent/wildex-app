package com.android.wildex.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.android.wildex.R

val FiraSans =
    FontFamily(
        Font(R.font.fira_sans_regular, FontWeight.Normal),
        Font(R.font.fira_sans_medium, FontWeight.Medium),
        Font(R.font.fira_sans_semibold, FontWeight.SemiBold),
        Font(R.font.fira_sans_bold, FontWeight.Bold),
        Font(R.font.fira_sans_italic, FontWeight.Normal, FontStyle.Italic))

val PhoneTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp),
        displayMedium =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 45.sp,
                lineHeight = 52.sp),
        displaySmall =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 36.sp,
                lineHeight = 44.sp),
        headlineLarge =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 32.sp,
                lineHeight = 40.sp),
        headlineMedium =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 36.sp),
        headlineSmall =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                lineHeight = 32.sp),
        titleLarge =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp),
        titleMedium =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp),
        titleSmall =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp),
        bodyLarge =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp),
        bodyMedium =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp),
        bodySmall =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp),
        labelLarge =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp),
        labelMedium =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp),
        labelSmall =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp))

val TabletTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 68.sp,
                lineHeight = 76.sp,
                letterSpacing = (-0.25).sp),
        displayMedium =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 54.sp,
                lineHeight = 62.sp),
        displaySmall =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 42.sp,
                lineHeight = 50.sp),
        headlineLarge =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 38.sp,
                lineHeight = 46.sp),
        headlineMedium =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 34.sp,
                lineHeight = 42.sp),
        headlineSmall =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 36.sp),
        titleLarge =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                lineHeight = 32.sp),
        titleMedium =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.15.sp),
        titleSmall =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.1.sp),
        bodyLarge =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.5.sp),
        bodyMedium =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.25.sp),
        bodySmall =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.4.sp),
        labelLarge =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.1.sp),
        labelMedium =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.5.sp),
        labelSmall =
            TextStyle(
                fontFamily = FiraSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp))
