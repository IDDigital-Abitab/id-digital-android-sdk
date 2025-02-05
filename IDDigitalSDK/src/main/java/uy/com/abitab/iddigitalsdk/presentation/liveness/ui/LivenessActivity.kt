package uy.com.abitab.iddigitalsdk.presentation.liveness.ui

import FaceLivenessComponent
import LoadingScreen
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import uy.com.abitab.iddigitalsdk.CallbackHandler
import uy.com.abitab.iddigitalsdk.GENERIC_ERROR_MESSAGE
import uy.com.abitab.iddigitalsdk.IDDigitalError
import uy.com.abitab.iddigitalsdk.data.network.LivenessError
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.screens.LivenessInstructionsScreen
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.screens.LivenessCompletedLoadingScreen
import uy.com.abitab.iddigitalsdk.data.network.LivenessService
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.viewmodels.LivenessUiState
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.viewmodels.LivenessViewModel
import uy.com.abitab.iddigitalsdk.utils.PermissionsManager.registerPermissionLauncher
import java.io.IOException

class LivenessActivity : ComponentActivity() {
    private val livenessService: LivenessService by inject()
    private val viewModel: LivenessViewModel by viewModel()

    inline fun <reified T> Gson.fromJson(json: String?): T? {
        return fromJson(json, T::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureSystemUI()

        val document = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_DOCUMENT, Document::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getSerializableExtra(EXTRA_DOCUMENT) as? Document
        }

        if (document == null) {
            CallbackHandler.onError(
                IDDigitalError.WrongDataError(
                    "No se ingresó un documento, o el mismo no es válido.",
                    null
                )
            )
            finish()
            return
        }

        registerPermissionLauncher(this)

        setContent {
            var showInstructions by remember { mutableStateOf(true) }

            if (showInstructions) {
                LivenessInstructionsScreen(onStart = {
                    showInstructions = false
                    viewModel.startLiveness(document)
                }, onBack = { finish() })
            } else {
                LoadingScreen()
            }
        }
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { uiState ->
                        when (uiState) {
                            is LivenessUiState.Initial -> {
                            }

                            is LivenessUiState.Loading -> {
                                Log.d("LivenessActivity", "Loading...")
                            }

                            is LivenessUiState.ChallengeCreated -> {
                                Log.d(
                                    "LivenessActivity",
                                    "ChallengeCreated: ${uiState.challengeId}"
                                )
                                startFaceLivenessDetector(uiState.challengeId)
                            }

                            is LivenessUiState.ChallengeCompleted -> {
                                Log.d(
                                    "LivenessActivity",
                                    "ChallengeCompleted: ${uiState.challengeId}"
                                )
                                viewModel.validateChallenge(uiState.challengeId)
                            }

                            is LivenessUiState.Success -> {
                                Log.d("LivenessActivity", "Success: ${uiState.challengeId}")
                                CallbackHandler.onCompleted(uiState.challengeId)
                                finish()
                            }

                            is LivenessUiState.Error -> {
                                Log.e("LivenessActivity", "Error: ${uiState.error}")
                                when (uiState.error) {
                                    is IDDigitalError.NetworkError -> {
                                        CallbackHandler.onError(uiState.error)
                                    }

                                    is IDDigitalError.CameraPermissionError -> {
                                        CallbackHandler.onError(uiState.error)
                                    }

                                    else -> {
                                        CallbackHandler.onError(uiState.error)
                                    }
                                }
                                finish()
                            }
                        }
                    }
                }
                launch {
                    viewModel.permissionResultChannel.collect { isGranted ->
                        viewModel.onPermissionResult(isGranted)
                    }
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

    private fun startFaceLivenessDetector(challengeId: String) {
        lifecycleScope.launch {
            try {
                val sessionId = livenessService.executeChallenge(challengeId)
                setContent {
                    FaceLivenessComponent(
                        sessionId = sessionId,
                        onComplete = {
                            onLivenessComplete(challengeId)
                        },
                        onError = { error ->
                            handleFaceLivenessError(error)
                        }
                    )


                }
            } catch (e: LivenessError) {
                CallbackHandler.onError(
                    IDDigitalError.UnknownError(GENERIC_ERROR_MESSAGE, e)
                )
                finish()
            }
        }
    }

    private fun handleFaceLivenessError(error: FaceLivenessDetectionException) {
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
    }

    private fun onLivenessComplete(challengeId: String) {
        viewModel.onLivenessCompleted(challengeId)
        setContent {
            LivenessCompletedLoadingScreen()
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT = "EXTRA_DOCUMENT"

        fun createIntent(
            context: Context,
            document: Document,

            ): Intent {
            return Intent(context, LivenessActivity::class.java).apply {
                putExtra(EXTRA_DOCUMENT, document)
            }
        }
    }
}