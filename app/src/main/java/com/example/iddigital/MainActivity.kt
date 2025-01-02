package com.example.iddigital

import TransferDetailsScreen
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import uy.com.abitab.iddigitalsdk.Document
import uy.com.abitab.iddigitalsdk.IDDigitalError
import uy.com.abitab.iddigitalsdk.IDDigitalSDK
import uy.com.abitab.iddigitalsdk.composables.AppTheme
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val httpClient = OkHttpClient.Builder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiKey = BuildConfig.API_KEY
        IDDigitalSDK.getInstance().initialize(this, apiKey)

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

        IDDigitalSDK.getInstance().startLiveness(this, document, onError = { error ->
            when(error) {
                is IDDigitalError.NetworkError -> {
                    Toast.makeText(this, "Ha ocurrido un error de conexión", Toast.LENGTH_SHORT).show()
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
                is IDDigitalError.WrongDataError -> {
                    Toast.makeText(this, "Datos incorrectos", Toast.LENGTH_SHORT).show()
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
                modifier = Modifier.background(Color(0xFF6200EE))
            )
        }) {
            TransferDetailsScreen(
                onContinue = onStartLivenessClick
            )
        }
    }
}
