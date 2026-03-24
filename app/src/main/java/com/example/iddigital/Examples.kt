import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uy.com.abitab.iddigitalsdk.IDDigitalSDK
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Examples(sdkInstance: IDDigitalSDK, onError: (IDDigitalError) -> Unit) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }

    var documentNumber by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mi app") })
        }, modifier = Modifier.clickable(interactionSource = interactionSource,
            indication = null,
            onClick = { focusManager.clearFocus() })
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextField(
                value = documentNumber,
                shape = MaterialTheme.shapes.small.copy(
                    bottomEnd = ZeroCornerSize, topEnd = ZeroCornerSize
                ),
                onValueChange = { it ->
                    if (it.length <= 8) documentNumber = it
                },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                label = { Text("Documento") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(IntrinsicSize.Min)
            ) {

            }

            Text("Asociación", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CheckCanAssociate(
                    sdkInstance, documentNumber = documentNumber
                )

                AssociateDevice(sdkInstance,
                    documentNumber = documentNumber,
                    onCompleted = { idToken ->
                        Log.d("MainActivity", "ID Token: $idToken")
                        documentNumber = ""
                    })
                CheckAssociation(sdkInstance)

                RemoveAssociation(sdkInstance)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
            ) {}
            Spacer(modifier = Modifier.height(32.dp))

            Text("Desafíos", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CreateValidationSession(sdkInstance, ChallengeType.Pin)
                Spacer(modifier = Modifier.width(16.dp))
                CreateValidationSession(sdkInstance, ChallengeType.Liveness)
            }
        }
    }
}

@Composable
fun AssociateDevice(sdkInstance: IDDigitalSDK, documentNumber: String, onCompleted: (idToken: String) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }

    fun associateDevice() {
        coroutineScope.launch {
            if (documentNumber.length < 8) return@launch
            try {
                sdkInstance.associate(context = context, document = Document(
                    number = documentNumber
                ), onCompleted = { idToken ->
                    Toast.makeText(
                        context, "Dispositivo asociado con éxito", Toast.LENGTH_SHORT
                    ).show()
                    onCompleted(idToken)
                }, onError = {
                    Toast.makeText(
                        context, "Error al asociar dispositivo: $it", Toast.LENGTH_SHORT
                    ).show()
                })
            } catch (e: Throwable) {
                Toast.makeText(context, "Error al asociar dispositivo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Button(
        onClick = { associateDevice() },
        enabled = documentNumber.isNotEmpty(),
    ) {
        Text("Asociar")
    }
}

@Composable
fun CheckCanAssociate(sdkInstance: IDDigitalSDK, documentNumber: String) {
    val context = LocalContext.current
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }

    fun checkCanAssociate() {
        coroutineScope.launch {
            if (documentNumber.length < 8) return@launch
            try {
                val response = sdkInstance.canAssociate(document = Document(
                    number = documentNumber
                ), onError = {
                    Toast.makeText(
                        context, "Error al asociar dispositivo: $it", Toast.LENGTH_SHORT
                    ).show()
                })
                Toast.makeText(
                    context,
                    if (response) "El documento $documentNumber puede ser asociado" else "El documento $documentNumber NO puede ser asociado",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Throwable) {
                Toast.makeText(context, "Error al comprobar si puede asociar", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    FilledTonalButton(
        onClick = { checkCanAssociate() },
        enabled = documentNumber.isNotEmpty(),
    ) {
        Text(
            "Puede asociar?",
            color = if (documentNumber.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CheckAssociation(sdkInstance: IDDigitalSDK) {
    var associationValue by remember { mutableStateOf<Boolean?>(null) }
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }
    val context = LocalContext.current

    FilledTonalButton(
        onClick = {
            coroutineScope.launch {
                associationValue = sdkInstance.isAssociated()
                Toast.makeText(
                    context,
                    if (associationValue == true) "Usuario ya se encuentra asociado" else "No existe usuario asociado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
    ) {
        Text("Existe asociación?", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun RemoveAssociation(sdkInstance: IDDigitalSDK) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }
    val context = LocalContext.current

    Button(
        onClick = {
            coroutineScope.launch {
                sdkInstance.removeAssociation()
            }
            Toast.makeText(context, "Asociacion eliminada", Toast.LENGTH_SHORT).show()
        }, colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        Text("Eliminar", color = MaterialTheme.colorScheme.onError)
    }
}

@Composable
fun CreateValidationSession(sdkInstance: IDDigitalSDK, challengeType: ChallengeType) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }
    val context = LocalContext.current

    FilledTonalButton(onClick = {
        coroutineScope.launch {
            try {
                sdkInstance.createValidationSession(context = context,
                    type = challengeType,
                    onError = {
                        Toast.makeText(context, "Error: $it", Toast.LENGTH_SHORT).show()
                    },
                    onCompleted = {
                        Toast.makeText(context, "Completado: $it", Toast.LENGTH_SHORT).show()
                    })
            } catch (e: Throwable) {
                Toast.makeText(
                    context, "Error al validar Liveness", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }) {
        Text("Validar $challengeType", color = MaterialTheme.colorScheme.primary)
    }
}