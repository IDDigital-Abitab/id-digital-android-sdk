package uy.com.abitab.iddigitalsdk;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.net.Uri;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType;
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError;
import uy.com.abitab.iddigitalsdk.utils.NotInitializedError;

public class IDDigitalSDKJavaWrapperTest {

    @Before
    public void resetSdk() throws Exception {
        Field sdk = IDDigitalSDKJavaWrapper.class.getDeclaredField("sdk");
        sdk.setAccessible(true);
        sdk.set(null, null);
    }

    @Test
    public void reportsNotInitializedBeforeUsingSdk() {
        final IDDigitalError[] reportedError = new IDDigitalError[1];

        IDDigitalSDKJavaWrapper.isAssociated(
                error -> reportedError[0] = error,
                value -> {
                    throw new AssertionError("No result is expected before initialization");
                }
        );

        assertTrue(reportedError[0] instanceof NotInitializedError);
    }

    @Test
    public void exposesCurrentJavaContract() throws Exception {
        Class<IDDigitalSDKJavaWrapper> wrapper = IDDigitalSDKJavaWrapper.class;
        Class<IDDigitalSDKJavaWrapper.OnErrorListener> error =
                IDDigitalSDKJavaWrapper.OnErrorListener.class;
        Class<IDDigitalSDKJavaWrapper.OnNullableStringResultListener> nullableString =
                IDDigitalSDKJavaWrapper.OnNullableStringResultListener.class;

        wrapper.getMethod("parseAuthenticationLink", Uri.class);
        wrapper.getMethod(
                "associate",
                Context.class,
                String.class,
                error,
                IDDigitalSDKJavaWrapper.OnAssociationCompletedListener.class
        );
        wrapper.getMethod(
                "associateViaQrScan",
                Context.class,
                error,
                nullableString
        );
        wrapper.getMethod(
                "validateViaQrScan",
                Context.class,
                ChallengeType.class,
                error,
                nullableString
        );
        wrapper.getMethod(
                "completeTransaction",
                String.class,
                String.class,
                error,
                nullableString
        );
        wrapper.getMethod(
                "startActiveTransactionPolling",
                long.class,
                error,
                IDDigitalSDKJavaWrapper.OnTransactionDetectedListener.class
        );
        wrapper.getMethod("stopActiveTransactionPolling", error);
    }
}
