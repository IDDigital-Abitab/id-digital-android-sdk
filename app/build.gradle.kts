import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val apiKey = localProperties.getProperty("API_KEY") ?: ""
val apiBaseUrl = localProperties.getProperty("API_BASE_URL") ?: ""
val sdkEnvironment = localProperties.getProperty("SDK_ENVIRONMENT")
    ?.trim()
    ?.uppercase()
    ?.ifEmpty { "STAGING" }
    ?: "STAGING"
require(sdkEnvironment == "STAGING" || sdkEnvironment == "PRODUCTION") {
    "SDK_ENVIRONMENT must be STAGING or PRODUCTION, but was '$sdkEnvironment'"
}

// Configuracion del cliente Keycloak usado por el login del Patron B (puente web).
// client_id/redirect_uri son especificos de lo que se registre en Keycloak para esta
// app de ejemplo; no tienen un default seguro y deben completarse en local.properties.
val keycloakBaseUrl = localProperties.getProperty("KEYCLOAK_BASE_URL") ?: "https://bqm-keycloak-dev.alabamasolutions.com"
val keycloakRealm = localProperties.getProperty("KEYCLOAK_REALM") ?: "bqm-realm"
val keycloakClientId = localProperties.getProperty("KEYCLOAK_CLIENT_ID") ?: "alabama-client"
val keycloakRedirectUri = localProperties.getProperty("KEYCLOAK_REDIRECT_URI") ?: "iddigitalsample://auth"

android {
    namespace = "com.example.iddigital"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.iddigital"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "SDK_ENVIRONMENT", "\"$sdkEnvironment\"")
        buildConfigField("String", "KEYCLOAK_BASE_URL", "\"$keycloakBaseUrl\"")
        buildConfigField("String", "KEYCLOAK_REALM", "\"$keycloakRealm\"")
        buildConfigField("String", "KEYCLOAK_CLIENT_ID", "\"$keycloakClientId\"")
        buildConfigField("String", "KEYCLOAK_REDIRECT_URI", "\"$keycloakRedirectUri\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    // Firebase Cloud Messaging: recibe el push cross-device de esta app de ejemplo
    // (rol de "infraestructura FCM propia del Integrador"), ver fcm/IDDigitalSampleFcmService.kt.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(project(":IDDigitalSDK"))
    implementation(libs.androidx.ui.android)
    implementation(libs.aws.auth.cognito)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.compose.material3:material3:1.1.1")
    implementation("androidx.activity:activity-compose:1.6.0")
    implementation("androidx.compose.ui:ui:1.4.0")
    implementation("androidx.compose.material3:material3:1.0.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.0")


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.material)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.okhttp)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.material.icons.extended)
}