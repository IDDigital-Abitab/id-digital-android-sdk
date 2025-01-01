package com.example.iddigital

import TransferDetailsScreen
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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
import uy.com.abitab.iddigitalsdk.Document
import uy.com.abitab.iddigitalsdk.IDDigitalSDK
import uy.com.abitab.iddigitalsdk.composables.AppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IDDigitalSDK.getInstance().initialize(this, "FwE93jFEj0_Uxawc10EX5q3mxkQlnMkGKMxeAR1-3Ls")

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


        IDDigitalSDK.getInstance().startLiveness(
            this,
            document,
            onError = { error ->
                Log.e("MainActivity", "Error message: ${error.message}")
                Log.e("MainActivity", "Error: $error")
            },
            onCompleted = { challengeId ->
                Log.d("MainActivity", "Challenge completed: $challengeId")
            }
        )
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
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFFEC0000),
                    ),
                    title = { Text("Aprobar transacción", color = Color.White) },
                    navigationIcon = {},
                    actions = {},
                    modifier = Modifier.background(Color(0xFF6200EE))
                )
            }
        ) {
            TransferDetailsScreen(
                onContinue = onStartLivenessClick
            )
        }
    }
}
