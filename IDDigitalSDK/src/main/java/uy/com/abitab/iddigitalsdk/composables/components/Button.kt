package uy.com.abitab.iddigitalsdk.composables.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uy.com.abitab.iddigitalsdk.ui.theme.AbitabTheme
import androidx.compose.material3.Button as MaterialButton

@Composable
fun Button(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    return MaterialButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = MaterialTheme.shapes.medium,

    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }

}

@Preview(showBackground = true, showSystemUi = true)
@Preview(showBackground = true, showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun ButtonPreview() {
    AbitabTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center


        )
        {
            Button(text = "Button") {
                //
            }
        }
    }
}