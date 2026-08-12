# ID Digital SDK para Android

SDK nativa de ID Digital para aplicaciones Android.

## Requisitos de Instalación

- **minSdk:** 26 (Android 8.0) o superior.
- **Java 8+ API desugaring:** El SDK requiere habilitar `coreLibraryDesugaring` en el archivo `build.gradle.kts` de la aplicación integradora debido al uso de APIs modernas de Java (ej. `java.time` dentro de los módulos de AWS).

```kotlin
android {
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("uy.com.abitab:iddigitalsdk:1.0.5")
}
```

## Documentación

La [guía de integración](../.docs/sdk/cliente/README.md) explica los flujos de
autenticación y cuándo invocar cada operación. La referencia de API Kotlin y Java se genera desde
el KDoc de la superficie pública mediante Dokka.

Requisito local: JDK 17.

En Linux o macOS:

```shell
./gradlew :IDDigitalSDK:dokkaHtml
```

En Windows:

```powershell
.\gradlew.bat :IDDigitalSDK:dokkaHtml
```

El índice generado queda en `IDDigitalSDK/build/dokka/html/index.html`.

GitHub Actions ejecuta la misma tarea en pull requests, `main` y tags. El resultado se
descarga desde la ejecución del workflow **API documentation**, en el artefacto
`iddigital-android-api-docs-<commit>`.

## App de ejemplo

La integración de referencia está documentada en [`app/README.md`](app/README.md).
