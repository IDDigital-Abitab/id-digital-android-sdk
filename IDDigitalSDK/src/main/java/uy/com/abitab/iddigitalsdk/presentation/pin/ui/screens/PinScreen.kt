@file:OptIn(ExperimentalMaterial3Api::class)

package uy.com.abitab.iddigitalsdk.presentation.pin.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRightAlt
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get
import uy.com.abitab.iddigitalsdk.composables.components.IDDigitalWatermark
import uy.com.abitab.iddigitalsdk.data.PinDataStoreManager
import uy.com.abitab.iddigitalsdk.ui.theme.AbitabTheme
import androidx.compose.material3.Button as MaterialButton
import androidx.core.net.toUri

const val PIN_LENGTH = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinScreen(
    onCompleted: (pin: String, saveBiometricPin: Boolean, usedBiometric: Boolean, savePinToBiometrics: Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
    onClose: () -> Unit,
    hasError: Boolean? = false,
    isCreatingNewPin: Boolean = false,
    executeChallenge: (suspend () -> Boolean?)? = null
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var pinValue by remember { mutableStateOf("") }

    val pinDataStoreManager: PinDataStoreManager = get()
    var biometricAuthStatus: BiometricAuthStatus by remember { mutableStateOf(BiometricAuthStatus.Idle) }
    var biometricShown by remember { mutableStateOf(false) }

    var isBiometricEnabledByUser by remember { mutableStateOf(false) }
    var saveBiometricPinEnabled by remember { mutableStateOf(true) }
    var showLostPinAlert by remember { mutableStateOf(false) }

    var pinRecentlyChanged by remember { mutableStateOf<Boolean?>(null) }

    val haptic = LocalHapticFeedback.current

    fun onDigitClick(value: String) {
        if (pinValue.length < PIN_LENGTH) {
            haptic.performHapticFeedback(
                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
            )
            pinValue += value
        }
    }

    fun onBackSpaceClick() {
        haptic.performHapticFeedback(
            androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
        )
        pinValue = pinValue.dropLast(1)
    }

    fun checkPin() {
        haptic.performHapticFeedback(
            androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
        )
        if (pinValue.length != PIN_LENGTH) return
        val savePinToBiometrics = pinRecentlyChanged == true && isBiometricEnabledByUser
        onCompleted(pinValue, saveBiometricPinEnabled, false, savePinToBiometrics)
        pinValue = ""
    }

    var showError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SideEffect {
        showError = hasError == true
        if (showError) {
            scope.launch {
                delay(3000)
                showError = false
            }
        }
    }

    val executor = remember { ContextCompat.getMainExecutor(context) }

    val authenticationCallback = remember {
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                scope.launch {
                    biometricAuthStatus =
                        BiometricAuthStatus.Error("Error: $errString ($errorCode)")
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                scope.launch {
                    try {
                        val decryptedPin = pinDataStoreManager.getDecryptedPin()
                        if (decryptedPin != null) {
                            biometricAuthStatus = BiometricAuthStatus.Success
                            onCompleted(decryptedPin, false, true, false)
                            pinValue = ""
                        } else {
                            biometricAuthStatus =
                                BiometricAuthStatus.Error("PIN biométrico no encontrado o corrupto.")
                            pinDataStoreManager.clearPinAndBiometricPreference()
                        }
                    } catch (e: SecurityException) {
                        biometricAuthStatus =
                            BiometricAuthStatus.Error("Error de seguridad al descifrar el PIN: ${e.message}")
                        pinDataStoreManager.clearPinAndBiometricPreference()
                    } catch (e: Exception) {
                        biometricAuthStatus =
                            BiometricAuthStatus.Error("Error inesperado al obtener PIN: ${e.message}")
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                scope.launch {
                    biometricAuthStatus =
                        BiometricAuthStatus.Error("Autenticación biométrica fallida.")
                    Toast.makeText(context, "Autenticación biométrica fallida.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }


    val biometricPrompt = remember(activity, executor, authenticationCallback) {
        BiometricPrompt(activity, executor, authenticationCallback)
    }

    val promptInfo = remember {
        val builder = BiometricPrompt.PromptInfo.Builder().setTitle("Autenticación biométrica")
            .setSubtitle("Usa tus datos biométricos para completar el PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            ).setNegativeButtonText("Cancelar")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setConfirmationRequired(true)
        }

        builder.build()
    }


    fun startBiometricAuthentication() {
        biometricAuthStatus = BiometricAuthStatus.Prompting
        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(pinRecentlyChanged) {
        isBiometricEnabledByUser = pinDataStoreManager.isBiometricPinEnabled()

        if (isCreatingNewPin || biometricShown || hasError == true || !isBiometricEnabledByUser || pinRecentlyChanged != false) {
            return@LaunchedEffect
        }
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricShown = true
            startBiometricAuthentication()
        }
    }

    LaunchedEffect(Unit) {
        if (executeChallenge == null) {
            pinRecentlyChanged = false
        }
        else if (pinRecentlyChanged == null) {
            val result = executeChallenge();
            pinRecentlyChanged = result ?: false
        }
    }

    if (showLostPinAlert) {
        LostPinAlert(onDismiss = { showLostPinAlert = false })
    }

    if (pinRecentlyChanged !== null) {
        AbitabTheme {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            if (onBack !== null) {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Volver",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Cerrar",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,

                            ),
                        scrollBehavior = scrollBehavior,
                    )
                }) { innerPadding ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            PaddingValues(
                                top = innerPadding.calculateTopPadding(),
                                bottom = 0.dp,
                            )
                        )
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 24.dp)
                ) {


                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ingresá tu PIN",
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(top = 32.dp, bottom = 50.dp)
                        )

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(4) {
                                    if (it > 0) Spacer(modifier = Modifier.width(16.dp))
                                    val backgroundColor =
                                        if (it < pinValue.length) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primaryContainer
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(backgroundColor, shape = CircleShape)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .padding(top = 20.dp)
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                AnimatedVisibility(
                                    visible = showError,
                                    enter = fadeIn(animationSpec = tween(durationMillis = 1000)) + slideInVertically(
                                        animationSpec = tween(durationMillis = 500)
                                    ),
                                    exit = fadeOut(animationSpec = tween(durationMillis = 500)) + slideOutVertically(
                                        animationSpec = tween(durationMillis = 500)
                                    )
                                ) {
                                    Text(
                                        "Pin incorrecto, inténtalo nuevamente.",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(30.dp))


                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(15.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                                        PinDigitButton(value = "1", onClick = { onDigitClick("1") })
                                        PinDigitButton(value = "2", onClick = { onDigitClick("2") })
                                        PinDigitButton(value = "3", onClick = { onDigitClick("3") })
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                                        PinDigitButton(value = "4", onClick = { onDigitClick("4") })
                                        PinDigitButton(value = "5", onClick = { onDigitClick("5") })
                                        PinDigitButton(value = "6", onClick = { onDigitClick("6") })
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                                        PinDigitButton(value = "7", onClick = { onDigitClick("7") })
                                        PinDigitButton(value = "8", onClick = { onDigitClick("8") })
                                        PinDigitButton(value = "9", onClick = { onDigitClick("9") })
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                                        if (!isCreatingNewPin && hasError == false && isBiometricEnabledByUser && pinValue.isEmpty() && pinRecentlyChanged == false) {
                                            MaterialButton(
                                                shape = CircleShape,
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                                ),
                                                modifier = Modifier.size(75.dp),
                                                onClick = { startBiometricAuthentication() }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Fingerprint,
                                                    contentDescription = "Biometría"
                                                )
                                            }
                                        } else {
                                            MaterialButton(
                                                shape = CircleShape,
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                                ),
                                                modifier = Modifier.size(75.dp),
                                                onClick = { onBackSpaceClick() }) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                                    contentDescription = "Borrar"
                                                )
                                            }
                                        }
                                        PinDigitButton(value = "0", onClick = { onDigitClick("0") })
                                        MaterialButton(
                                            shape = CircleShape,
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                            modifier = Modifier.size(75.dp),
                                            onClick = { checkPin() }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.ArrowRightAlt,
                                                contentDescription = "Borrar"
                                            )
                                        }
                                    }
                                }
                            }
                        }



                        Column {
                            if (isCreatingNewPin) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp)
                                        .align(Alignment.CenterHorizontally),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Utilizar biometría")
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Switch(checked = saveBiometricPinEnabled, onCheckedChange = {
                                        haptic.performHapticFeedback(
                                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                        )
                                        saveBiometricPinEnabled = it
                                    })
                                }
                            }
                            else if (pinRecentlyChanged == true && isBiometricEnabledByUser) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp)
                                        .align(Alignment.CenterHorizontally),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Recientemente cambiaste tu pin.\nPara poder utilizar la biometría, debes completarlo exitosamente de forma manual.",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp)
                                    .align(Alignment.CenterHorizontally),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = {
                                    showLostPinAlert = true
                                }) {
                                    Text("¿Olvidaste tu PIN?")
                                }
                            }
                        }
                    }


                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )

                    IDDigitalWatermark()

                    Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
                }
            }
        }
    }
}

