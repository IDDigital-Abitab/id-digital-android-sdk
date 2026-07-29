package com.example.iddigital

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.iddigital.PendingVerificationFlow
import com.example.iddigital.deeplink.DeepLinkPayload
import com.example.iddigital.fcm.PushPayload
import com.example.iddigital.keycloak.KeycloakRedirectResult
import com.google.firebase.messaging.FirebaseMessaging
import uy.com.abitab.iddigitalsdk.IDDigitalSDK
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Examples(
    sdkInstance: IDDigitalSDK,
    onError: (IDDigitalError) -> Unit,
    keycloakRedirect: KeycloakRedirectResult? = null,
    incomingPush: PushPayload? = null,
    incomingDeepLink: DeepLinkPayload? = null,
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }

    var debugTransactionId by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ID Digital — App de ejemplo") })
        }, modifier = Modifier.clickable(interactionSource = interactionSource,
            indication = null,
            onClick = { focusManager.clearFocus() })
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(32.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Flujo principal: Patron B (puente web) de punta a punta, ver
            // .docs/sdk/primera-asociacion-app-integradora.md §2.2 y .docs/sdk/cliente/*.md
            PendingVerificationFlow(
                sdkInstance = sdkInstance,
                keycloakRedirect = keycloakRedirect,
                incomingPush = incomingPush,
                incomingDeepLink = incomingDeepLink,
                onError = onError,
            )

            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
            ) {}
            Spacer(modifier = Modifier.height(32.dp))

            Text("Herramientas / debug", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Métodos de la SDK probados de forma aislada, fuera del flujo guiado de arriba.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(16.dp))

            CopyFcmTokenButton()

            Spacer(modifier = Modifier.height(16.dp))

            // El backend resuelve el citizen desde esta transacción (via
            // resolve_transaction_pk), así que es el único dato que se necesita a
            // mano para probar associate() de forma aislada, ver
            // .docs/sdk/cliente/04-invocacion-sdk.md.
            TextField(
                value = debugTransactionId,
                shape = MaterialTheme.shapes.small.copy(
                    bottomEnd = ZeroCornerSize, topEnd = ZeroCornerSize
                ),
                onValueChange = { debugTransactionId = it },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                label = { Text("transactionId") },
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Asociación", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AssociateDevice(sdkInstance,
                    transactionId = debugTransactionId,
                    onCompleted = { idToken, validationSessionId ->
                        Log.d("MainActivity", "ID Token: $idToken")
                        Log.d("MainActivity", "Validation Session ID: $validationSessionId")
                        debugTransactionId = ""
                    })
                AssociateViaQrScan(sdkInstance)
                CheckAssociation(sdkInstance)

                RemoveAssociation(sdkInstance)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
            ) {}
            Spacer(modifier = Modifier.height(32.dp))

            Text("Desafíos", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CreateValidationSession(sdkInstance, ChallengeType.Pin)
                Spacer(modifier = Modifier.width(16.dp))
                CreateValidationSession(sdkInstance, ChallengeType.Liveness)
                ValidateViaQrScan(sdkInstance, ChallengeType.Pin)
                Spacer(modifier = Modifier.width(16.dp))
                ValidateViaQrScan(sdkInstance, ChallengeType.Liveness)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
            ) {}
            Spacer(modifier = Modifier.height(32.dp))

            Text("Completar transacción (manual)", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            CompleteTransaction(sdkInstance)
        }
    }
}

/**
 * Obtiene y copia el token FCM de este dispositivo, para pegarlo en
 * SDK_MOCK_BQM_FCM_TEST_DEVICE_TOKEN del backend (ver sdk/firebase_mock_bqm.py
 * en id-2.0-backend) y que el mock BQM pueda enviarle un push real.
 */
