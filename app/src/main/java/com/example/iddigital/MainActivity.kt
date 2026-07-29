package com.example.iddigital

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.iddigital.deeplink.DeepLinkPayload
import com.example.iddigital.deeplink.IncomingDeepLink
import com.example.iddigital.deeplink.deepLinkPayloadFromIntent
import com.example.iddigital.fcm.IncomingPush
import com.example.iddigital.fcm.PushPayload
import com.example.iddigital.fcm.pushPayloadFromIntent
import com.example.iddigital.keycloak.KeycloakRedirectResult
import com.example.iddigital.keycloak.buildIdDigitalResumeUri
import com.example.iddigital.keycloak.parseIdDigitalCallback
import com.example.iddigital.keycloak.parseKeycloakRedirect
import uy.com.abitab.iddigitalsdk.IDDigitalSDK
import uy.com.abitab.iddigitalsdk.domain.models.IDDigitalSDKEnvironment
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError

class MainActivity : ComponentActivity() {
    private lateinit var sdkInstance: IDDigitalSDK
    private var keycloakRedirect = mutableStateOf<KeycloakRedirectResult?>(null)

    private val notificationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()
        handleIntent(intent)

        val apiKey = BuildConfig.API_KEY
        try {
            sdkInstance = IDDigitalSDK.initialize(
                this,
                apiKey,
                environment = IDDigitalSDKEnvironment.STAGING,
                onError = {},
                onCompleted = {},
                baseUrl = BuildConfig.API_BASE_URL.ifBlank { null },
            )

            setContent {
                MainScreen(
                    sdkInstance = sdkInstance,
                    keycloakRedirect = keycloakRedirect.value,
                    incomingPush = IncomingPush.current.value,
                    incomingDeepLink = IncomingDeepLink.current.value,
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing IDDigitalSDK: ${e.message}", Toast.LENGTH_LONG)
                .show()
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Detecta el deep link de retorno de Keycloak (ver KeycloakAuth.kt), el caso no estandar
     * del boton "Id Digital" para clientes en una lista tipo MOBILE_CLIENTS de un Keycloak de
     * terceros (ver parseIdDigitalCallback - llega asi solo si ese Keycloak fuerza ese
     * patron; no aplica con la configuracion recomendada, ver
     * .docs/sdk/cliente/02-configuracion-keycloak.md), el deep link same-device del puente
     * web (ver .docs/sdk/cliente/07-deep-link-same-device.md) y, si la app se abrió tocando
     * la notificación del push cross-device (ver IDDigitalSampleFcmService), los datos de
     * esa transacción pendiente.
     */
    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri != null) {
            val idDigitalCallback = parseIdDigitalCallback(uri)
            if (idDigitalCallback != null) {
                // Caso no estandar (ver KDoc de handleIntent) - se abre en un Custom Tab
                // (no con un HTTP client propio) porque login-actions/authenticate exige la
                // cookie de sesion que Keycloak seteo en el Custom Tab del login original;
                // sin ella devuelve 400 cookie_not_found (ver
                // KeycloakAuth.buildIdDigitalResumeUri). El resultado final llega mas tarde
                // como un intent nuevo a KEYCLOAK_REDIRECT_URI, que cae en el "else" de abajo.
                CustomTabsIntent.Builder().build()
                    .launchUrl(this, buildIdDigitalResumeUri(idDigitalCallback))
            } else {
                parseKeycloakRedirect(uri)?.let { keycloakRedirect.value = it }
            }
        }
        pushPayloadFromIntent(intent)?.let { IncomingPush.current.value = it }
        deepLinkPayloadFromIntent(intent)?.let { IncomingDeepLink.current.value = it }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val alreadyGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!alreadyGranted) {
            notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    sdkInstance: IDDigitalSDK,
    keycloakRedirect: KeycloakRedirectResult? = null,
    incomingPush: PushPayload? = null,
    incomingDeepLink: DeepLinkPayload? = null,
) {
    val context = LocalContext.current
    fun handleIDDigitalSdkError(error: IDDigitalError) {
        Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
    }

    MaterialTheme {
        Scaffold(topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { Text("ID Digital — App de ejemplo", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {},
                actions = {},
            )
        }) {
            Examples(
                sdkInstance = sdkInstance,
                keycloakRedirect = keycloakRedirect,
                incomingPush = incomingPush,
                incomingDeepLink = incomingDeepLink,
                onError = {
                    handleIDDigitalSdkError(it)
                }
            )
        }
    }
}

