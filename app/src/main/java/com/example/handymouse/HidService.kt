package com.example.handymouse

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import java.util.concurrent.Executors

class HidService(base: Context?) : ContextWrapper(base) {
    private val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

    private var hid: BluetoothHidDevice? = null

    private val reportDescriptor = listOf(
        0x05, 0x01,     // Usage Page (Generic Desktop)
        0x09, 0x02,     // Usage (Mouse)
        0xA1, 0x01,     // Collection (Application)
        0x09, 0x01,     //   Usage (Pointer)
        0xA1, 0x00,     //   Collection (Physical)
        0x05, 0x09,     //     Usage Page (Button)
        0x19, 0x01,     //     Usage Minimum (1)
        0x29, 0x03,     //     Usage Maximum (3)
        0x15, 0x00,     //     Logical Minimum (0)
        0x25, 0x01,     //     Logical Maximum (1)
        0x95, 0x03,     //     Report Count (3)
        0x75, 0x01,     //     Report Size (1)
        0x81, 0x02,     //     Input (Data, Variable, Absolute)
        0x05, 0x01,     //     Usage Page (Generic Desktop)
        0x09, 0x30,     //     Usage (X)
        0x09, 0x31,     //     Usage (Y)
        0x15, 0x81,     //     Logical Minimum (-127)
        0x25, 0x7F,     //     Logical Maximum (127)
        0x75, 0x08,     //     Report Size (8)
        0x95, 0x02,     //     Report Count (2)
        0x81, 0x06,     //     Input (Data, Variable, Relative)
        0xC0,           //   End Collection
        0xC0,           // End Collection
    ).map { it.toByte() }.toByteArray()

    private val sdp = BluetoothHidDeviceAppSdpSettings(
        getString(R.string.app_name),
        getString(R.string.app_description),
        getString(R.string.creator),
        BluetoothHidDevice.SUBCLASS1_MOUSE,
        reportDescriptor
    )

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        @SuppressLint("MissingPermission")
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.i("BluetoothHidDevice.Callback", "Called onAppStatusChanged with pluggedDevice: ${pluggedDevice}, registered: $registered")
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.i("BluetoothHidDevice.Callback", "Called onConnectionStateChanged with device: ${device}, state: $state")
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.i("BluetoothProfile.ServiceListener", "onServiceConnected with an HID device")
                hid = proxy as BluetoothHidDevice
                hid!!.registerApp(sdp, null, null, Executors.newSingleThreadExecutor(), hidCallback)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.i("BluetoothProfile.ServiceListener", "onServiceDisconnected with an HID device")
                hid!!.unregisterApp()
                hid = null
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun registerHid() {
        Log.i("registedHid", bluetoothManager.adapter.name)
        bluetoothManager.adapter.getProfileProxy(this, profileListener, BluetoothProfile.HID_DEVICE)
    }
}