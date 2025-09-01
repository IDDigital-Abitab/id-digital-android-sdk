package uy.com.abitab.iddigitalsdk.domain.models

import java.io.Serializable

data class Challenge(
    val id: String,
    val type: String,
    val status: String,
    val expirationDate: String
)

sealed class ChallengeType : Serializable {
    data object Liveness : ChallengeType() {
        private fun readResolve(): Any = Liveness
    }

    data object Pin : ChallengeType() {
        private fun readResolve(): Any = Pin
    }

    companion object {
        fun fromString(typeString: String): ChallengeType? {
            return when (typeString.lowercase()) {
                "liveness" -> Liveness
                "pin" -> Pin
                else -> null
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            is Liveness -> "liveness"
            is Pin -> "pin"
        }
    }
}
