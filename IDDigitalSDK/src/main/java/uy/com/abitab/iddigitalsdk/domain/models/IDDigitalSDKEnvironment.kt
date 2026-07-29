package uy.com.abitab.iddigitalsdk.domain.models

/**
 * Ambiente de servicios utilizado por la SDK.
 *
 * Las credenciales son específicas de cada ambiente y no deben intercambiarse.
 */
enum class IDDigitalSDKEnvironment {
    /** Ambiente previo a producción para integración y pruebas. */
    STAGING,

    /** Ambiente productivo de ID Digital. */
    PRODUCTION
}