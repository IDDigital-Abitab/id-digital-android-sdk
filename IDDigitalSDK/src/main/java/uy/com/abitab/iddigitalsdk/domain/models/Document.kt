package uy.com.abitab.iddigitalsdk.domain.models

import java.io.Serializable

/**
 * Documento asociado al ciudadano almacenado localmente.
 *
 * La aplicación no necesita construir este modelo para iniciar asociaciones nuevas. Puede
 * recibirlo como parte de [DeviceAssociation].
 *
 * @property number número del documento.
 * @property type tipo de documento definido por ID Digital, por ejemplo `ci` o `psp`.
 * @property country código ISO 3166-1 alpha-2 del país emisor.
 */
data class Document(
    val number: String,
    val type: String,
    val country: String
) : Serializable