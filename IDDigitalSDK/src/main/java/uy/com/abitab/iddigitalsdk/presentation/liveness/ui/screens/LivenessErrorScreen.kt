package uy.com.abitab.iddigitalsdk.presentation.liveness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uy.com.abitab.iddigitalsdk.composables.components.Button
import uy.com.abitab.iddigitalsdk.ui.theme.AbitabTheme

@Preview
@Composable
fun LivenessErrorScreenPreview() {
    LivenessErrorScreen(onClose = {}, onRetry = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivenessErrorScreen(onClose: () -> Unit, onRetry: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    AbitabTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {},
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No pudimos validar tu identidad",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
                )

                Text(
                    "Ubicate en un lugar con fondo despejado y colocá tu rostro dentro del óvalo.\nTu rostro debe estar descubierto y tener buena iluminación.",
                    modifier = Modifier.padding(bottom = 32.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 48.dp)
                ) {
                    Button(
                        onClick = onRetry, text = "REINTENTAR"
                    )
                }
            }
        }
    }
}