@Composable
fun PinDigitButton(value: String, onClick: (value: String) -> Unit) {
    return MaterialButton(
        shape = ButtonDefaults.filledTonalShape,
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = Modifier.size(75.dp),
        onClick = {
            onClick(value)
        }) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

@Preview(showBackground = true, device = "id:pixel_8_pro", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, device = "id:pixel_8_pro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LostPinAlertPreview() {
    LostPinAlert(onDismiss = {})
}

@Composable
fun LostPinAlert(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val intent = remember { Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=uy.com.abitab.iddigital.prod".toUri()) }

    AbitabTheme {
        Dialog(onDismissRequest = { onDismiss() }) {
            Card(
                modifier = Modifier
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp).padding(top = 16.dp)
                ) {
                    Text(
                        text = "¿Olvidaste tu PIN?",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Si olvidaste tu PIN podés reiniciarlo desde la app de ID Digital",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    FilledTonalButton(onClick = {
                        context.startActivity(intent)
                    }) {
                        Text(text = "Abrir app ID Digital")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        onDismiss()
                    }) {
                        Text(text = "Cancelar", color = MaterialTheme.colorScheme.primary)
                    }

                }
            }
        }
    }


}

sealed class BiometricAuthStatus {
    object Idle : BiometricAuthStatus()
    object Prompting : BiometricAuthStatus()
    object Success : BiometricAuthStatus()
    data class Error(val message: String) : BiometricAuthStatus()
}