@Composable
fun CopyFcmTokenButton() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    FilledTonalButton(onClick = {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Toast.makeText(
                    context, "Error al obtener el token FCM: ${task.exception?.message}", Toast.LENGTH_SHORT
                ).show()
                return@addOnCompleteListener
            }
            val token = task.result
            clipboardManager.setText(AnnotatedString(token))
            Toast.makeText(context, "Token FCM copiado al portapapeles", Toast.LENGTH_SHORT).show()
        }
    }) {
        Text("Copiar token FCM", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun AssociateDevice(
    sdkInstance: IDDigitalSDK,
    transactionId: String,
    onCompleted: (idToken: String, validationSessionId: String) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }

    fun associateDevice() {
        coroutineScope.launch {
            if (transactionId.isBlank()) return@launch
            try {
                sdkInstance.associate(context = context, transactionId = transactionId,
                    onCompleted = { idToken, validationSessionId ->
                        Toast.makeText(
                            context, "Dispositivo asociado con éxito", Toast.LENGTH_SHORT
                        ).show()
                        onCompleted(idToken, validationSessionId)
                    }, onError = {
                        Toast.makeText(
                            context, "Error al asociar dispositivo: $it", Toast.LENGTH_SHORT
                        ).show()
                    })
            } catch (e: Throwable) {
                Toast.makeText(context, "Error al asociar dispositivo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Button(
        onClick = { associateDevice() },
        enabled = transactionId.isNotBlank(),
    ) {
        Text("Asociar")
    }
}

/**
 * Fallback QR cross-device (ver .docs/sdk/cliente/08-qr-cross-device.md), probado de forma
 * aislada: associateViaQrScan() reemplaza el paso de associate() (decodifica el
 * transactionId con la cámara propia de la SDK, sin necesitar ningún dato de
 * identificación por adelantado) y hace internamente Liveness/PIN +
 * completeTransaction(); onCompleted(finishUrl) es solo informativo, nunca se abre.
 */
@Composable
fun AssociateViaQrScan(sdkInstance: IDDigitalSDK) {
    val context = LocalContext.current
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }

    fun associateViaQrScan() {
        coroutineScope.launch {
            try {
                sdkInstance.associateViaQrScan(context = context, onCompleted = { finishUrl ->
                    Toast.makeText(
                        context, "Transacción completada vía QR", Toast.LENGTH_SHORT
                    ).show()
                    Log.d("MainActivity", "QR cross-device finishUrl: $finishUrl")
                }, onError = {
                    Toast.makeText(
                        context, "Error al escanear QR: $it", Toast.LENGTH_SHORT
                    ).show()
                })
            } catch (e: Throwable) {
                Toast.makeText(context, "Error al escanear QR", Toast.LENGTH_SHORT).show()
            }
        }
    }

    FilledTonalButton(onClick = { associateViaQrScan() }) {
        Text("Asociar vía QR")
    }
}

/**
 * Camino de validación del mismo fallback QR (ver .docs/sdk/cliente/08-qr-cross-device.md),
 * probado de forma aislada: validateViaQrScan() reemplaza el paso de
 * createValidationSession() para un dispositivo ya asociado - no requiere Document, la
 * asociación local ya identifica al citizen.
 */
@Composable
fun ValidateViaQrScan(sdkInstance: IDDigitalSDK, challengeType: ChallengeType) {
    val context = LocalContext.current
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }

    fun validateViaQrScan() {
        coroutineScope.launch {
            try {
                sdkInstance.validateViaQrScan(context = context, type = challengeType, onCompleted = { finishUrl ->
                    Toast.makeText(
                        context, "Transacción completada vía QR", Toast.LENGTH_SHORT
                    ).show()
                    Log.d("MainActivity", "QR cross-device finishUrl: $finishUrl")
                }, onError = {
                    Toast.makeText(
                        context, "Error al escanear QR: $it", Toast.LENGTH_SHORT
                    ).show()
                })
            } catch (e: Throwable) {
                Toast.makeText(context, "Error al escanear QR", Toast.LENGTH_SHORT).show()
            }
        }
    }

    FilledTonalButton(onClick = { validateViaQrScan() }) {
        Text("Validar $challengeType vía QR", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun CheckAssociation(sdkInstance: IDDigitalSDK) {
    var associationValue by remember { mutableStateOf<Boolean?>(null) }
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }
    val context = LocalContext.current

    FilledTonalButton(
        onClick = {
            coroutineScope.launch {
                associationValue = sdkInstance.isAssociated()
                Toast.makeText(
                    context,
                    if (associationValue == true) "Usuario ya se encuentra asociado" else "No existe usuario asociado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
    ) {
        Text("Existe asociación?", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun RemoveAssociation(sdkInstance: IDDigitalSDK) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }
    val context = LocalContext.current

    Button(
        onClick = {
            coroutineScope.launch {
                sdkInstance.removeAssociation()
            }
            Toast.makeText(context, "Asociacion eliminada", Toast.LENGTH_SHORT).show()
        }, colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        Text("Eliminar", color = MaterialTheme.colorScheme.onError)
    }
}

@Composable
fun CompleteTransaction(sdkInstance: IDDigitalSDK) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }
    val context = LocalContext.current

    var transactionId by remember { mutableStateOf("") }
    var validationSessionId by remember { mutableStateOf("") }

    TextField(
        value = transactionId,
        onValueChange = { transactionId = it },
        label = { Text("Transaction ID") },
    )
    Spacer(modifier = Modifier.height(8.dp))
    TextField(
        value = validationSessionId,
        onValueChange = { validationSessionId = it },
        label = { Text("Validation Session ID") },
    )
    Spacer(modifier = Modifier.height(16.dp))

    // transactionId/validationSessionId provienen de una push recibida desde el bridge web
    // (ver .docs/sdk/flujo-autenticacion-unificado.md); acá se ingresan a mano solo para
    // probar el método de forma aislada.
    FilledTonalButton(
        onClick = {
            coroutineScope.launch {
                sdkInstance.completeTransaction(
                    transactionId = transactionId,
                    validationSessionId = validationSessionId,
                    onError = {
                        Toast.makeText(context, "Error: $it", Toast.LENGTH_SHORT).show()
                    },
                    onSuccess = { finishUrl ->
                        val message = if (finishUrl != null) {
                            "Transacción completada. finishUrl: $finishUrl"
                        } else {
                            "Transacción completada (sin finishUrl)"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                )
            }
        },
        enabled = transactionId.isNotEmpty() && validationSessionId.isNotEmpty(),
    ) {
        Text("Completar transacción", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun CreateValidationSession(sdkInstance: IDDigitalSDK, challengeType: ChallengeType) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }
    val context = LocalContext.current

    FilledTonalButton(onClick = {
        coroutineScope.launch {
            try {
                sdkInstance.createValidationSession(context = context,
                    type = challengeType,
                    onError = {
                        Toast.makeText(context, "Error: $it", Toast.LENGTH_SHORT).show()
                    },
                    onCompleted = { validationSessionId ->
                        Toast.makeText(context, "Validation Session ID: $validationSessionId", Toast.LENGTH_SHORT).show()
                    })
            } catch (e: Throwable) {
                Toast.makeText(
                    context, "Error al validar Liveness", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }) {
        Text("Validar $challengeType", color = MaterialTheme.colorScheme.primary)
    }
}