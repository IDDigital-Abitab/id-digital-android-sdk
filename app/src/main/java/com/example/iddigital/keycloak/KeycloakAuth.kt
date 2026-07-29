package com.example.iddigital.keycloak

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.example.iddigital.BuildConfig
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

/**
 * Dispara el tramo Keycloak del Patron B (puente web): abre el `authorize` endpoint
 * del realm configurado (broker hacia ID Digital) en un navegador in-app, para que el
 * backend de ID Digital cree la TransactionOIDC pendiente que despues se resuelve a mano
 * con el flujo guiado (ver PendingVerificationFlow).
 *
 * La app no hace exchange de tokens: solo dispara el login y confirma el retorno via
 * deep link, ver KeycloakRedirect / MainActivity.onNewIntent.
 */
object KeycloakAuth {

    data class PendingLogin(val state: String, val codeVerifier: String)

    fun isConfigured(): Boolean =
        BuildConfig.KEYCLOAK_CLIENT_ID.isNotBlank() && BuildConfig.KEYCLOAK_REDIRECT_URI.isNotBlank()

    /**
     * Abre el login de Keycloak en un Custom Tab. Devuelve el [PendingLogin] generado
     * (state + code_verifier) para que el caller lo pueda mostrar/loguear si lo necesita;
     * esta app de ejemplo no hace exchange de tokens, por lo que no valida el `state` al volver.
     */
    fun launch(context: Context): PendingLogin {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = deriveCodeChallenge(codeVerifier)
        val state = generateState()

        val authorizeUri = BuildConfig.KEYCLOAK_BASE_URL.toUri().buildUpon()
            .appendEncodedPath("realms/${BuildConfig.KEYCLOAK_REALM}/protocol/openid-connect/auth")
            .appendQueryParameter("client_id", BuildConfig.KEYCLOAK_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.KEYCLOAK_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "openid")
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()

        CustomTabsIntent.Builder().build().launchUrl(context, authorizeUri)

        return PendingLogin(state = state, codeVerifier = codeVerifier)
    }

    private fun generateState(): String = generateRandomUrlSafeString(16)

    private fun generateCodeVerifier(): String = generateRandomUrlSafeString(32)

    private fun generateRandomUrlSafeString(byteLength: Int): String {
        val bytes = ByteArray(byteLength)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun deriveCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}

/** Resultado de parsear el deep link de retorno de Keycloak/ID Digital. */
sealed class KeycloakRedirectResult {
    data class Success(val code: String, val state: String?) : KeycloakRedirectResult()
    data class Error(val error: String, val description: String?) : KeycloakRedirectResult()
}

/** Parsea el Uri recibido en el deep link `KEYCLOAK_REDIRECT_URI` (ver AndroidManifest). */
fun parseKeycloakRedirect(uri: Uri): KeycloakRedirectResult? {
    val code = uri.getQueryParameter("code")
    val error = uri.getQueryParameter("error")
    return when {
        code != null -> KeycloakRedirectResult.Success(code, uri.getQueryParameter("state"))
        error != null -> KeycloakRedirectResult.Error(error, uri.getQueryParameter("error_description"))
        else -> null
    }
}

/**
 * Sesion de Keycloak decodificada del `state` que arma BQMAuthenticator antes de mandar al
 * usuario a ID Digital (ver BQMAuthenticator.java: `params = "session_code=...&execution=...
 * &client_id=...&tab_id=..."`, luego Base64). Es la contraparte de
 * BqmResourceProvider.digitarIdDecoder, pero corriendo del lado de la app en vez de un
 * endpoint propio de Keycloak - ver .docs/sdk/... (diagrama de este flujo, sesion de chat).
 */
data class IdDigitalKeycloakCallback(
    val code: String,
    val sessionCode: String,
    val executionId: String,
    val clientId: String,
    val tabId: String,
)

private const val ID_DIGITAL_CALLBACK_PATH = "/user/redirectKc"

/**
 * Detecta el retorno del boton "Id Digital" para clientes en una lista tipo `MOBILE_CLIENTS`
 * de un Keycloak de terceros. **No es el camino estandar del SDK** (ver
 * .docs/sdk/cliente/02-configuracion-keycloak.md): el client_id que usa el flujo SDK no
 * deberia estar en esa lista, ya que el `redirect_uri` HTTPS normal (`finishUrl` =
 * `bqm-resource-provider/idDigital`) ya funciona sin ningun handler adicional - ver
 * PendingVerificationFlow. Esta funcion queda como referencia para el caso en que un
 * Keycloak de terceros fuerce este patron igualmente. Devuelve null si [uri] no matchea este
 * path, para poder probarlo contra cualquier Uri sin checks previos (p.ej. el retorno directo
 * de Keycloak que atiende parseKeycloakRedirect, o un finishUrl https).
 */
fun parseIdDigitalCallback(uri: Uri): IdDigitalKeycloakCallback? {
    if (uri.path != ID_DIGITAL_CALLBACK_PATH) return null
    val code = uri.getQueryParameter("code") ?: return null
    val state = uri.getQueryParameter("state") ?: return null
    val decoded = runCatching { String(Base64.decode(state, Base64.DEFAULT), Charsets.UTF_8) }
        .getOrNull() ?: return null
    val params = decoded.split("&").mapNotNull { pair ->
        val parts = pair.split("=", limit = 2)
        if (parts.size == 2) parts[0] to parts[1] else null
    }.toMap()
    val sessionCode = params["session_code"] ?: return null
    val executionId = params["execution"] ?: return null
    val clientId = params["client_id"] ?: return null
    val tabId = params["tab_id"] ?: return null
    return IdDigitalKeycloakCallback(code, sessionCode, executionId, clientId, tabId)
}

/**
 * Solo aplica al caso no estandar de [parseIdDigitalCallback] (client en una lista tipo
 * `MOBILE_CLIENTS`). Arma la URL para retomar la AuthenticationSessionModel pendiente en
 * Keycloak - el mismo GET que hace BqmResourceProvider.digitarIdDecoder
 * (`login-actions/authenticate?session_code=...&execution=...&tab_id=...&code=...`).
 *
 * Importante: esto hay que abrirlo en un navegador (Custom Tab, ver MainActivity), no pedirlo
 * con un HTTP client propio de la app. `login-actions/authenticate` exige, ademas de estos
 * query params, la cookie de sesion (`AUTH_SESSION_ID`/`KC_RESTART`) que Keycloak seteo en el
 * Custom Tab que arranco el login (KeycloakAuth.launch) - un HTTP client sin esa cookie
 * recibe 400 `cookie_not_found` (confirmado en logs de Keycloak, no llega ni a ejecutar
 * BQMAuthenticator). Los Custom Tabs comparten cookie jar con Chrome, asi que abrir esta URL
 * ahi si la lleva. El resultado final (exito o error) le llega a la app mas tarde como un
 * intent nuevo al `KEYCLOAK_REDIRECT_URI` real del cliente - lo sigue manejando
 * parseKeycloakRedirect, sin cambios.
 */
fun buildIdDigitalResumeUri(callback: IdDigitalKeycloakCallback): Uri =
    BuildConfig.KEYCLOAK_BASE_URL.toUri().buildUpon()
        .appendEncodedPath("realms/${BuildConfig.KEYCLOAK_REALM}/login-actions/authenticate")
        .appendQueryParameter("session_code", callback.sessionCode)
        .appendQueryParameter("execution", callback.executionId)
        .appendQueryParameter("client_id", callback.clientId)
        .appendQueryParameter("tab_id", callback.tabId)
        .appendQueryParameter("code", callback.code)
        .build()
