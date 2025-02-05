package uy.com.abitab.iddigitalsdk.utils

import android.content.Context
import android.util.Log
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.AmplifyConfiguration
import org.json.JSONObject
import uy.com.abitab.iddigitalsdk.R
import java.nio.charset.Charset


object AmplifyInitializer: AmplifyInitializerInterface {

    override fun initialize(context: Context) {
        try {
            val inputStream = context.resources.openRawResource(R.raw.amplifyconfiguration)
            val jsonString = inputStream.bufferedReader(Charset.defaultCharset()).use { it.readText() }
            val configuration = AmplifyConfiguration.fromJson(JSONObject(jsonString))

            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(configuration, context)

            Log.d("AmplifyInitializer", "Amplify configurado correctamente.")
        } catch (error: AmplifyException) {
            Log.e("AmplifyInitializer", "Error al inicializar Amplify: ${error.message}", error)
            throw RuntimeException("Failed to initialize Amplify: ${error.message}", error)
        }
    }
}
