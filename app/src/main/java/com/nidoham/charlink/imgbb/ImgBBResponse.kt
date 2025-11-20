package com.nidoham.charlink.imgbb

import com.google.gson.annotations.SerializedName

data class ImgBBResponse(
    @SerializedName("data") val data: Data? = null,
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("status") val status: Int = 0
) {
    data class Data(
        @SerializedName("id") val id: String,
        @SerializedName("url") val url: String,
        @SerializedName("display_url") val displayUrl: String,
        @SerializedName("delete_url") val deleteUrl: String
    )
}