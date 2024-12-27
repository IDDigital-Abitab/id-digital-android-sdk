package uy.com.abitab.iddigitalsdk.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import org.json.JSONObject
import uy.com.abitab.iddigitalsdk.R
import uy.com.abitab.iddigitalsdk.composables.InstructionsScreen
import java.io.IOException


class LivenessActivity : ComponentActivity() {

    private val httpClient = OkHttpClient()
    private var accessToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureSystemUI()

        accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN)
        if (accessToken == null) {
            finish()
            return
        }

        setContent {
            var showInstructions by remember { mutableStateOf(true) }

            if (showInstructions) {
                InstructionsScreen(
                    onStart = {
                        showInstructions = false
                        startLivenessFlow()
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

    private fun startLivenessFlow() {
        lifecycleScope.launch {
            try {
                val sessionId = fetchSessionID(
                    "http://192.168.1.11:3000/liveness-challenge",
                    accessToken!!
                )
                Log.d("LivenessActivity", "Session ID obtenido: $sessionId")

                setContent {
                    MaterialTheme(
                        colorScheme = LivenessColorScheme.default()
                    ) {
                        FaceLivenessDetector(
                            sessionId = sessionId,
                            region = "us-east-1",
                            disableStartView = true,
                            onComplete = { finish() },
                            onError = { error ->
                                Log.e("LivenessActivity", "Error en liveness: ${error.message}")
                                Log.e("LivenessActivity", error.throwable.toString())
                                finish()
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("LivenessActivity", "Error al obtener el session ID: ${e.message}")
                finish()
            }
        }
    }

    private suspend fun fetchSessionID(url: String, accessToken: String): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Error en la solicitud: ${response.code}")
                }
                val responseBody = response.body?.string()
                    ?: throw IOException("Cuerpo de la respuesta vacío")

                val json = JSONObject(responseBody)
                return@withContext json.getString("sessionId")
            }
        }

    companion object {
        private const val EXTRA_ACCESS_TOKEN = "EXTRA_ACCESS_TOKEN"

        fun createIntent(context: Context, accessToken: String): Intent {
            return Intent(context, LivenessActivity::class.java).apply {
                putExtra(EXTRA_ACCESS_TOKEN, accessToken)
            }
        }
    }
}
