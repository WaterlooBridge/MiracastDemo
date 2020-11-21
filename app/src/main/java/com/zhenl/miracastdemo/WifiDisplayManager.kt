package com.zhenl.miracastdemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.milink.miracast.DisplayManagerCompat
import com.milink.ui.MiLinkApplication
import com.miui.wifidisplay.MiracastReceiver
import com.miui.wifidisplay.WifiDisplayAdmin

object WifiDisplayManager {

    private const val TAG = "WifiDisplayManager"

    private lateinit var context: Context
    lateinit var displayManager: DisplayManager

    fun attach(context: Context) {
        this.context = context
        displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val field = MiLinkApplication::class.java.getDeclaredField("sContext")
        field.isAccessible = true
        field.set(null, context)
        WifiDisplayAdmin.getInstance().openWifiDisplay()
    }

    fun startScan() {
        // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/java/android/hardware/display/DisplayManager.java
        val filter = IntentFilter(MiracastReceiver.WFD_STATUS_CHANGE_ACTION)
        context.registerReceiver(receiver, filter)
        DisplayManagerCompat.startWifiDisplayScan(displayManager)
    }

    fun stopScan() {
        context.unregisterReceiver(receiver)
        DisplayManagerCompat.stopWifiDisplayScan(displayManager)
    }

    val displays = MutableLiveData<List<WifiDisplay>>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val gson = Gson()
            val json = gson.toJson(WifiDisplayAdmin.getInstance().displays)
            Log.d(TAG, json)
            displays.value = gson.fromJson(json, object : TypeToken<List<WifiDisplay>>() {}.type)
        }
    }
}