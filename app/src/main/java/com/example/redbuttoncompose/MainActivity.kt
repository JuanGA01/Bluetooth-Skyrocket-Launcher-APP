package com.example.redbuttoncompose

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var isScanning = false
    private val targetDeviceName = "Complejo de lanzamiento Cavo cañaberal"
    private var bluetoothGatt: BluetoothGatt? = null


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initBluetooth()

        setContent {
            val vm: CountdownViewModel = viewModel()
            MainScreen(vm)
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun initBluetooth() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter.isEnabled) {
            requestPermissions()
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun requestPermissions() {
        val permissions = listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            startScan()
        }
    }


    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (
                result[Manifest.permission.BLUETOOTH_SCAN] == true &&
                result[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            ) {
                startScan()
            } else {
                println("🚫 Permisos requeridos no fueron concedidos")
            }
        }


    private fun startScan() {
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            println("🚫 Permiso BLUETOOTH_SCAN no concedido")
            return
        }

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        val scanCallback = object : ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (device.name == targetDeviceName) {
                    if (
                        ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        println("🚫 Permiso BLUETOOTH_SCAN no concedido (stopScan)")
                        return
                    }
                    bluetoothLeScanner.stopScan(this)
                    connectToDevice(device)
                }
            }
        }

        bluetoothLeScanner.startScan(scanCallback)
        isScanning = true
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectToDevice(device: BluetoothDevice) {
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            println("🚫 Permiso BLUETOOTH_CONNECT no concedido")
            return
        }

        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (
                    ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    println("🚫 Permiso BLUETOOTH_CONNECT no concedido en callback")
                    return
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    println("✅ Conectado a ${device.name}")
                } else {
                    println("❌ Desconectado de ${device.name}")
                }
            }
        })
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroy() {
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothGatt?.close()
        }
        super.onDestroy()
    }


}
