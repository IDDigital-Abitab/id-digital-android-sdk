package uy.com.abitab.iddigitalsdk.presentation.qr_association.ui.screens

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
import uy.com.abitab.iddigitalsdk.CallbackHandler
import uy.com.abitab.iddigitalsdk.domain.usecases.CompleteTransactionUseCase
import uy.com.abitab.iddigitalsdk.presentation.device_association.ui.screens.DeviceAssociationInstructionsScreen
import uy.com.abitab.iddigitalsdk.presentation.device_association.ui.viewmodels.DeviceAssociationUiState
import uy.com.abitab.iddigitalsdk.presentation.device_association.ui.viewmodels.DeviceAssociationViewModel
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.screens.LivenessScreen
import uy.com.abitab.iddigitalsdk.presentation.pin.ui.screens.PinScreen
import uy.com.abitab.iddigitalsdk.utils.UnknownError
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError

/**
 * QR cross-device variant of `DeviceAssociation.kt` (device_association
 * package): identical challenge flow (instructions -> liveness/pin), reusing
 * [DeviceAssociationViewModel] unchanged - see .docs/sdk/cliente/01-arquitectura-y-flujos.md.
 *
 * The only differences are: it starts with a QR scan step to obtain the
 * `transactionId` (an opaque signed token, never parsed/validated here) -
 * used both to create the device association session and, on success, to
 * complete that OIDC transaction ([CompleteTransactionUseCase]) itself
 * instead of just reporting `idToken`/`validationSessionId` back to the
 * Integrator - there is no Integrator round-trip (push/deep link) to do that
 * in this flow, since the citizen scanned the code directly from within this
 * same app.
 */
@Composable
internal fun QrAssociationFlow(context: Context, onClose: () -> Unit) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    val viewModel: DeviceAssociationViewModel = koinViewModel()

    val completeTransactionUseCase: CompleteTransactionUseCase = remember {
        GlobalContext.get().get()
    }

    val uiState by viewModel.uiState.collectAsState(initial = DeviceAssociationUiState.Initial)
    var isRetry by remember { mutableStateOf(false) }
    var scannedTransactionId by remember { mutableStateOf<String?>(null) }

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
                val successState = uiState as DeviceAssociationUiState.Success
                val transactionId = scannedTransactionId
                if (transactionId == null) {
                    // No debería pasar (la ruta "scan" es el único camino hacia
                    // "instructions"), pero se cubre para no dejar la Activity
                    // colgada sin invocar ningún callback.
                    CallbackHandler.onError(UnknownError("QR scan result missing"))
                    (context as? Activity)?.finish()
                    return@LaunchedEffect
                }
                try {
                    val result = completeTransactionUseCase(
                        transactionId,
                        successState.validationSessionId
                    )
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

    NavHost(navController = navController, startDestination = "scan") {
        composable("scan") {
            QrScanScreen(
                onScanned = { value ->
                    scannedTransactionId = value
                    viewModel.setTransactionId(value)
                    navController.navigate("instructions") {
                        popUpTo("scan") { inclusive = true }
                    }
                },
                onClose = onClose
            )
        }

        composable("instructions") {
            DeviceAssociationInstructionsScreen(
                onStart = { viewModel.startDeviceAssociation() },
                onClose = { (context as? Activity)?.finish() }
            )
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
