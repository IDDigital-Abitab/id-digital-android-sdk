package uy.com.abitab.iddigitalsdk.utils

import android.content.Context

interface AmplifyInitializerInterface {
    suspend fun initialize(context: Context)
}