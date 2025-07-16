package uy.com.abitab.iddigitalsdk.presentation.liveness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException
import com.amplifyframework.ui.liveness.ui.FaceLivenessDetector
import uy.com.abitab.iddigitalsdk.ui.theme.AbitabTheme

@Composable
fun LivenessDetectorScreen(sessionId: String, onComplete : () -> Unit, onError : (error: FaceLivenessDetectionException) -> Unit) {
    AbitabTheme {
        Box(
            modifier = Modifier
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            FaceLivenessDetector(
                sessionId = sessionId,
                region = "us-east-1",
                disableStartView = true,
                onComplete = onComplete,
                onError = onError
            )
        }
    }
}