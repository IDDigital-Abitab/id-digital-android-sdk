package com.example.iddigital

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.iddigital.deeplink.DeepLinkPayload
import com.example.iddigital.fcm.PushPayload
import com.example.iddigital.keycloak.KeycloakRedirectResult
import uy.com.abitab.iddigitalsdk.IDDigitalClient
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Examples(
    sdkInstance: IDDigitalClient,
    onError: (IDDigitalError) -> Unit,
    keycloakRedirect: KeycloakRedirectResult? = null,
    incomingPush: PushPayload? = null,
    incomingDeepLink: DeepLinkPayload? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ID Digital — App de ejemplo") })
        },
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

            // Identifica de que build sale la APK instalada, para no confundir versiones
            // al probar en varios dispositivos.
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "${BuildConfig.APPLICATION_ID} · v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
