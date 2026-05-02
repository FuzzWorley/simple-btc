package com.bitcoinportfolio.data.api.dto

import com.google.gson.annotations.SerializedName

data class MetalsLiveDto(
    @SerializedName("gold") val gold: Double?
)
