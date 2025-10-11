import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import java.io.InputStream
import java.io.OutputStream
import com.google.protobuf.InvalidProtocolBufferException
import uy.com.abitab.iddigitalsdk.DeviceAssociationProto
import uy.com.abitab.iddigitalsdk.DocumentProto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uy.com.abitab.iddigitalsdk.domain.models.DeviceAssociation
import uy.com.abitab.iddigitalsdk.domain.models.Document


object DeviceAssociationSerializer : Serializer<DeviceAssociationProto> {
    override val defaultValue: DeviceAssociationProto = DeviceAssociationProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): DeviceAssociationProto {
        try {
            return DeviceAssociationProto.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: DeviceAssociationProto, output: OutputStream) {
        t.writeTo(output)
    }
}

val Context.deviceAssociationStore: DataStore<DeviceAssociationProto> by dataStore(
    fileName = "device_association.pb",
    serializer = DeviceAssociationSerializer
)

suspend fun Context.saveDeviceAssociation(deviceAssociation: DeviceAssociation) {
    deviceAssociationStore.updateData { currentAssociation ->
        val documentProto = DocumentProto.newBuilder()
            .setType(deviceAssociation.document.type)
            .setNumber(deviceAssociation.document.number)
            .setCountry(deviceAssociation.document.country)
            .build()

        currentAssociation.toBuilder()
            .setToken(deviceAssociation.token)
            .setDocument(documentProto)
            .setCreatedAt(deviceAssociation.createdAt)
            .build()
    }
}

fun Context.getDeviceAssociation(): Flow<DeviceAssociation?> {
    return deviceAssociationStore.data.map { associationProto ->
        if (associationProto == DeviceAssociationProto.getDefaultInstance()) {
            null
        } else {
            DeviceAssociation(
                token = associationProto.token,
                document = Document(
                    type = associationProto.document.type,
                    number = associationProto.document.number,
                    country = associationProto.document.country
                ),
                createdAt = associationProto.createdAt,
                idToken = associationProto.idToken
            )
        }
    }
}

suspend fun Context.removeDeviceAssociation() {
    deviceAssociationStore.updateData {
        DeviceAssociationProto.getDefaultInstance()
    }
}
