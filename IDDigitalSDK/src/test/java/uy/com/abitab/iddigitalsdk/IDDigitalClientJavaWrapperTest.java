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

public class IDDigitalClientJavaWrapperTest {

    @Before
    public void resetSdk() throws Exception {
        Field sdk = IDDigitalClientJavaWrapper.class.getDeclaredField("sdk");
        sdk.setAccessible(true);
        sdk.set(null, null);
    }

    @Test
    public void reportsNotInitializedBeforeUsingSdk() {
        final IDDigitalError[] reportedError = new IDDigitalError[1];

        IDDigitalClientJavaWrapper.isAssociated(
                error -> reportedError[0] = error,
                value -> {
                    throw new AssertionError("No result is expected before initialization");
                }
        );

        assertTrue(reportedError[0] instanceof NotInitializedError);
    }

    @Test
    public void exposesCurrentJavaContract() throws Exception {
        Class<IDDigitalClientJavaWrapper> wrapper = IDDigitalClientJavaWrapper.class;
        Class<IDDigitalClientJavaWrapper.OnErrorListener> error =
                IDDigitalClientJavaWrapper.OnErrorListener.class;
        Class<IDDigitalClientJavaWrapper.OnNullableStringResultListener> nullableString =
                IDDigitalClientJavaWrapper.OnNullableStringResultListener.class;

        wrapper.getMethod("parseAuthenticationLink", Uri.class);
        wrapper.getMethod(
                "associate",
                Context.class,
                String.class,
                error,
                IDDigitalClientJavaWrapper.OnAssociationCompletedListener.class
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
                IDDigitalClientJavaWrapper.OnTransactionDetectedListener.class
        );
        wrapper.getMethod("stopActiveTransactionPolling", error);
    }
}
