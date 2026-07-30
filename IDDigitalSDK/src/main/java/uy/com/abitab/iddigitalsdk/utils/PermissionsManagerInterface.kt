package uy.com.abitab.iddigitalsdk.utils

import android.content.Context

internal interface PermissionsManagerInterface {
    fun hasCameraPermission(context: Context): Boolean
    suspend fun requestCameraPermission(context: Context): Boolean
}