package uy.com.abitab.iddigitalsdk

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.models.DeviceAssociation
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.models.IDDigitalSDKEnvironment
import uy.com.abitab.iddigitalsdk.domain.models.Record
import uy.com.abitab.iddigitalsdk.domain.models.ValidationSession
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError

// ---------------------------------------------------------
// Wrapper Kotlin -> Java
// ---------------------------------------------------------
object IDDigitalSDKJavaWrapper {

    private var sdk: IDDigitalSDK? = null

    // ---------------------------------------------------------
    // Interfaces Java-friendly
    // ---------------------------------------------------------
    @FunctionalInterface
    interface OnErrorListener {
        fun onError(error: IDDigitalError)
    }

    @FunctionalInterface
    interface OnCompletedListener {
        fun onCompleted(value: String)
    }

    @FunctionalInterface
    interface OnBooleanResultListener {
        fun onResult(value: Boolean)
    }

    @FunctionalInterface
    interface OnValidationSessionListener {
        fun onSuccess(session: ValidationSession)
    }

    @FunctionalInterface
    interface OnDeviceAssociationListener {
        fun onSuccess(association: DeviceAssociation?)
    }

    @JvmStatic
    fun initialize(
        context: Context,
        apiKey: String,
        environment: IDDigitalSDKEnvironment,
        onError: OnErrorListener?,
        onCompleted: OnCompletedListener?
    ) {
        try {
            sdk = IDDigitalSDK.initialize(
                context,
                apiKey,
                environment,
                { error -> onError?.onError(error) },
                { result -> onCompleted?.onCompleted(result) }
            )
        } catch (e: Throwable) {
            onError?.onError(e.toIDDigitalError())
        }
    }

    @JvmStatic
    fun associate(
        context: Context,
        document: Document,
        onError: OnErrorListener?,
        onCompleted: OnCompletedListener?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sdk?.associate(
                    context,
                    document,
                    { error -> onError?.onError(error) },
                    { result -> onCompleted?.onCompleted(result) }
                )
            } catch (e: Throwable) {
                onError?.onError(e.toIDDigitalError())
            }
        }
    }

    @JvmStatic
    fun canAssociate(
        document: Document,
        onError: OnErrorListener?,
        listener: OnBooleanResultListener
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = try {
                sdk?.canAssociate(document, { error -> onError?.onError(error) }) ?: false
            } catch (e: Throwable) {
                onError?.onError(e.toIDDigitalError())
                false
            }
            listener.onResult(result)
        }
    }

    @JvmStatic
    fun isAssociated(listener: OnBooleanResultListener) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = sdk?.isAssociated() ?: false
            listener.onResult(result)
        }
    }

    @JvmStatic
    fun removeAssociation() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sdk?.removeAssociation()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun getDeviceAssociation(
        onError: OnErrorListener?,
        listener: OnDeviceAssociationListener
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sdk?.getDeviceAssociation(
                    { error -> onError?.onError(error) },
                    { association -> listener.onSuccess(association) }
                )
            } catch (e: Throwable) {
                onError?.onError(e.toIDDigitalError())
                listener.onSuccess(null)
            }
        }
    }

    @JvmStatic
    fun createValidationSession(
        context: Context,
        type: ChallengeType,
        onError: OnErrorListener?,
        onCompleted: OnCompletedListener?
    ) {
        try {
            sdk?.createValidationSession(
                context,
                type,
                { error -> onError?.onError(error) },
                { result -> onCompleted?.onCompleted(result) }
            )
        } catch (e: Throwable) {
            onError?.onError(e.toIDDigitalError())
        }
    }

//    @JvmStatic
//    fun executeChallenge(
//        challengeId: String,
//        data: Record,
//        onError: OnErrorListener?,
//        onCompleted: OnCompletedListener?
//    ) {
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                sdk?.executeChallenge(
//                    challengeId,
//                    data,
//                    { error -> onError?.onError(error) },
//                    { onCompleted?.onCompleted("Challenge executed successfully") }
//                )
//            } catch (e: Throwable) {
//                onError?.onError(e.toIDDigitalError())
//            }
//        }
//    }
//
//    @JvmStatic
//    fun validateChallenge(
//        challengeId: String,
//        data: Record,
//        onError: OnErrorListener?,
//        listener: OnBooleanResultListener
//    ) {
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                sdk?.validateChallenge(
//                    challengeId,
//                    data,
//                    { error -> onError?.onError(error) },
//                    { isValid -> listener.onResult(isValid) }
//                )
//            } catch (e: Throwable) {
//                onError?.onError(e.toIDDigitalError())
//                listener.onResult(false)
//            }
//        }
//    }

    @JvmStatic
    fun sendToKeycloak(
        tabId: String,
        sessionCode: String,
        clientId: String,
        realm: String,
        sdkToken: String,
        onError: OnErrorListener?,
        onSuccess: OnCompletedListener?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sdk?.sendToKeycloak(
                    tabId,
                    sessionCode,
                    clientId,
                    realm,
                    sdkToken,
                    { error -> onError?.onError(error) },
                    { response -> onSuccess?.onCompleted(response) }
                )
            } catch (e: Throwable) {
                onError?.onError(e.toIDDigitalError())
            }
        }
    }
}
