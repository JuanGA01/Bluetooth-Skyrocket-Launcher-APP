package redbuttoncompose

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.redbutton.redbuttoncompose.R
import redbuttoncompose.blescanner.BleScanManager
import redbuttoncompose.blescanner.adapter.BleDeviceAdapter
import redbuttoncompose.blescanner.connector.BleDeviceConnector
import redbuttoncompose.blescanner.model.BleDevice
import redbuttoncompose.blescanner.model.BleScanCallback
import redbuttoncompose.permitions.PermissionsUtilities
import redbuttoncompose.permitions.PermissionsUtilities.dispatchOnRequestPermissionsResult
import redbuttoncompose.permitions.permissions

class MainActivity : ComponentActivity()  {
    private lateinit var btnStartScan: Button
    private lateinit var btManager: BluetoothManager
    private lateinit var bleScanManager: BleScanManager
    private lateinit var bleDeviceConnector: BleDeviceConnector
    private lateinit var foundDevices: MutableList<BleDevice>

    @SuppressLint("NotifyDataSetChanged", "MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bleDeviceConnector = BleDeviceConnector(this)

        // RecyclerView handling
        val rvFoundDevices = findViewById<View>(R.id.rv_found_devices) as RecyclerView
        foundDevices = BleDevice.createBleDevicesList()
        val adapter = BleDeviceAdapter(foundDevices)
        rvFoundDevices.adapter = adapter
        rvFoundDevices.layoutManager = LinearLayoutManager(this)

        // BleManager creation
        btManager = getSystemService(BluetoothManager::class.java)
        bleScanManager = BleScanManager(btManager, 5000, scanCallback = BleScanCallback(
            allowedName = "Rocket Launcher",
            onAllowedDeviceFound = {
                val name = it.address
                if (name.isNullOrBlank()) return@BleScanCallback

                val device = BleDevice(name)
                if (!foundDevices.contains(device)) {
                    foundDevices.add(device)
                    adapter.notifyItemInserted(foundDevices.size - 1)
                }
            }
        ))


        // Adding the actions the manager must do before and after scanning
        bleScanManager.beforeScanActions.add { btnStartScan.isEnabled = false }
        bleScanManager.beforeScanActions.add {
            foundDevices.clear()
            adapter.notifyDataSetChanged()
        }
        bleScanManager.afterScanActions.add { btnStartScan.isEnabled = true }

        // Adding the onclick listener to the start scan button
        btnStartScan = findViewById(R.id.btn_start_scan)
        btnStartScan.setOnClickListener {
            // Checks if the required permissions are granted and starts the scan if so, otherwise it requests them
            when (PermissionsUtilities.checkPermissionsGranted(
                this,
                permissions
            )) {
                true -> bleScanManager.scanBleDevices()
                false -> PermissionsUtilities.checkPermissions(
                    this, permissions, BLE_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun bondDevice(device: BluetoothDevice) {
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            Toast.makeText(this, "Device already bonded", Toast.LENGTH_SHORT).show()
            return
        }
        device.createBond()
        Toast.makeText(this, "Bonding started", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        dispatchOnRequestPermissionsResult(
            requestCode,
            grantResults,
            onGrantedMap = mapOf(BLE_PERMISSION_REQUEST_CODE to { bleScanManager.scanBleDevices() }),
            onDeniedMap = mapOf(BLE_PERMISSION_REQUEST_CODE to { Toast.makeText(this,
                "Some permissions were not granted, please grant them and try again",
                Toast.LENGTH_LONG).show() })
        )
    }

    companion object {
        private const val BLE_PERMISSION_REQUEST_CODE = 1
    }

}