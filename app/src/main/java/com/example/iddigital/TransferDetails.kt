import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDetailsScreen(onContinue: () -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mi Banco") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(32.dp)
                .fillMaxSize(),
        ) {
            Text(
                text = "Detalles de la Transferencia",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            TransferDetails(
                origin = "Cuenta Corriente 12345678",
                destination = "Cuenta Ahorro 98765432",
                amount = "1.500,00 UYU",
                concept = "Pago de servicios"
            )
            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)) {
                Button(onClick = onContinue, Modifier.height(60.dp).fillMaxWidth()) {
                    Text("CONTINUAR", color = Color.White, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun TransferDetails(origin: String, destination: String, amount: String, concept: String) {
    Column {
        DetailItem("Origen:", origin)
        DetailItem("Destino:", destination)
        DetailItem("Monto:", amount)
        DetailItem("Concepto:", concept)
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransferDetailsScreenPreview() {
    TransferDetailsScreen()
}