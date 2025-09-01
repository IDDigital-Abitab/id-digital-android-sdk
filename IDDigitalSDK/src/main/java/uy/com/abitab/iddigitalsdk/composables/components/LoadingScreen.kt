import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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

@Preview()
@Composable
fun LoadingScreen() {
    val spec = R.raw.loading
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(spec))

    AbitabTheme {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            LottieAnimation(
                composition,
                modifier = Modifier
                    .width(50.dp)
                    .height(50.dp),
                iterations = LottieConstants.IterateForever,
            )
        }
    }
}