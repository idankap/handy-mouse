package com.example.handymouse

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var hidService: HidService? = null

    private val enableDiscoverableLauncher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode != 120) {
            // TODO: alert the user why he need to be discoverable
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

        hidService = HidService(this)

        val advertiseButton = findViewById<Button>(R.id.advertise)
        advertiseButton.setOnClickListener {
            callBluetoothAction {
                enableDiscoverableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE))
            }
        }

        val leftButton = findViewById<Button>(R.id.left)
        leftButton.setOnClickListener {

        }
        val middleButton = findViewById<Button>(R.id.middle)
        middleButton.setOnClickListener {

        }
        val rightButton = findViewById<Button>(R.id.right)
        rightButton.setOnClickListener {

        }
    }

    override fun onStart() {
        hidService?.register()
        super.onStart()
    }
}