package uy.com.abitab.iddigitalsdk.utils

import uy.com.abitab.iddigitalsdk.domain.models.Document

data class ApiResponse<T>(
    val data: T,
)