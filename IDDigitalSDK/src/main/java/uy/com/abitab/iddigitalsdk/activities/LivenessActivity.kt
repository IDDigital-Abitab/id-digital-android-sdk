package uy.com.abitab.iddigitalsdk.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
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
import com.amplifyframework.ui.liveness.ui.FaceLivenessDetector
import com.amplifyframework.ui.liveness.ui.LivenessColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import uy.com.abitab.iddigitalsdk.BuildConfig
import uy.com.abitab.iddigitalsdk.Document
import uy.com.abitab.iddigitalsdk.R
import uy.com.abitab.iddigitalsdk.composables.AbitabTheme
import uy.com.abitab.iddigitalsdk.composables.InstructionsScreen
import uy.com.abitab.iddigitalsdk.network.LivenessService
import java.io.IOException


class LivenessActivity : ComponentActivity() {

    private lateinit var livenessService: LivenessService
    private var accessToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureSystemUI()

        accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN)
        val document = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_DOCUMENT, Document::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_DOCUMENT) as? Document
        }

        if (accessToken == null || document == null) {
            finish()
            return
        }
        livenessService = LivenessService(accessToken!!)

        setContent {
            var showInstructions by remember { mutableStateOf(true) }

            if (showInstructions) {
                InstructionsScreen(
                    onStart = {
                        showInstructions = false
                        startLivenessFlow(document)
                    },
                    onBack = { finish() }
                )
            } else {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading))

                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
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
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = true
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun startLivenessFlow(document: Document) {
        lifecycleScope.launch {
            try {
                val challengeId = livenessService.createChallenge(document)
                Log.d("LivenessActivity", "Challenge ID obtenido: $challengeId")
                val sessionId = livenessService.executeChallenge(challengeId)
                Log.d("LivenessActivity", "Session ID obtenido: $sessionId")

                setContent {
                    AbitabTheme {
                        Box(
                            modifier = Modifier
                                .background(Color.Black)
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            FaceLivenessDetector(
                                sessionId = sessionId,
                                region = "us-east-1",
                                disableStartView = true,
                                onComplete = {
                                    lifecycleScope.launch {
                                        livenessService.validateChallenge(challengeId)
                                        finish()
                                    }
                                    setContent {
                                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading))

                                        Box(
                                            Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
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
                                },
                                onError = { error ->
                                    Log.e("LivenessActivity", "Error en liveness: ${error.message}")
                                    Log.e("LivenessActivity", error.throwable.toString())
                                    finish()
                                }
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LivenessActivity", "Error en startLivenessFlow: ${e.message}")
                finish()
            }
        }
    }


    companion object {
        private const val EXTRA_ACCESS_TOKEN = "EXTRA_ACCESS_TOKEN"
        private const val EXTRA_DOCUMENT = "EXTRA_DOCUMENT"

        fun createIntent(context: Context, accessToken: String, document: Document): Intent {
            return Intent(context, LivenessActivity::class.java).apply {
                putExtra(EXTRA_ACCESS_TOKEN, accessToken)
                putExtra(EXTRA_DOCUMENT, document)
            }
        }
    }
}
