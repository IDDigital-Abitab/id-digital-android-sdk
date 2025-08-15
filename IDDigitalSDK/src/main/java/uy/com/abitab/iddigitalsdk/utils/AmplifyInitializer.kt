package uy.com.abitab.iddigitalsdk.utils

import android.content.Context
import android.util.Log
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.AmplifyConfiguration
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import uy.com.abitab.iddigitalsdk.R
import uy.com.abitab.iddigitalsdk.data.network.ConfigService
import java.nio.charset.Charset


object AmplifyInitializer: AmplifyInitializerInterface, KoinComponent {
    private val configService: ConfigService by inject()

    override suspend fun initialize(context: Context) {
        val configData = configService.getConfiguration()
        try {

            val jsonString = """{
  "UserAgent": "aws-amplify-cli/2.0",
  "Version": "1.0",
  "auth": {
    "plugins": {
      "awsCognitoAuthPlugin": {
        "UserAgent": "aws-amplify-cli/0.1.0",
        "Version": "0.1.0",
        "IdentityManager": {
          "Default": {}
        },
        "CredentialsProvider": {
          "CognitoIdentity": {
            "Default": {
              "PoolId": "${configData.cognitoIdentityPoolId}",
              "Region": "${configData.region}"
            }
          }
        },
        "CognitoUserPool": {
          "Default": {
            "PoolId": "${configData.cognitoUserPoolId}",
            "AppClientId": "${configData.cognitoAppClientId}",
            "Region": "${configData.region}"
          }
        },
        "Auth": {
          "Default": {
            "authenticationFlowType": "USER_SRP_AUTH",
            "socialProviders": [],
            "usernameAttributes": [],
            "signupAttributes": [
              "EMAIL"
            ],
            "passwordProtectionSettings": {
              "passwordPolicyMinLength": 8,
              "passwordPolicyCharacters": []
            },
            "mfaConfiguration": "OFF",
            "mfaTypes": [
              "SMS"
            ],
            "verificationMechanisms": [
              "PHONE_NUMBER"
            ]
          }
        }
      }
    }
  }
}"""
            val configuration = AmplifyConfiguration.fromJson(JSONObject(jsonString))

            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(configuration, context)
            Log.i("AmplifyInitializer", "Initialized Amplify with Cognito Auth plugin")

        } catch (error: Throwable) {
            error.toIDDigitalError()
            throw RuntimeException("Failed to initialize Amplify: ${error.message}", error)
        }
    }
}
