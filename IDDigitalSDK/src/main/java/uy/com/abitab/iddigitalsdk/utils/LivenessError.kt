package uy.com.abitab.iddigitalsdk.utils

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class LivenessError(val code: Int, val message: String) : Parcelable