package uy.com.abitab.iddigitalsdk.presentation.device_association.ui.screens

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
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.presentation.device_association.ui.viewmodels.DeviceAssociationUiState
import uy.com.abitab.iddigitalsdk.presentation.device_association.ui.viewmodels.DeviceAssociationViewModel
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.screens.LivenessScreen
import uy.com.abitab.iddigitalsdk.presentation.pin.ui.screens.PinScreen


@Composable
fun DeviceAssociation(document: Document, context: Context, onClose: () -> Unit) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    val viewModel: DeviceAssociationViewModel = koinViewModel { parametersOf(document) }
    viewModel.setDocument(document)

    val uiState by viewModel.uiState.collectAsState(initial = DeviceAssociationUiState.Initial)
    var isRetry by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is DeviceAssociationUiState.LaunchChallenge -> {
                val challenge = (uiState as DeviceAssociationUiState.LaunchChallenge).challenge
                isRetry = (uiState as DeviceAssociationUiState.LaunchChallenge).isRetry
                when (challenge.type) {
                    "liveness" -> {
                        if (navController.currentDestination?.route != "liveness") {
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

            is DeviceAssociationUiState.Success -> {
                val idToken = (uiState as DeviceAssociationUiState.Success).idToken
                CallbackHandler.onCompleted(idToken)
                (context as? Activity)?.finish()
            }

            is DeviceAssociationUiState.Error -> {
                val error = (uiState as DeviceAssociationUiState.Error).error
                CallbackHandler.onError(error)
                (context as? Activity)?.finish()
            }

            else -> {}
        }
    }

    if (uiState is DeviceAssociationUiState.Loading) {
        LoadingScreen()
    }

    NavHost(navController = navController, startDestination = "instructions") {
        composable("instructions") {
            DeviceAssociationInstructionsScreen(onStart = { viewModel.startDeviceAssociation() },
                onClose = { (context as? Activity)?.finish() })
        }

        composable("liveness") {
            LivenessScreen(
                onClose = onClose,
                onBack = { navController.popBackStack() },
                onCompleted = {
                    isRetry = false
                    coroutineScope.launch {
                        viewModel.validateChallenge()
                    }
                },
                executeChallenge = { viewModel.executeChallenge() },
                isRetry = isRetry
            )
        }

        composable("pin") {
            PinScreen(
                isCreatingNewPin = true,
                onClose = onClose,
                onBack = { navController.popBackStack() },
                onCompleted = { pin, saveBiometricPin, _, _ ->
                    isRetry = false
                    coroutineScope.launch {
                        viewModel.validateChallenge(
                            pin, options = mapOf(
                                "saveBiometricPin" to saveBiometricPin
                            )
                        )
                    }
                },
                hasError = isRetry
            )
        }
    }
}