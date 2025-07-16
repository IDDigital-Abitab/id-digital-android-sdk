@file:OptIn(ExperimentalMaterial3Api::class)

package uy.com.abitab.iddigitalsdk.presentation.liveness.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import uy.com.abitab.iddigitalsdk.R
import uy.com.abitab.iddigitalsdk.composables.components.Button
import uy.com.abitab.iddigitalsdk.composables.components.IDDigitalWatermark
import uy.com.abitab.iddigitalsdk.ui.theme.AbitabTheme


@Preview(showBackground = true, device = "id:pixel_8_pro", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, device = "id:pixel_8_pro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LivenessIntructionsScreenPreview() {
    LivenessInstructionsScreen(onStart = {}, onBack = {}, onClose = {})
}

fun isCameraPermissionGranted(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivenessInstructionsScreen(onStart: () -> Unit, onBack: (() -> Unit)?, onClose: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val context = LocalContext.current
    val cameraPermissionState = remember { mutableStateOf(isCameraPermissionGranted(context)) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            cameraPermissionState.value = isGranted
            if (isGranted) {
                onStart()
            } else {
                Toast.makeText(
                    context,
                    "Permiso de cámara denegado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )

    AbitabTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        if (onBack !== null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
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


                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .clip(
                            RoundedCornerShape(
                                topStart = 24.dp, topEnd = 24.dp
                            )
                        )
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Validá tu identidad",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
                    )

                    Text(
                        "Para continuar, necesitamos validar tu identidad. " + "Te pediremos que realices una validación facial.",
                        modifier = Modifier.padding(bottom = 32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    InstructionItem(
                        number = "1",
                        text = "Colocá tu rostro dentro del óvalo.",
                        imageResId = R.drawable.liveness_instructions
                    )
                    InstructionItem(
                        number = "2", text = "Tu rostro debe estar descubierto."
                    )
                    InstructionItem(
                        number = "3", text = "Ubicate en un lugar con buena luz."
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    WarningText()

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 48.dp)
                    ) {
                        Button(
                            onClick = {
                                if (cameraPermissionState.value) {
                                    onStart()
                                } else {
                                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }, text = "COMENZAR"
                        )
                    }
                }

                IDDigitalWatermark()

                Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
            }
        }
    }
}


@Composable
fun InstructionItem(number: String, text: String, imageResId: Int? = null) {
    Row(
        verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer, CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = number,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = text, style = MaterialTheme.typography.titleMedium)

            if (imageResId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = null,
                    modifier = Modifier
                        .width(150.dp)
                        .align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}


@Composable
fun WarningText() {
    val warningColor = if (isSystemInDarkTheme()) Color(0xFFd5ad66) else Color(0xFFB97700)


    Column(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = warningColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_warning),
                contentDescription = "Advertencia",
                tint = warningColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Advertencia de fotosensibilidad",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W500,
                color = warningColor
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "El brillo de la pantalla va a subir a 100% temporalmente. La verificación muestra luces de colores. Tené precaución si sos fotosensible.",
            color = warningColor,
            style = MaterialTheme.typography.bodySmall

        )
    }
}
