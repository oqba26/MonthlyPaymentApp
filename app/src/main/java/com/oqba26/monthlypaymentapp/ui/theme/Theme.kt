package com.oqba26.monthlypaymentapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = Color.White,
    secondary = SecondaryGreenLight,
    onSecondary = Color.White,
    secondaryContainer = SecondaryGreenDark,
    onSecondaryContainer = Color.White,
    tertiary = Pink80,
    onTertiary = Gray900,
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray300,
    outline = Gray600,
    outlineVariant = Gray700,
    error = AccentRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = Color.White,
    secondary = SecondaryGreen,
    onSecondary = Color.White,
    secondaryContainer = SecondaryGreenLight,
    onSecondaryContainer = Color.White,
    tertiary = Pink40,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = Gray900,
    surface = SurfaceLight,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,
    surfaceContainer = Gray50,
    outline = Gray400,
    outlineVariant = Gray300,
    error = AccentRed,
    onError = Color.White,
    inverseSurface = Gray900,
    inverseOnSurface = Color.White
)

@Composable
fun MonthlyPaymentManagement2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Dynamic color can clash with custom themes
    content: @Composable () -> Unit
) {
    // یک بار کانتکست UI و اپ را بگیریم
    val uiContext = LocalContext.current
    val appContext = uiContext.applicationContext

    // Repository را با appContext بساز تا لیک نده
    val settingsRepository = remember(appContext) { SettingsRepository(appContext) }
    val selectedFontName by settingsRepository
        .selectedFontFlow
        .collectAsState(initial = SettingsRepository.DEFAULT_FONT)

    val fontFamily = when (selectedFontName) {
        "Vazirmatn" -> VazirmatnFontFamily
        "BYekan" -> BYekanFontFamily
        "Sahel" -> SahelFontFamily
        "IranianSans" -> IranianSansFontFamily
        else -> EstedadFontFamily
    }

    // از uiContext بیرونی استفاده کن؛ دیگه context جدید تعریف نکن
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(uiContext)
            else dynamicLightColorScheme(uiContext)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        if (activity != null) {
            SideEffect {
                val window = activity.window
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightStatusBars = false  // متن سفید می‌شه
            }
        }
    }

    val typography = Typography(
        displayLarge = TextStyle(fontFamily = fontFamily, fontSize = 57.sp),
        displayMedium = TextStyle(fontFamily = fontFamily, fontSize = 45.sp),
        displaySmall = TextStyle(fontFamily = fontFamily, fontSize = 36.sp),
        headlineLarge = TextStyle(fontFamily = fontFamily, fontSize = 32.sp),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontSize = 28.sp),
        headlineSmall = TextStyle(fontFamily = fontFamily, fontSize = 24.sp),
        titleLarge = TextStyle(fontFamily = fontFamily, fontSize = 22.sp),
        titleMedium = TextStyle(fontFamily = fontFamily, fontSize = 16.sp),
        titleSmall = TextStyle(fontFamily = fontFamily, fontSize = 14.sp),
        bodyLarge = TextStyle(fontFamily = fontFamily, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontSize = 14.sp),
        bodySmall = TextStyle(fontFamily = fontFamily, fontSize = 12.sp),
        labelLarge = TextStyle(fontFamily = fontFamily, fontSize = 14.sp),
        labelMedium = TextStyle(fontFamily = fontFamily, fontSize = 12.sp),
        labelSmall = TextStyle(fontFamily = fontFamily, fontSize = 11.sp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}