package uy.com.abitab.iddigitalsdk.composables

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val primaryColor = Color(0xFFEC0000)
    val secondaryColor = Color(0xFF1C63B6)

    val typography = Typography(
        headlineLarge = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 32.sp,
            color = Color(0xFF002856)
        ),
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = Color(0xFF002856),

            ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = Color(0xFF002856),
        )
    )


    MaterialTheme(
        colorScheme = ColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor, // Color used for containers with primary content
            onPrimaryContainer = Color.White,
            inversePrimary = primaryColor, // Color for primary elements on inverse surfaces
            secondary = secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = secondaryColor, // Color used for containers with secondary content
            onSecondaryContainer = Color.White,
            tertiary = Color(0xFFD9E3EB),
            onTertiary = Color(0xFF00437A),
            tertiaryContainer = Color(0xFFD9E3EB), // Color used for containers with tertiary content
            onTertiaryContainer = Color(0xFF00437A),
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = Color.LightGray, // A slightly different surface color for variation
            onSurfaceVariant = Color.Black,
            surfaceTint = primaryColor, // A tint color applied to surfaces
            inverseSurface = Color.DarkGray, // Color for inverse surfaces
            inverseOnSurface = Color.Black, // Color for text on inverse surfaces
            error = Color.Red,
            onError = Color.White,
            errorContainer = Color.Red, // Color used for containers with error content
            onErrorContainer = Color.White,
            outline = Color.Gray, // Color for outlines and dividers
            outlineVariant = Color.Gray, // A variant color for outlines and dividers
            scrim = Color.Black.copy(alpha = 0.5f), // A semi-transparent overlay color
        ),
        typography = typography,
        content = content,
    )
}