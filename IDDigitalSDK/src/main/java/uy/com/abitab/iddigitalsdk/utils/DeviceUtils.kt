package uy.com.abitab.iddigitalsdk.utils

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.util.UUID


fun getDeviceFingerprint(context: Context): String {
    return hashString(getAppSpecificUUID(context))
}

private fun getAppSpecificUUID(context: Context): String {
    val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("IDDigitalSDK", Context.MODE_PRIVATE)
    var appUUID = sharedPrefs.getString("id_digital_installation_uuid", null)

    if (appUUID == null) {
        appUUID = UUID.randomUUID().toString()
        sharedPrefs.edit().putString("id_digital_installation_uuid", appUUID).apply()
    }

    return appUUID
}

private fun hashString(input: String): String {
    return try {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        hash.fold("", { str, it -> str + "%02x".format(it) })
    } catch (e: Exception) {
        input
    }
}
