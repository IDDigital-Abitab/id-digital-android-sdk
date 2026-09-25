package com.example.iddigital

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.iddigital.deeplink.DeepLinkPayload
import com.example.iddigital.fcm.PushPayload
import com.example.iddigital.keycloak.KeycloakAuth
import com.example.iddigital.keycloak.KeycloakRedirectResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uy.com.abitab.iddigitalsdk.IDDigitalClient
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError

/**
 * Tipo de aviso recibido, por push (ver .docs/sdk/cliente/03-endpoint-push.md) o por
 * deep link same-device (ver .docs/sdk/cliente/01-arquitectura-y-flujos.md). El push
 * llega con el tipo ya resuelto por el backend del integrador; el deep link solo trae
 * transactionId, así que el tipo se decide localmente con sdkInstance.isAssociated().
 */
private enum class PendingVerificationType {
    Association,
    Validation,
}

private sealed class StepState {
    data object Idle : StepState()
    data object Running : StepState()
    data object Done : StepState()
    data class Failed(val message: String) : StepState()
}

/**
 * Login Keycloak del Patron B: abre el authorize del realm configurado en un Custom Tab.
 * El backend de ID Digital crea ahi la TransactionOIDC pendiente; el transactionId llega
 * por push o deep link (LaunchedEffect mas abajo) o se resuelve con el fallback QR.
 */
@Composable
private fun KeycloakLoginSection(keycloakRedirect: KeycloakRedirectResult?) {
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Abre el login de Keycloak. El backend de ID Digital va a crear ahí la " +
                "transacción pendiente; esta app la resuelve por push, deep link o el QR de abajo.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (!KeycloakAuth.isConfigured()) {
                    Toast.makeText(
                        context,
                        "Falta configurar KEYCLOAK_CLIENT_ID/KEYCLOAK_REDIRECT_URI en local.properties",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }
                KeycloakAuth.launch(context)
            },
        ) {
            Text("Iniciar sesión con Keycloak")
        }
        keycloakRedirect?.let {
            Spacer(modifier = Modifier.height(8.dp))
            // Mismo estado sirve para dos casos: (1) retorno directo de este login de
            // prueba (Custom Tab, sin boton "Id Digital"), o (2) retorno del boton "Id
            // Digital" luego de que Keycloak completa el flujo pendiente (ver
            // MainActivity.handleIntent → buildIdDigitalResumeUri, abierto en un Custom
            // Tab). En ambos, "code" es lo que Keycloak devolvió al redirect_uri real del
            // cliente.
            when (it) {
                is KeycloakRedirectResult.Success ->
                    Text(
                        "Keycloak devolvió code=${it.code.take(8)}…",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                is KeycloakRedirectResult.Error ->
                    Text(
                        "Keycloak devolvió un error: ${it.error} ${it.description ?: ""}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
            }
        }
    }
}

