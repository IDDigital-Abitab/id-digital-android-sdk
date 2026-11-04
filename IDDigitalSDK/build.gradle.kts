import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask

import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoReportBase

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dokka)
    jacoco
    `maven-publish`
    id("kotlin-parcelize")
    id("com.google.protobuf") version "0.9.4"
}

jacoco {
    toolVersion = "0.8.12"
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
        repositories {
            maven {
                name = "LocalDist"
                url = uri(layout.buildDirectory.dir("repo"))
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
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
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
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
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

private fun JacocoReportBase.configureSdkLogicCoverage() {
    val included = listOf(
        "uy/com/abitab/iddigitalsdk/data/network/**",
        "uy/com/abitab/iddigitalsdk/domain/**",
        "uy/com/abitab/iddigitalsdk/presentation/device_association/ui/viewmodels/**",
        "uy/com/abitab/iddigitalsdk/presentation/validation_session/ui/viewmodels/**",
    )
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
            include(included)
        },
        fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
            include(included)
        },
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("outputs/unit_test_code_coverage/**/*.exec")
            include("jacoco/*.exec")
        }
    )
}

val sdkCoverageReport = tasks.register<JacocoReport>("sdkCoverageReport") {
    group = "verification"
    description = "Line coverage of the SDK logic covered by IDAPP-606."
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    configureSdkLogicCoverage()
}

tasks.register<JacocoCoverageVerification>("sdkCoverageVerification") {
    group = "verification"
    description = "Fails when SDK logic line coverage drops below 70%."
    dependsOn(sdkCoverageReport)
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
    configureSdkLogicCoverage()
}

tasks.named("check") {
    dependsOn("sdkCoverageVerification")
}

// Robolectric loads SDK classes from its own class loader. Without this, JaCoCo
// records those classes as uncovered even when the tests pass.
tasks.withType<Test>().configureEach {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}