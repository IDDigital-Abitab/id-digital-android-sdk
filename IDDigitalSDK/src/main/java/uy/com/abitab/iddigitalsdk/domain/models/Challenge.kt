package uy.com.abitab.iddigitalsdk.domain.models

import java.io.Serializable

/**
 * Representación interna de un desafío recibido desde el backend.
 *
 * @suppress
 */
data class Challenge(
    val id: String,
    val type: String,
    val status: String,
    val expirationDate: String
)

// TODO check if this is the best way to do it
//sealed class ChallengeType : Serializable {
//    data object Liveness : ChallengeType() {
//        private fun readResolve(): Any = Liveness
//    }
//
//    data object Pin : ChallengeType() {
//        private fun readResolve(): Any = Pin
//    }
//
//    companion object {
//        fun fromString(typeString: String): ChallengeType? {
//            return when (typeString.lowercase()) {
//                "liveness" -> Liveness
//                "pin" -> Pin
//                else -> null
//            }
//        }
//    }
//
//    override fun toString(): String {
//        return when (this) {
//            is Liveness -> "liveness"
//            is Pin -> "pin"
//        }
//    }
//}

/**
 * Tipo de desafío de identidad que presentará la SDK.
 *
 * @property typeName valor utilizado en el contrato con el backend.
 */
enum class ChallengeType(val typeName: String) : Serializable {
    /** Prueba biométrica de vida mediante la cámara del dispositivo. */
    Liveness("liveness"),

    /** Verificación mediante el PIN de ID Digital. */
    Pin("pin");

    /** Conversión desde los valores utilizados por el contrato HTTP. */
    companion object {
        /**
         * Convierte el nombre del contrato en su tipo de desafío.
         *
         * @param typeString nombre `liveness` o `pin`, sin distinguir mayúsculas.
         * @return el tipo correspondiente, o `null` si el nombre no es reconocido.
         */
        fun fromString(typeString: String): ChallengeType? {
            return entries.find { it.typeName.equals(typeString, ignoreCase = true) }
        }
    }

    /** Devuelve [typeName], el valor esperado por el backend. */
    override fun toString(): String {
        return typeName
    }
}
