package uy.com.abitab.iddigitalsdk.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.ui.tooling.preview.Preview
import uy.com.abitab.iddigitalsdk.R

@Preview(showBackground = true)
@Composable
fun IntructionsScreenPreview() {
    InstructionsScreen({}, {})
}


@Composable
fun InstructionsScreen(onStart: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                PaddingValues(
                    start = 16.dp,
                    top = WindowInsets.systemBars
                        .asPaddingValues()
                        .calculateTopPadding() + 16.dp,
                    end = 16.dp,
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding() + 16.dp
                )
            )
    ) {

        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Rounded.Close,
                modifier = Modifier.size(28.dp),
                contentDescription = "Cancelar",
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Instrucciones:",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF002856),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                InstructionItem(
                    number = "1",
                    text = "Colocá tu rostro dentro del óvalo.",
                    imageResId = R.drawable.liveness_instructions
                )
                InstructionItem(
                    number = "2",
                    text = "Tu rostro debe estar descubierto."
                )
                InstructionItem(
                    number = "3",
                    text = "Ubicate en un lugar con buena luz."
                )

                Spacer(modifier = Modifier.height(48.dp))

                WarningText()
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Powered by")
                Spacer(Modifier.width(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.id_digital),
                    contentDescription = "ID Digital",
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF002856),
                    contentColor = Color.White
                )
            ) {
                Text(text = "COMENZAR")
            }
        }
    }
}


@Composable
fun InstructionItem(number: String, text: String, imageResId: Int? = null) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(vertical = 8.dp)
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00437A),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = text,
                fontSize = 16.sp,
                color = Color.Black
            )

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
            .border(width = 1.dp, color = Color(0xFFB97700), shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_warning),
                contentDescription = "Advertencia",
                tint = Color(0xFFB97700),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Advertencia de fotosensibilidad",
                fontSize = 17.sp,
                color = Color(0xFFB97700)
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "El brillo de la pantalla va a subir a 100% temporalmente. La verificación muestra luces de colores. Tené precaución si sos fotosensible.",
            fontSize = 15.sp,
            color = Color(0xFFB97700)
        )
    }
}