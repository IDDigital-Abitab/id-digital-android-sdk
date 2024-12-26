package com.example.iddigital

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uy.com.abitab.iddigitalsdk.IDDigitalSDK

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar el SDK
        IDDigitalSDK.getInstance().initialize(this, "accessToken")

        // Configurar la UI inicial
        setContent {
            MainScreen {
                startLiveness()
            }
        }
    }

    private fun startLiveness() {
        IDDigitalSDK.getInstance().startLiveness(this)
    }
}

@Composable
fun MainScreen(onStartLivenessClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Bienvenido a ID Digital SDK")

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para iniciar liveness
            Button(
                onClick = onStartLivenessClick
            ) {
                Text(text = "Iniciar Liveness")
            }
        }
    }
}
