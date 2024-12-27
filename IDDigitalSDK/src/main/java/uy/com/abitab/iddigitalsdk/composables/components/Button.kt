package uy.com.abitab.iddigitalsdk.composables.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button as MaterialButton

@Composable
fun Button(
    text: String,
    onClick: () -> Unit
) {
    return MaterialButton(
        onClick = onClick,
        modifier = Modifier.height(56.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}