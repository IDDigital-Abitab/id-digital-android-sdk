# ID Digital SDK para Android

SDK nativa de ID Digital para aplicaciones Android.

## Documentación

La [guía de integración](../.docs/sdk/cliente/README.md) explica los flujos de
autenticación y cuándo invocar cada operación. La referencia de API Kotlin se genera desde
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
