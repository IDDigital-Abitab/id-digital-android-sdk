package uy.com.abitab.iddigitalsdk.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import getDeviceAssociation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uy.com.abitab.iddigitalsdk.domain.usecases.GetPendingTransactionsUseCase

/**
 * Polling de "transacción activa": canal redundante al push que replica el
 * mecanismo ya usado por la app default de ID Digital (ver
 * .docs/sdk/cliente/09-polling-transaccion-activa.md). Solo cubre login
 * recurrente (validation): un dispositivo sin asociar no tiene bearer token
 * contra el cual preguntar, así que mientras no haya asociación el polling
 * queda en espera silenciosa (no es un error).
 *
 * Se pausa/reanuda automáticamente con el foreground/background del proceso
 * (ProcessLifecycleOwner), igual que el gate `appState === 'active'` de la
 * app RN. Se detiene a sí mismo apenas reporta una transacción, para no
 * volver a dispararla en cada tick mientras el Integrador la está
 * resolviendo - llamar [start] de nuevo para volver a habilitarlo (p. ej.
 * tras cerrar esa transacción con completeTransaction).
 */
internal class ActiveTransactionPoller(
    private val context: Context,
    private val getPendingTransactionsUseCase: GetPendingTransactionsUseCase,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var pollingJob: Job? = null
    private var intervalMs: Long = DEFAULT_INTERVAL_MS
    private var onTransactionDetected: ((String) -> Unit)? = null
    private var isForeground = false
    private var isEnabled = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun start(intervalMs: Long, onTransactionDetected: (String) -> Unit) {
        this.intervalMs = intervalMs
        this.onTransactionDetected = onTransactionDetected
        isEnabled = true
        restartIfNeeded()
    }

    fun stop() {
        isEnabled = false
        onTransactionDetected = null
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onStart(owner: LifecycleOwner) {
        isForeground = true
        restartIfNeeded()
    }

    override fun onStop(owner: LifecycleOwner) {
        isForeground = false
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun restartIfNeeded() {
        if (!isEnabled || !isForeground || pollingJob?.isActive == true) return

        pollingJob = scope.launch {
            while (isActive) {
                try {
                    // Silencioso si todavía no hay asociación: no hay bearer
                    // token contra el cual preguntar (ver docstring de la
                    // clase). No es un error - solo se espera al próximo tick.
                    if (context.getDeviceAssociation().firstOrNull() != null) {
                        val oldest = getPendingTransactionsUseCase().firstOrNull()
                        if (oldest != null) {
                            val callback = onTransactionDetected
                            isEnabled = false
                            onTransactionDetected = null
                            callback?.invoke(oldest.id)
                            return@launch
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("IDDigitalSDK", "Error polling active transactions", e)
                }
                delay(intervalMs)
            }
        }
    }

    companion object {
        const val DEFAULT_INTERVAL_MS = 10_000L
    }
}
