package uy.com.abitab.iddigitalsdk.domain.models

import java.io.Serializable

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

enum class ChallengeType(val typeName: String) : Serializable {
    Liveness("liveness"),
    Pin("pin");

    companion object {
        fun fromString(typeString: String): ChallengeType? {
            return entries.find { it.typeName.equals(typeString, ignoreCase = true) }
        }
    }

    override fun toString(): String {
        return typeName
    }
}
