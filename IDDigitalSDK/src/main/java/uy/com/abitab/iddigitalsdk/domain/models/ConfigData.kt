package uy.com.abitab.iddigitalsdk.domain.models

data class ConfigData(
    val cognitoAppClientId: String,
    val cognitoUserPoolId: String,
    val cognitoIdentityPoolId: String,
    val region: String
)