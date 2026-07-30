package uy.com.abitab.iddigitalsdk.presentation.qr_validation.ui.screens

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import uy.com.abitab.iddigitalsdk.CallbackHandler
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.usecases.CompleteTransactionUseCase
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.screens.LivenessScreen
import uy.com.abitab.iddigitalsdk.presentation.pin.ui.screens.PinScreen
import uy.com.abitab.iddigitalsdk.presentation.qr_association.ui.screens.QrScanScreen
import uy.com.abitab.iddigitalsdk.presentation.validation_session.ui.viewmodels.ValidationSessionUiState
import uy.com.abitab.iddigitalsdk.presentation.validation_session.ui.viewmodels.ValidationSessionViewModel
import uy.com.abitab.iddigitalsdk.utils.UnknownError
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError

/**
 * QR cross-device variant of `ValidationSession.kt` (validation_session
 * package): identical challenge flow (liveness/pin), reusing
 * [ValidationSessionViewModel] unchanged - see .docs/sdk/cliente/08-qr-cross-device.md.
 *
 * The only differences are: it starts with a QR scan step to obtain the
 * `transactionId` (an opaque signed token, never parsed/validated here), and
 * on success it completes that OIDC transaction ([CompleteTransactionUseCase])
 * itself instead of just reporting the `validationSessionId` back to the
 * Integrator - there is no Integrator round-trip (push/deep link) to do that
 * in this flow, since the citizen scanned the code directly from within this
 * same app.
 */
@Composable
internal fun QrValidationFlow(challengeType: ChallengeType, context: Context, onClose: () -> Unit) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    val viewModel: ValidationSessionViewModel = koinViewModel { parametersOf(challengeType) }
    // Gateado a challengeType (no a cada recomposición): este composable colecta
    // uiState más abajo, así que cualquier transición de estado (incluyendo Success)
    // recompone la función. Llamar setType() sin guard reemitía Initial en cada
    // recomposición, lo que podía cancelar el LaunchedEffect(uiState) de más abajo
    // justo mientras completeTransactionUseCase() esperaba la respuesta del backend.
    LaunchedEffect(challengeType) {
        viewModel.setType(challengeType)
    }

    val completeTransactionUseCase: CompleteTransactionUseCase = remember {
        GlobalContext.get().get()
    }

    val uiState by viewModel.uiState.collectAsState(initial = ValidationSessionUiState.Initial)
    var isRetry by remember { mutableStateOf(false) }
    var scannedTransactionId by remember { mutableStateOf<String?>(null) }

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
                val validationSessionId = (uiState as ValidationSessionUiState.Success).validationSessionId
                val transactionId = scannedTransactionId
                if (transactionId == null) {
                    // No debería pasar (la ruta "scan" es el único camino hacia
                    // "loading"), pero se cubre para no dejar la Activity colgada
                    // sin invocar ningún callback.
                    CallbackHandler.onError(UnknownError("QR scan result missing"))
                    (context as? Activity)?.finish()
                    return@LaunchedEffect
                }
                try {
                    val result = completeTransactionUseCase(transactionId, validationSessionId)
                    CallbackHandler.onQrAssociationCompleted(result.finishUrl)
                } catch (e: CancellationException) {
                    // No es un error de negocio: esta corrutina fue cancelada (por ejemplo,
                    // porque uiState cambió de nuevo). Reportarla como UnknownError rompería
                    // la cooperación de cancelación estructurada y podría mostrar un error
                    // genérico pese a que el backend ya haya completado la transacción.
                    throw e
                } catch (e: Throwable) {
                    CallbackHandler.onError(e.toIDDigitalError("Error completing QR transaction"))
                }
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

    NavHost(navController = navController, startDestination = "scan") {
        composable("scan") {
            QrScanScreen(
                onScanned = { value ->
                    scannedTransactionId = value
                    coroutineScope.launch {
                        viewModel.createValidationSession(challengeType)
                    }
                    navController.navigate("loading") {
                        popUpTo("scan") { inclusive = true }
                    }
                },
                onClose = onClose
            )
        }

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
                        viewModel.validateChallenge(
                            mapOf(
                                "pin" to pin,
                                "usedBiometric" to usedBiometric,
                                "savePinToBiometrics" to savePinToBiometrics
                            )
                        )
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
