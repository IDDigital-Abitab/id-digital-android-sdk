package uy.com.abitab.iddigitalsdk.utils

import android.content.Context

interface PermissionsManagerInterface {
    fun hasCameraPermission(context: Context): Boolean
    suspend fun requestCameraPermission(context: Context): Boolean
}