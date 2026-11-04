package uy.com.abitab.iddigitalsdk

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class IDDigitalClientLinkTest {

    @Test
    fun parseAuthenticationLink_returnsTransactionId() {
        // Given an authentication deep link
        val uri = Uri.parse("iddigital://auth?transactionId=tx-42")

        // When the app asks the SDK to read it
        val transactionId = IDDigitalClient.parseAuthenticationLink(uri)

        // Then the transaction id is available for associate or completeTransaction
        assertEquals("tx-42", transactionId)
    }

    @Test
    fun parseAuthenticationLink_returnsNullWhenTransactionIdIsAbsent() {
        // Given a redirect that does not belong to an ID Digital transaction,
        // for example the app's own Keycloak return
        val uri = Uri.parse("iddigitalsample://auth?code=abc&state=xyz")

        // When the app asks the SDK to read it
        val transactionId = IDDigitalClient.parseAuthenticationLink(uri)

        // Then the SDK ignores it
        assertNull(transactionId)
    }
}
