package com.example.iddigital

import Examples
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import uy.com.abitab.iddigitalsdk.IDDigitalSDK
import uy.com.abitab.iddigitalsdk.domain.models.IDDigitalSDKEnvironment
import uy.com.abitab.iddigitalsdk.utils.BadResponseError
import uy.com.abitab.iddigitalsdk.utils.CameraPermissionError
import uy.com.abitab.iddigitalsdk.utils.ChallengeValidationError
import uy.com.abitab.iddigitalsdk.utils.DeviceNotAssociatedError
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.utils.InvalidApiKeyError
import uy.com.abitab.iddigitalsdk.utils.InvalidChallengeIdError
import uy.com.abitab.iddigitalsdk.utils.InvalidDocumentError
import uy.com.abitab.iddigitalsdk.utils.InvalidPinError
import uy.com.abitab.iddigitalsdk.utils.NoInternetConnection
import uy.com.abitab.iddigitalsdk.utils.NotInitializedError
import uy.com.abitab.iddigitalsdk.utils.ServiceUnavailableError
import uy.com.abitab.iddigitalsdk.utils.TimeoutError
import uy.com.abitab.iddigitalsdk.utils.TooManyAttemptsError
import uy.com.abitab.iddigitalsdk.utils.UnexpectedResponseError
import uy.com.abitab.iddigitalsdk.utils.UnknownError
import uy.com.abitab.iddigitalsdk.utils.UnknownHostError
import uy.com.abitab.iddigitalsdk.utils.UserCancelledError
import uy.com.abitab.iddigitalsdk.utils.UserCannotBeAssociatedError

class MainActivity : ComponentActivity() {
    private lateinit var sdkInstance: IDDigitalSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiKey = BuildConfig.API_KEY
        try {
            sdkInstance = IDDigitalSDK.initialize(this, apiKey, environment = IDDigitalSDKEnvironment.STAGING, onError = {}, onCompleted = {})

            setContent {
                MainScreen(
                    sdkInstance = sdkInstance
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing IDDigitalSDK: ${e.message}", Toast.LENGTH_LONG)
                .show()
            e.printStackTrace()
        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(sdkInstance: IDDigitalSDK) {
    val context = LocalContext.current
    fun handleIDDigitalSdkError(error: IDDigitalError) {
        Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
        when (error) {
            is InvalidChallengeIdError -> TODO()
            is InvalidPinError -> TODO()
            is DeviceNotAssociatedError -> TODO()
            is NoInternetConnection -> TODO()
            is UnknownHostError -> TODO()
            is UserCannotBeAssociatedError -> TODO()
            is BadResponseError -> TODO()
            is CameraPermissionError -> TODO()
            is InvalidApiKeyError -> TODO()
            is InvalidDocumentError -> TODO()
            is NotInitializedError -> TODO()
            is ServiceUnavailableError -> TODO()
            is TimeoutError -> TODO()
            is TooManyAttemptsError -> TODO()
            is UnexpectedResponseError -> TODO()
            is UnknownError -> TODO()
            is UserCancelledError -> TODO()
            is ChallengeValidationError -> TODO()
        }
    }


    MaterialTheme {
        Scaffold(topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { Text("Mi app", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {},
                actions = {},
            )
        }) {
            Examples(
                sdkInstance = sdkInstance,
                onError = {
                    handleIDDigitalSdkError(it)
                }
            )
        }
    }
}

