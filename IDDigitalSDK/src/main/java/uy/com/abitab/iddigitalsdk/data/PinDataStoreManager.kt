package uy.com.abitab.iddigitalsdk.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import uy.com.abitab.iddigitalsdk.utils.PinSecurityManager

val Context.pinDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_pin_prefs")

class PinDataStoreManager(private val context: Context) {

    private val pinSecurityManager = PinSecurityManager()

    private object PreferencesKeys {
        val ENCRYPTED_PIN = stringPreferencesKey("encrypted_user_pin")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_pin_enabled")
        val LAST_BIOMETRIC_PIN_USAGE = stringPreferencesKey("last_biometric_pin_usage")
    }

    suspend fun savePinAndBiometricPreference(pin: String, enableBiometric: Boolean) {
        context.pinDataStore.edit { preferences ->
            if (enableBiometric) {
                try {
                    val encryptedPin = pinSecurityManager.encryptPin(pin)
                    preferences[PreferencesKeys.ENCRYPTED_PIN] = encryptedPin
                    preferences[PreferencesKeys.BIOMETRIC_ENABLED] = true
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    preferences[PreferencesKeys.BIOMETRIC_ENABLED] = false
                }
            } else {
                preferences.remove(PreferencesKeys.ENCRYPTED_PIN)
                preferences[PreferencesKeys.BIOMETRIC_ENABLED] = false
            }
        }
    }

    suspend fun getDecryptedPin(): String? {
        val encryptedPinFlow = context.pinDataStore.data.map { preferences ->
            preferences[PreferencesKeys.ENCRYPTED_PIN]
        }
        val encryptedPin = encryptedPinFlow.first()

        return if (encryptedPin != null) {
            try {
                pinSecurityManager.decryptPin(encryptedPin)
            } catch (e: SecurityException) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    suspend fun isBiometricPinEnabled(): Boolean {
        return context.pinDataStore.data.map { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false
        }.first()
    }

    suspend fun clearPinAndBiometricPreference() {
        context.pinDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.ENCRYPTED_PIN)
            preferences.remove(PreferencesKeys.BIOMETRIC_ENABLED)
        }
    }

    suspend fun saveLastBiometricPinUsage(isoDate: String) {
        context.pinDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BIOMETRIC_PIN_USAGE] = isoDate
        }
    }

    suspend fun getLastBiometricPinUsage(): String? {
        val flow = context.pinDataStore.data.map { preferences ->
            preferences[PreferencesKeys.LAST_BIOMETRIC_PIN_USAGE]
        }
        return flow.first()
    }
}