/**
 * Flujo guiado del tramo "puente web": recibe el transactionId/type que en producción
 * llegan por push (ver .docs/sdk/cliente/03-endpoint-push.md) — automáticamente si llega
 * un [incomingPush] (ver IDDigitalSampleFcmService) o un [incomingDeepLink] same-device
 * (ver .docs/sdk/cliente/01-arquitectura-y-flujos.md) — y orquesta asociación o validación
 * seguida de completeTransaction(), igual que el código de referencia de
 * .docs/sdk/cliente/04-integracion-sdk.md. El backend resuelve al ciudadano desde esta
 * transacción, así que no hace falta ningún dato de documento acá. Al completar, si el
 * backend devolvió finishUrl, lo abre con el abridor de URLs del sistema en vez de
 * depender del polling del browser en background.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingVerificationFlow(
    sdkInstance: IDDigitalClient,
    keycloakRedirect: KeycloakRedirectResult?,
    onError: (IDDigitalError) -> Unit,
    incomingPush: PushPayload? = null,
    incomingDeepLink: DeepLinkPayload? = null,
) {
    val context = LocalContext.current
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }

    var transactionId by remember { mutableStateOf("") }
    var pendingType by remember { mutableStateOf(PendingVerificationType.Association) }
    var challengeType by remember { mutableStateOf(ChallengeType.Pin) }
    var qrChallengeType by remember { mutableStateOf(ChallengeType.Pin) }

    var resolveStepState by remember { mutableStateOf<StepState>(StepState.Idle) }
    var completeStepState by remember { mutableStateOf<StepState>(StepState.Idle) }
    var qrStepState by remember { mutableStateOf<StepState>(StepState.Idle) }

    // Enruta el fallback QR igual que el deep link same-device más abajo: según el estado
    // local del dispositivo, no según si la transacción pendiente llegó por push. Se
    // refresca tras cada asociación exitosa (ver refreshDeviceAssociation) para no cachear
    // un valor stale si el usuario asocia y valida en la misma sesión.
    var isDeviceAssociated by remember { mutableStateOf(false) }

    suspend fun refreshDeviceAssociation() {
        isDeviceAssociated = sdkInstance.isAssociated()
    }

    val isRunning = resolveStepState is StepState.Running || completeStepState is StepState.Running ||
        qrStepState is StepState.Running
    val canScanQr = !isRunning

    fun completeTransaction(validationSessionId: String, openFinishUrl: Boolean) {
        completeStepState = StepState.Running
        coroutineScope.launch {
            sdkInstance.completeTransaction(
                transactionId = transactionId,
                validationSessionId = validationSessionId,
                onError = {
                    completeStepState = StepState.Failed(it.message ?: "Error")
                    onError(it)
                },
                onSuccess = { finishUrl ->
                    completeStepState = StepState.Done
                    Toast.makeText(context, "Transacción completada", Toast.LENGTH_SHORT).show()
                    // Solo same-device (deep link, ver openFinishUrl en los callers):
                    // abrimos finishUrl nosotros en vez de depender del tab del
                    // browser, ver .docs/sdk/cliente/01-arquitectura-y-flujos.md. Un push real
                    // puede ser cross-device - ahí el browser original es el único que tiene
                    // la cookie de sesión correcta para login-actions/authenticate en Keycloak,
                    // y su propio polling ya hace este mismo redirect; si el teléfono también
                    // abriera finishUrl, competiría por la misma AuthenticationSessionModel de
                    // un solo uso y el que pierda la carrera ve "You are already logged in" en
                    // vez de completar nada. finishUrl viene null si el backend no pudo
                    // generarlo (ver sdk/views.py complete_oidc_transaction); en ese caso el
                    // polling de la SPA sigue siendo el fallback.
                    if (openFinishUrl) {
                        finishUrl?.let {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                        }
                    }
                }
            )
        }
    }

    fun resolveAndComplete(openFinishUrl: Boolean) {
        resolveStepState = StepState.Running
        completeStepState = StepState.Idle
        coroutineScope.launch {
            when (pendingType) {
                PendingVerificationType.Association -> {
                    try {
                        sdkInstance.associate(
                            context = context,
                            transactionId = transactionId,
                            onError = {
                                resolveStepState = StepState.Failed(it.message ?: "Error")
                                onError(it)
                            },
                            onCompleted = { _, validationSessionId ->
                                resolveStepState = StepState.Done
                                completeTransaction(validationSessionId, openFinishUrl)
                                coroutineScope.launch { refreshDeviceAssociation() }
                            }
                        )
                    } catch (e: Throwable) {
                        Log.e(SDK_LOG_TAG, "associate() failed: ${e.message}", e)
                        resolveStepState = StepState.Failed(e.message ?: "Error al asociar dispositivo")
                    }
                }

                PendingVerificationType.Validation -> {
                    sdkInstance.createValidationSession(
                        context = context,
                        type = challengeType,
                        onError = {
                            resolveStepState = StepState.Failed(it.message ?: "Error")
                            onError(it)
                        },
                        onCompleted = { validationSessionId ->
                            resolveStepState = StepState.Done
                            completeTransaction(validationSessionId, openFinishUrl)
                        }
                    )
                }
            }
        }
    }

    // Fallback QR cross-device (ver .docs/sdk/cliente/01-arquitectura-y-flujos.md): el SPA lo
    // ofrece cuando el push (de asociación o de validación) no se pudo confirmar entregado
    // (sdk_push_failed=true). associateViaQrScan()/validateViaQrScan() reemplazan el paso
    // de asociar()/createValidationSession() — decodifican el transactionId desde la
    // cámara propia de la SDK y hacen internamente Liveness/PIN + completeTransaction(),
    // así que no hay nada más para orquestar acá. Siempre cross-device: nunca se abre
    // finishUrl, el browser del otro dispositivo cierra por su propio polling. No requiere
    // ningún dato de identificación por adelantado: el backend resuelve al citizen desde la
    // transacción codificada en el QR recién al escanearlo.
    fun associateViaQrScan() {
        qrStepState = StepState.Running
        coroutineScope.launch {
            try {
                sdkInstance.associateViaQrScan(
                    context = context,
                    onError = {
                        qrStepState = StepState.Failed(it.message ?: "Error")
                        onError(it)
                    },
                    onCompleted = { finishUrl ->
                        qrStepState = StepState.Done
                        Toast.makeText(context, "Transacción completada vía QR", Toast.LENGTH_SHORT).show()
                        Log.d("PendingVerificationFlow", "QR cross-device finishUrl: $finishUrl")
                        coroutineScope.launch { refreshDeviceAssociation() }
                    }
                )
            } catch (e: Throwable) {
                Log.e(SDK_LOG_TAG, "associateViaQrScan() failed: ${e.message}", e)
                qrStepState = StepState.Failed(e.message ?: "Error al escanear QR")
            }
        }
    }

    // Camino de validación del mismo fallback: se usa en vez de associateViaQrScan()
    // cuando el dispositivo ya está asociado localmente (isDeviceAssociated) - no requiere
    // Document, la asociación local ya identifica al citizen.
    fun validateViaQrScan() {
        qrStepState = StepState.Running
        coroutineScope.launch {
            try {
                sdkInstance.validateViaQrScan(
                    context = context,
                    type = qrChallengeType,
                    onError = {
                        qrStepState = StepState.Failed(it.message ?: "Error")
                        onError(it)
                    },
                    onCompleted = { finishUrl ->
                        qrStepState = StepState.Done
                        Toast.makeText(context, "Transacción completada vía QR", Toast.LENGTH_SHORT).show()
                        Log.d("PendingVerificationFlow", "QR cross-device finishUrl: $finishUrl")
                    }
                )
            } catch (e: Throwable) {
                Log.e(SDK_LOG_TAG, "validateViaQrScan() failed: ${e.message}", e)
                qrStepState = StepState.Failed(e.message ?: "Error al escanear QR")
            }
        }
    }

    fun checkAssociation() {
        coroutineScope.launch {
            refreshDeviceAssociation()
            Toast.makeText(
                context,
                if (isDeviceAssociated) "Usuario ya se encuentra asociado" else "No existe usuario asociado",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    // removeAssociation() borra las dos puntas: hace DELETE associations/ contra el backend
    // y despues limpia la asociacion local y el PIN/biometria (ver IDDigitalClient). Ojo: el
    // SDK se traga el error del DELETE (solo Log.e) y limpia lo local igual, asi que si la
    // llamada falla el dispositivo queda sin asociacion local pero el backend la conserva.
    // Refresca isDeviceAssociated para que el fallback QR vuelva al camino de asociacion.
    fun removeAssociation() {
        coroutineScope.launch {
            sdkInstance.removeAssociation()
            refreshDeviceAssociation()
            Toast.makeText(context, "Asociación eliminada", Toast.LENGTH_SHORT).show()
        }
    }

    // Al llegar un push real (ver IDDigitalSampleFcmService), completa transactionId y
    // resuelve la transacción sola, sin necesidad de copiar/pegar nada a mano. El backend
    // resuelve al citizen desde esta transacción (via
    // resolve_transaction_pk), así que no hace falta ningún dato de documento acá -
    // documentNumber/Type/Country del payload solo le sirven al backend del Integrador para
    // decidir a qué dispositivo notificar (.docs/sdk/cliente/03-endpoint-push.md), no a la
    // SDK. openFinishUrl=false: un push puede ser cross-device (el caso típico, ver
    // .docs/sdk/cliente/01-arquitectura-y-flujos.md), y el browser original ya completa el
    // login por su propio polling — abrir finishUrl también desde el teléfono compite por la
    // misma sesión de Keycloak (ver comentario en completeTransaction más abajo).
    LaunchedEffect(incomingPush) {
        val push = incomingPush ?: return@LaunchedEffect
        transactionId = push.transactionId
        pendingType = if (push.type == "association") {
            PendingVerificationType.Association
        } else {
            PendingVerificationType.Validation
        }
        resolveAndComplete(openFinishUrl = false)
    }

    // Al llegar el deep link same-device (ver MainActivity.handleIntent), la app decide
    // localmente si el paso pendiente es asociación o validación, y resuelve de una - el
    // transactionId del deep link (ver IncomingDeepLink.kt) alcanza para ambos
    // casos, sin necesitar ningún dato de documento.
    //
    // Workaround de prueba (NO es el criterio de IDAPP-1002/1004): openFinishUrl=false.
    // Hoy el backend genera un authorization code tanto en complete_oidc_transaction
    // (cuando la app llama completeTransaction) como en cada poll de la SPA
    // (complete_transaction). Keycloak consume el primero y rechaza el segundo.
    // Hasta que el backend no deje de emitir finishUrl a la SPA en same-device,
    // redirige solo la SPA. Ver FIXES.md § Backend.
    LaunchedEffect(incomingDeepLink) {
        val link = incomingDeepLink ?: return@LaunchedEffect
        transactionId = link.transactionId
        refreshDeviceAssociation()
        pendingType = if (isDeviceAssociated) {
            PendingVerificationType.Validation
        } else {
            PendingVerificationType.Association
        }
        resolveAndComplete(openFinishUrl = false)
    }

    // Los pasos que abren una pantalla de la SDK quedan en Running si el ciudadano abandona
    // el flujo, porque la SDK solo invoca los callbacks al completar o al fallar. Al volver
    // al foreground, un Running que sobrevivió significa flujo abandonado: se descarta para
    // no dejar la pantalla entera deshabilitada. Un flujo que terminó bien ya pasó a
    // Done/Failed, así que no se toca.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (resolveStepState is StepState.Running) resolveStepState = StepState.Idle
                if (qrStepState is StepState.Running) qrStepState = StepState.Idle
                coroutineScope.launch { refreshDeviceAssociation() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        KeycloakLoginSection(keycloakRedirect)

        Spacer(modifier = Modifier.height(32.dp))

        Text("Fallback QR cross-device", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Independiente de todo lo anterior: no requiere transactionId ni haber " +
                "recibido una push. El SPA ofrece este QR en la pantalla de espera " +
                "cuando la push (de asociación o de validación) no se pudo confirmar " +
                "entregada (sdk_push_failed) — ver .docs/sdk/cliente/01-arquitectura-y-flujos.md. " +
                "El camino se decide según el estado local del dispositivo, no según el " +
                "tipo de transacción pendiente: si ya está asociado, se valida; si no, se " +
                "asocia. La SDK escanea el token con su propia cámara y hace todo el resto " +
                "internamente (Liveness/PIN + completeTransaction).",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isDeviceAssociated) {
            Text(
                "Dispositivo asociado: se valida en vez de asociar.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                FilterChip(
                    selected = qrChallengeType == ChallengeType.Pin,
                    onClick = { qrChallengeType = ChallengeType.Pin },
                    enabled = !isRunning,
                    label = { Text("Pin") },
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = qrChallengeType == ChallengeType.Liveness,
                    onClick = { qrChallengeType = ChallengeType.Liveness },
                    enabled = !isRunning,
                    label = { Text("Liveness") },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(
                onClick = { validateViaQrScan() },
                enabled = canScanQr,
            ) {
                Text("Escanear QR (validación)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            StepRow(label = "Escanear QR, validar y completar transacción", state = qrStepState)
        } else {
            Text(
                "Dispositivo sin asociación local: no hace falta ningún dato de " +
                    "documento acá - el backend resuelve al ciudadano desde la " +
                    "transacción codificada en el QR recién al escanearlo.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(
                onClick = { associateViaQrScan() },
                enabled = canScanQr,
            ) {
                Text("Escanear QR (asociación)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            StepRow(label = "Escanear QR, asociar y completar transacción", state = qrStepState)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Asociación", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "\"Existe asociación?\" consulta solo el estado local del dispositivo. " +
                "\"Eliminar\" borra las dos puntas: la DeviceAssociation del backend, la " +
                "asociación local y el PIN/biometría guardados. Después de eliminar, el " +
                "fallback QR de arriba vuelve al camino de asociación.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(
            onClick = { checkAssociation() },
            enabled = !isRunning,
        ) {
            Text("Existe asociación?")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { removeAssociation() },
            enabled = !isRunning,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
        ) {
            Text("Eliminar", color = MaterialTheme.colorScheme.onError)
        }
    }
}

@Composable
private fun StepRow(label: String, state: StepState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val (icon, tint) = when (state) {
            is StepState.Idle -> Icons.Filled.RadioButtonUnchecked to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            is StepState.Running -> Icons.Filled.HourglassEmpty to MaterialTheme.colorScheme.primary
            is StepState.Done -> Icons.Filled.CheckCircle to Color(0xFF2E7D32)
            is StepState.Failed -> Icons.Filled.Error to MaterialTheme.colorScheme.error
        }
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            if (state is StepState.Failed) "$label — ${state.message}" else label,
            color = tint,
        )
    }
}
