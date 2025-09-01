package uy.com.abitab.iddigitalsdk.domain.models

import java.io.Serializable

data class Document(
    val number: String,
    val type: String? = null,
    val country: String? = null
) : Serializable