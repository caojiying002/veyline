package com.veyline.app.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal const val MERCHANT_PROVINCE_SELECTION_RESULT_KEY = "merchant_province_selection_result"

@Parcelize
internal data class MerchantProvinceSelectionResult(
    val code: String?,
    val name: String?,
) : Parcelable
