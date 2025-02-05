package uy.com.abitab.iddigitalsdk.composables

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun AbitabTheme(content: @Composable () -> Unit) {
    val primaryColor = Color(0xFF002856)
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
            primaryContainer = primaryColor,
            onPrimaryContainer = Color.White,
            inversePrimary = primaryColor,
            secondary = secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = secondaryColor,
            onSecondaryContainer = Color.White,
            tertiary = Color(0xFFD9E3EB),
            onTertiary = Color(0xFF00437A),
            tertiaryContainer = Color(0xFFD9E3EB),
            onTertiaryContainer = Color(0xFF00437A),
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = Color.LightGray,
            onSurfaceVariant = Color.Black,
            surfaceTint = primaryColor,
            inverseSurface = Color.DarkGray,
            inverseOnSurface = Color.Black,
            error = Color.Red,
            onError = Color.White,
            errorContainer = Color.Red,
            onErrorContainer = Color.White,
            outline = Color.Gray,
            outlineVariant = Color.Gray,
            scrim = Color.Black.copy(alpha = 0.5f),
        ),
        typography = typography,
        content = content,
    )
}