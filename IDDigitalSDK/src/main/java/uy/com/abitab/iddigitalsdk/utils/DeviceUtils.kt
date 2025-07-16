package uy.com.abitab.iddigitalsdk.utils

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "IDDigitalSDK")

private val ID_DIGITAL_INSTALLATION_UUID_KEY = stringPreferencesKey("id_digital_installation_uuid")

suspend fun getDeviceFingerprint(context: Context): String {
    val uuid = getAppSpecificUUID(context)
    return hashString(uuid)
}

suspend fun getAppSpecificUUID(context: Context): String {
    val preferences = context.dataStore.data.first()
    val existingUUID = preferences[ID_DIGITAL_INSTALLATION_UUID_KEY]

    if (existingUUID != null) {
        return existingUUID
    }

    val newUUID = UUID.randomUUID().toString()
    context.dataStore.edit { mutablePreferences ->
        mutablePreferences[ID_DIGITAL_INSTALLATION_UUID_KEY] = newUUID
    }
    return newUUID
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
