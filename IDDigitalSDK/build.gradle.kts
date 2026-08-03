import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dokka)
    `maven-publish`
    id("kotlin-parcelize")
    id("com.google.protobuf") version "0.9.4"
}

val sdkVersion = providers.gradleProperty("VERSION_NAME")
    .orElse(providers.environmentVariable("VERSION"))
    .getOrElse("0.0.0-SNAPSHOT")

tasks.named<DokkaTask>("dokkaHtml") {
    moduleName.set("ID Digital SDK")
    outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
    failOnWarning.set(true)

    dokkaSourceSets.named("main") {
        documentedVisibilities.set(setOf(DokkaConfiguration.Visibility.PUBLIC))
        reportUndocumented.set(true)
        skipEmptyPackages.set(true)
        suppressGeneratedFiles.set(true)

        perPackageOption {
            matchingRegex.set(
                """uy\.com\.abitab\.iddigitalsdk\.(composables|data|di|presentation|ui|domain\.(repositories|usecases))(\..*)?"""
            )
            suppress.set(true)
        }

        suppressedFiles.from(
            file("src/main/java/uy/com/abitab/iddigitalsdk/domain/models/ConfigData.kt"),
            file("src/main/java/uy/com/abitab/iddigitalsdk/data/DeviceAssociationDataStoreManager.kt"),
            fileTree("src/main/java/uy/com/abitab/iddigitalsdk/composables"),
            fileTree("src/main/java/uy/com/abitab/iddigitalsdk/utils") {
                exclude("IDDigitalError.kt")
            }
        )
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "uy.com.abitab"
                artifactId = "iddigitalsdk"
                version = sdkVersion
            }
        }
    }
}

android {
    namespace = "uy.com.abitab.iddigitalsdk"
    compileSdk = 35


    defaultConfig {
        minSdk = 26

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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.androidx.material3)
    implementation(libs.liveness)
    implementation(libs.aws.auth.cognito)
    implementation(libs.androidx.material3.v111)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.datastore.core.android)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.activity.ktx)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.okhttp)
    implementation(libs.lottie.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Active-transaction polling (ProcessLifecycleOwner) - .docs/sdk/cliente/05-polling-transaccion-activa.md
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite.v4302)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)

    // QR cross-device (associateViaQrScan) - .docs/sdk/cliente/01-arquitectura-y-flujos.md
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.30.2"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("kotlin") {
                    option("lite")
                }
                create("java") {
                    option("lite")
                }
            }
            task.builtins {

            }
        }
    }
}