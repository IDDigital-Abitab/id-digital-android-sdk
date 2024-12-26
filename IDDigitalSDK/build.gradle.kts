plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "uy.com.abitab.iddigitalsdk"
    compileSdk = 34


    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation("androidx.compose.material3:material3:1.1.1") // Material3
    implementation("com.amplifyframework.ui:liveness:1.2.0") // Amplify Liveness
    implementation("com.amplifyframework:aws-auth-cognito:2.14.6") // AWS Auth Cognito
    implementation("androidx.compose.material3:material3:1.1.1") // Material3

    implementation("androidx.compose.ui:ui:1.4.0")
    implementation("androidx.compose.material3:material3:1.1.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.4.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.0")
    implementation("androidx.activity:activity-compose:1.7.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.airbnb.android:lottie:3.4.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}