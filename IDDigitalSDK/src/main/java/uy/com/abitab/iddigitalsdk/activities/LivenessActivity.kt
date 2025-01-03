package uy.com.abitab.iddigitalsdk.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException
import com.amplifyframework.ui.liveness.ui.FaceLivenessDetector
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import uy.com.abitab.iddigitalsdk.CallbackHandler
import uy.com.abitab.iddigitalsdk.Document
import uy.com.abitab.iddigitalsdk.GENERIC_ERROR_MESSAGE
import uy.com.abitab.iddigitalsdk.IDDigitalError
import uy.com.abitab.iddigitalsdk.PermissionsManager
import uy.com.abitab.iddigitalsdk.PermissionsManager.registerPermissionLauncher
import uy.com.abitab.iddigitalsdk.R
import uy.com.abitab.iddigitalsdk.composables.AbitabTheme
import uy.com.abitab.iddigitalsdk.composables.InstructionsScreen
import uy.com.abitab.iddigitalsdk.composables.PostLivenessProcessing
import uy.com.abitab.iddigitalsdk.network.LivenessService
import java.io.IOException


class LivenessActivity : ComponentActivity() {
    private lateinit var livenessService: LivenessService
    private var accessToken: String? = null

    private val permissionResultChannel = Channel<Boolean>(Channel.CONFLATED)


    inline fun <reified T> Gson.fromJson(json: String?): T? {
        return fromJson(json, T::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureSystemUI()

        accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN)

        val document = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_DOCUMENT, Document::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getSerializableExtra(EXTRA_DOCUMENT) as? Document
        }

        if (accessToken == null || document == null) {
            CallbackHandler.onError(
                IDDigitalError.WrongDataError(
                    "No se ingresó un documento, o el mismo no es válido.",
                    null
                )
            )
            finish()
            return
        }
        livenessService = LivenessService(accessToken!!)

        registerPermissionLauncher(this)

        setContent {
            var showInstructions by remember { mutableStateOf(true) }

            if (showInstructions) {
                InstructionsScreen(onStart = {
                    showInstructions = false
                    startLivenessFlow(document)
                }, onBack = { finish() })
            } else {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading))

                Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    LottieAnimation(
                        composition,
                        modifier = Modifier
                            .width(50.dp)
                            .height(50.dp),
                        iterations = LottieConstants.IterateForever
                    )
                }
            }
        }
    }

    private fun configureSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = true

        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun startLivenessFlow(document: Document) {
        lifecycleScope.launch {
            if (!PermissionsManager.hasCameraPermission(this@LivenessActivity)) {

                launch {
                    val isGranted =
                        PermissionsManager.requestCameraPermission(this@LivenessActivity)
                    permissionResultChannel.send(isGranted)
                }

                val isGranted = permissionResultChannel.receive()
                if (!isGranted) {
                    CallbackHandler.onError(
                        IDDigitalError.CameraPermissionError(
                            "Permiso de cámara denegado.",
                            null
                        )
                    )
                    finish()
                    return@launch
                }
            }

            try {
                val challengeId = livenessService.createChallenge(document)
                val sessionId = livenessService.executeChallenge(challengeId)


                setContent {
                    AbitabTheme {
                        Box(
                            modifier = Modifier
                                .background(Color.Black)
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            FaceLivenessDetector(sessionId = sessionId,
                                region = "us-east-1",
                                disableStartView = true,
                                onComplete = {
                                    lifecycleScope.launch {
                                        livenessService.validateChallenge(challengeId)
                                        CallbackHandler.onCompleted(challengeId)
                                        finish()
                                    }
                                    setContent {
                                        PostLivenessProcessing()
                                    }
                                },
                                onError = { error ->
                                    when (error) {
                                        is FaceLivenessDetectionException.UserCancelledException ->
                                            CallbackHandler.onError(
                                                IDDigitalError.UserCancelledError(
                                                    "El usuario canceló la validación",
                                                )
                                            )

                                        is FaceLivenessDetectionException.CameraPermissionDeniedException ->
                                            CallbackHandler.onError(
                                                IDDigitalError.CameraPermissionError(
                                                    "Permiso de cámara denegado.",
                                                    error.throwable
                                                )
                                            )

                                        else -> CallbackHandler.onError(
                                            IDDigitalError.UnknownError(
                                                GENERIC_ERROR_MESSAGE,
                                                error.throwable
                                            )
                                        )
                                    }
                                    finish()
                                })
                        }
                    }
                }
            } catch (e: IOException) {
                CallbackHandler.onError(
                    IDDigitalError.NetworkError(
                        "Ha ocurrido un error de red. Por favor, intenta nuevamente.",
                        e
                    )
                )
                finish()
            } catch (e: Exception) {
                CallbackHandler.onError(
                    IDDigitalError.UnknownError(GENERIC_ERROR_MESSAGE)
                )
                finish()
            }
        }
    }

    companion object {
        private const val EXTRA_ACCESS_TOKEN = "EXTRA_ACCESS_TOKEN"
        private const val EXTRA_DOCUMENT = "EXTRA_DOCUMENT"

        fun createIntent(
            context: Context,
            accessToken: String,
            document: Document,

            ): Intent {
            return Intent(context, LivenessActivity::class.java).apply {
                putExtra(EXTRA_ACCESS_TOKEN, accessToken)
                putExtra(EXTRA_DOCUMENT, document)
            }
        }
    }
}