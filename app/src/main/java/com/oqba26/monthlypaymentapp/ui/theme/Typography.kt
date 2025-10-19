package com.oqba26.monthlypaymentapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.oqba26.monthlypaymentapp.R

// Corrected Font Families based on available files

val EstedadFontFamily = FontFamily(
    Font(R.font.estedad_light, FontWeight.Light),
    Font(R.font.estedad_regular, FontWeight.Normal),
    Font(R.font.estedad_medium, FontWeight.Medium),
    Font(R.font.estedad_bold, FontWeight.Bold),
    Font(R.font.estedad_black, FontWeight.Black),
)

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn_thin, FontWeight.Thin),
    Font(R.font.vazirmatn_light, FontWeight.Light),
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_black, FontWeight.Black),
)

val BYekanFontFamily = FontFamily(
    Font(R.font.byekan, FontWeight.Normal),
    Font(R.font.byekan_bold, FontWeight.Bold)
)

val SahelFontFamily = FontFamily(
    // Only bold and black are available for Sahel
    Font(R.font.sahel_bold, FontWeight.Bold),
    Font(R.font.sahel_black, FontWeight.Black)
)

val IranianSansFontFamily = FontFamily(
    Font(R.font.iraniansans, FontWeight.Normal)
)

// Default Typography - This is now defined in Theme.kt to be dynamic
// We can keep a default here if needed for previews or other purposes.
val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = EstedadFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
