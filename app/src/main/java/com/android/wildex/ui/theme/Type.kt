package com.android.wildex.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object FontSizes {
  // profile / header
  val TopBarTitle = 20.sp
  val ProfileName = 24.sp
  val ProfileUsername = 18.sp

  // stats / counts
  val StatLarge = 24.sp
  val StatLabel = 18.sp

  // body / descriptions
  val BodyLarge = 16.sp
  val BodyMedium = 14.sp
  val BodySmall = 14.sp

  // other UI labels
  val ButtonLabel = 14.sp
  val SectionTitle = 14.sp
}

val Typography =
    Typography(
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = FontSizes.BodyLarge,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = FontSizes.TopBarTitle,
                lineHeight = 28.sp,
                letterSpacing = 0.sp),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = FontSizes.ProfileName,
                lineHeight = 28.sp,
                letterSpacing = 0.sp),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = FontSizes.BodyMedium,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp),
        bodySmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = FontSizes.BodySmall,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp))
