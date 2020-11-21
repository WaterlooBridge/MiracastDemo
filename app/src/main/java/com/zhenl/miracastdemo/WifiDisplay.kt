package com.zhenl.miracastdemo

import com.google.gson.annotations.SerializedName

class WifiDisplay {

    @SerializedName("mDeviceName")
    val deviceName: String? = null

    @SerializedName("mDeviceAddress")
    val deviceAddress: String? = null

    override fun toString(): String {
        return deviceName ?: "unknown"
    }
}