package uy.com.abitab.iddigitalsdk.presentation.validation_session.ui.screens

import LoadingScreen
import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import uy.com.abitab.iddigitalsdk.CallbackHandler
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.screens.LivenessScreen
import uy.com.abitab.iddigitalsdk.presentation.pin.ui.screens.PinScreen
import uy.com.abitab.iddigitalsdk.presentation.validation_session.ui.viewmodels.ValidationSessionUiState
import uy.com.abitab.iddigitalsdk.presentation.validation_session.ui.viewmodels.ValidationSessionViewModel

@Composable
fun ValidationSession(challengeType: ChallengeType, context: Context, onClose: () -> Unit) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    val viewModel: ValidationSessionViewModel = koinViewModel { parametersOf(challengeType) }
    viewModel.setType(challengeType)


    val uiState by viewModel.uiState.collectAsState(initial = ValidationSessionUiState.Initial)

    var isRetry by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.createValidationSession(challengeType)
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ValidationSessionUiState.LaunchChallenge -> {
                val challenge = (uiState as ValidationSessionUiState.LaunchChallenge).challenge
                isRetry = (uiState as ValidationSessionUiState.LaunchChallenge).isRetry
                when (challenge.type) {
                    "liveness" -> {
                        if (navController.currentDestination?.route?.startsWith("liveness") != true) {
                            navController.navigate("liveness") {
                                popUpTo("loading") { inclusive = true }
                            }
                        }
                    }

                    "pin" -> {
                        if (navController.currentDestination?.route?.startsWith("pin") != true) {
                            navController.navigate("pin") {
                                popUpTo("loading") { inclusive = true }
                            }
                        }
                    }
                }
            }

            is ValidationSessionUiState.Success -> {
                CallbackHandler.onCompleted("Validation session completed successfully")
                (context as? Activity)?.finish()
            }

            is ValidationSessionUiState.Error -> {
                val error = (uiState as ValidationSessionUiState.Error).error
                CallbackHandler.onError(error)
                (context as? Activity)?.finish()
            }

            is ValidationSessionUiState.Initial -> {}
            is ValidationSessionUiState.Loading -> {}
        }
    }



    NavHost(navController = navController, startDestination = "loading") {
        composable("loading") {
            LoadingScreen()
        }

        composable("liveness") {
            LivenessScreen(
                onClose = onClose,
                onCompleted = {
                    isRetry = false
                    coroutineScope.launch {
                        viewModel.validateChallenge()
                    }
                },
                executeChallenge = { viewModel.executeChallenge() as String },
                isRetry = isRetry
            )

        }

        composable("pin") {
            PinScreen(
                isCreatingNewPin = false,
                onClose = onClose,
                onCompleted = { pin, _, usedBiometric, savePinToBiometrics ->
                    isRetry = false
                    coroutineScope.launch {
                        viewModel.validateChallenge(mapOf(
                            "pin" to pin,
                            "usedBiometric" to usedBiometric,
                            "savePinToBiometrics" to savePinToBiometrics
                        ))
                    }
                },
                hasError = isRetry,
                executeChallenge = {
                    return@PinScreen viewModel.executeChallenge() as Boolean?
                }
            )
        }
    }
}