package com.example.handymouse

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
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
        0x95, 0x02,     //     Report Count (3)
        0x81, 0x06,     //     Input (Data, Variable, Relative)
        0xC0,           //   End Collection
        0xC0,           // End Collection
    ).map { it.toByte() }.toByteArray()

    private val enableDiscoverableLauncher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == 120) {
            start()
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode != Activity.RESULT_OK) {
            // TODO: alert the user why he need to allow bluetooth
        }
    }

    private val BLUETOOTH_CONNECT_RQ = 1

    private var bluetoothAction = {}

    private fun callBluetoothAction(action: () -> Unit) {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            bluetoothAction = action
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                BLUETOOTH_CONNECT_RQ
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            BLUETOOTH_CONNECT_RQ -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAction()
                    bluetoothAction = {}
                } else {
                    // TODO: alert the user why he need to allow bluetooth
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val advertiseButton = findViewById<Button>(R.id.advertise)
        advertiseButton.setOnClickListener {
            callBluetoothAction {
                enableDiscoverableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE))
            }
        }

//        bluetoothManager.adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
    }

    private fun start() {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            getString(R.string.app_name),
            getString(R.string.app_description),
            getString(R.string.creator),
            BluetoothHidDevice.SUBCLASS1_MOUSE,
            reportDescriptor
        )

        var hid: BluetoothHidDevice? = null

        val hidCallback = object : BluetoothHidDevice.Callback() {
            @SuppressLint("MissingPermission")
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                Log.i("BluetoothHidDevice.Callback", "Called onAppStatusChanged with pluggedDevice: ${pluggedDevice}, registered: $registered")
            }

            @SuppressLint("MissingPermission")
            override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
                Log.i("BluetoothHidDevice.Callback", "Called onConnectionStateChanged with device: ${device}, state: $state")
            }
        }

        val profileListener = object : BluetoothProfile.ServiceListener {
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

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter.getProfileProxy(this, profileListener, BluetoothProfile.HID_DEVICE)
    }
}