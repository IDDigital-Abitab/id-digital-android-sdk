package com.example.iddigital

import TransferDetailsScreen
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.IDDigitalSDK
import uy.com.abitab.iddigitalsdk.composables.AppTheme
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val httpClient = OkHttpClient.Builder().build()
    private lateinit var sdkInstance: IDDigitalSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiKey = BuildConfig.API_KEY
        sdkInstance = IDDigitalSDK.initialize(this, apiKey)

        setContent {
            MainScreen(onStartLivenessClick = {
                startLiveness()
            })
        }
    }

    private fun startLiveness() {
        val document = Document(
            number = "45743055"
        )

        sdkInstance.startLiveness(this, document, onError = { error ->
            when(error) {
                is IDDigitalError.NetworkError -> {
                    Toast.makeText(this, "Ha ocurrido un error de conexión", Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity", error.toString() )
                }
                is IDDigitalError.CameraPermissionError -> {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
                is IDDigitalError.UnknownError -> {
                    Toast.makeText(this, "Error desconocido", Toast.LENGTH_SHORT).show()
                }
                is IDDigitalError.UserCancelledError -> {
                    Toast.makeText(this, "Has cancelado la validación, vuelve a intentarlo", Toast.LENGTH_SHORT).show()
                }
                is IDDigitalError.SDKError.InvalidApiKey -> {
                    Toast.makeText(this, "API Key inválida", Toast.LENGTH_SHORT).show()
                }
                is IDDigitalError.SDKError.InvalidDocument -> {
                    Toast.makeText(this, "Documento inválido", Toast.LENGTH_SHORT).show()
                }
                is IDDigitalError.SDKError.NotInitialized -> {
                    Toast.makeText(this, "SDK no inicializado", Toast.LENGTH_SHORT).show()
                }
                is IDDigitalError.ServerError.BadResponse -> {
                    Toast.makeText(this, "Respuesta del servidor inválida", Toast.LENGTH_SHORT).show()
                }
                is IDDigitalError.ServerError.ServiceUnavailable -> {
                    Toast.makeText(this, "Servicio no disponible", Toast.LENGTH_SHORT).show()
                }
                is IDDigitalError.ServerError.UnexpectedResponse -> {
                    Toast.makeText(this, "Respuesta inesperada del servidor", Toast.LENGTH_SHORT).show()
                }
                is IDDigitalError.TimeoutError -> {
                    Toast.makeText(this, "Tiempo de espera agotado", Toast.LENGTH_SHORT).show()
                }
            }
        }, onCompleted = { challengeId ->
            setContent {
                ProcessingScreen()
            }
            lifecycleScope.launch {
                val result = getChallengeResult(challengeId)
                withContext(Dispatchers.Main) {
                    setContent {
                        AppTheme {
                            if (result == ChallengeResult.SUCCESS) {
                                SuccessScreen(onRetry = {
                                    setContent {
                                        MainScreen(onStartLivenessClick = {
                                            startLiveness()
                                        })
                                    }
                                })
                            } else {
                                ErrorScreen(onRetry = {
                                    setContent {
                                        MainScreen(onStartLivenessClick = {
                                            startLiveness()
                                        })
                                    }
                                })
                            }
                        }
                    }
                }
            }
        })
    }

    enum class ChallengeResult {
        SUCCESS,
        FAILED
    }

    private suspend fun getChallengeResult(challengeId: String): ChallengeResult {
        return withContext(Dispatchers.IO) {
            delay(2000)
            val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

            val request = Request.Builder()
                .url("$baseUrl/get-challenge-result/$challengeId")
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    Log.d("MainActivity", "Response: $response")
                    if (!response.isSuccessful) {
                        return@withContext ChallengeResult.FAILED
                    }
                    val responseBody = response.body.string()

                    val json = JSONObject(responseBody)
                    val result = json.getString("result")
                    Log.d("MainActivity", "Result: $result")
                    if (result == "succeeded") {
                        return@withContext ChallengeResult.SUCCESS
                    }
                    return@withContext ChallengeResult.FAILED
                }
            } catch (e: IOException) {
                Log.e("MainActivity", "Error: $e")
                return@withContext ChallengeResult.FAILED
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(onStartLivenessClick = {

    })
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(onStartLivenessClick: () -> Unit) {
    AppTheme {
        Scaffold(topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFEC0000),
                ),
                title = { Text("Aprobar transacción", color = Color.White) },
                navigationIcon = {},
                actions = {},
            )
        }) {
            TransferDetailsScreen(
                onContinue = onStartLivenessClick
            )
        }
    }
}
