package uy.com.abitab.iddigitalsdk.support

import android.os.Looper
import org.junit.Assert.fail
import org.robolectric.Shadows

internal fun idleMainUntil(message: () -> String, ready: () -> Boolean) {
    val timeoutMs = 5_000L
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!ready()) {
        if (System.currentTimeMillis() > deadline) {
            fail(message())
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(10)
    }
}
