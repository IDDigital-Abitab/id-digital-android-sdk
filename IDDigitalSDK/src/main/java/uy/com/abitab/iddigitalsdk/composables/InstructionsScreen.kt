@file:OptIn(ExperimentalMaterial3Api::class)

package uy.com.abitab.iddigitalsdk.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uy.com.abitab.iddigitalsdk.R
import uy.com.abitab.iddigitalsdk.composables.components.Button
import uy.com.abitab.iddigitalsdk.composables.components.IDDigitalWatermark


@Preview(showBackground = true)
@Composable
fun IntructionsScreenPreview() {
    InstructionsScreen({}, {})
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsScreen(onStart: () -> Unit, onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    AbitabTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.primary

                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        scrolledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    )
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
                    .background(Color.White)
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
                        text = "Verifica tu identidad",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
                    )

                    Text(
                        "Para continuar, necesitamos verificar tu identidad. " + "Te pediremos que realices una validación facial.",
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
                            onClick = onStart, text = "COMENZAR"
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
                .background(Color(0xFFD9E3EB), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = number,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = text)

            if (imageResId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = null,
                    modifier = Modifier
                        .width(150.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}


@Composable
fun WarningText() {
    Column(
        modifier = Modifier
            .border(
                width = .5.dp, color = Color(0xFFB97700), shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_warning),
                contentDescription = "Advertencia",
                tint = Color(0xFFB97700),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Advertencia de fotosensibilidad",
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                color = Color(0xFFB97700)
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "El brillo de la pantalla va a subir a 100% temporalmente. La verificación muestra luces de colores. Tené precaución si sos fotosensible.",
            fontSize = 14.sp,
            color = Color(0xFFB97700)
        )
    }
}