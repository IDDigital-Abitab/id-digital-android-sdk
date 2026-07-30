package uy.com.abitab.iddigitalsdk.utils

import android.content.Context

internal interface AmplifyInitializerInterface {
    suspend fun initialize(context: Context)
}