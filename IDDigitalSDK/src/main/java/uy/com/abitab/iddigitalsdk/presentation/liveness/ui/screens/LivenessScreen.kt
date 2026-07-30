package uy.com.abitab.iddigitalsdk.presentation.liveness.ui.screens

import LoadingScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException


internal const val LivenessInstructionsScreen = "LivenessInstructionsScreen"
internal const val LivenessDetectorScreen = "LivenessDetectorScreen"
internal const val LivenessErrorScreen = "LivenessErrorScreen"
internal const val LivenessCompletedScreen = "LivenessCompletedScreen"

@Composable
internal fun LivenessScreen(
    onClose: () -> Unit,
    onBack: (() -> Unit)? = null,
    onCompleted: () -> Unit,
    executeChallenge: suspend () -> String?,
    isRetry: Boolean? = false
) {
    val navController = rememberNavController()
    var livenessSessionId: String? by remember { mutableStateOf(null) }

    NavHost(
        navController = navController,
        startDestination = if (isRetry == true) LivenessErrorScreen else LivenessInstructionsScreen
    ) {
        composable(LivenessInstructionsScreen) {
            LivenessInstructionsScreen(
                onStart = {
                    navController.navigate(LivenessDetectorScreen)
                },
                onBack = onBack,
                onClose = onClose
            )
        }
        composable(LivenessDetectorScreen) {
            LaunchedEffect(Unit) {
                if (livenessSessionId == null) {
                    livenessSessionId = executeChallenge()
                }
            }
            livenessSessionId?.let { sessionId ->
                LivenessDetectorScreen(
                    sessionId = sessionId,
                    onComplete = { navController.navigate(LivenessCompletedScreen) },
                    onError = { error: FaceLivenessDetectionException ->
                        navController.navigate(LivenessErrorScreen)
                    }
                )
            }
        }
        composable(LivenessErrorScreen) {
            LivenessErrorScreen(
                onRetry = {
                    livenessSessionId = null
                    navController.clearBackStack(LivenessInstructionsScreen)
                    navController.navigate(LivenessInstructionsScreen)
                },
                onClose = onClose
            )

        }
        composable(LivenessCompletedScreen) {
            LoadingScreen()
            LaunchedEffect(Unit) {
                onCompleted()
            }

        }

    }
}