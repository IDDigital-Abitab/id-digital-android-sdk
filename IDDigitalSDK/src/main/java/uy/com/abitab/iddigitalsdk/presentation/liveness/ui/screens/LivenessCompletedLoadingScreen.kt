package uy.com.abitab.iddigitalsdk.presentation.liveness.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import uy.com.abitab.iddigitalsdk.R
import uy.com.abitab.iddigitalsdk.ui.theme.AbitabTheme
import uy.com.abitab.iddigitalsdk.composables.components.IDDigitalWatermark

@Preview(showBackground = true, device = "id:pixel_8_pro", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, device = "id:pixel_8_pro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LivenessCompletedLoadingScreen() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.loading)
    )

    AbitabTheme {
        Scaffold { innerPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(
                        PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding(),
                        )
                    )
                    .background(MaterialTheme.colorScheme.surfaceDim)
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    LottieAnimation(
                        composition,
                        modifier = Modifier
                            .width(50.dp)
                            .height(50.dp),
                        iterations = LottieConstants.IterateForever
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Procesando...", style = MaterialTheme.typography.bodyLarge)

                }
                IDDigitalWatermark()
            }
        }
    }
}



