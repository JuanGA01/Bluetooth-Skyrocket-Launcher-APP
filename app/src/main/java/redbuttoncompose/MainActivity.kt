package redbuttoncompose

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null
    private var isScanning = false

    private val targetDeviceName = "Rocket Launcher"
    private val serviceUUID: java.util.UUID? = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")
    private val characteristicUUID: java.util.UUID? = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")
    private val launchCode = "v4Y{/\u005CiOZ521#%.%" // Java escape for backslash

    private lateinit var viewModel: CountdownViewModel

    private val scanTimeoutHandler = Handler(Looper.getMainLooper())
    private val scanTimeoutRunnable = Runnable {
        if (isScanning) {
            stopScan()
            println("⏱️ Scan timeout. Stopped scanning.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        setContent {
            viewModel = viewModel()
            MainScreen(viewModel)
        }

        if (bluetoothAdapter.isEnabled) {
            requestPermissions()
        }
    }

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
                println("🚫 Permisos no concedidos")
            }
        }

    private fun startScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) return

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        scanCallback = object : ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (device.name == targetDeviceName) {
                    println("📡 Dispositivo detectado: ${device.name}")
                    stopScan()
                    connectToDevice(device)
                }
            }
        }

        bluetoothLeScanner.startScan(scanCallback)
        isScanning = true
        scanTimeoutHandler.postDelayed(scanTimeoutRunnable, 10_000)
    }

    private fun stopScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                bluetoothLeScanner.stopScan(scanCallback)
            } catch (e: SecurityException) {
                println("🚫 SecurityException al detener escaneo: ${e.message}")
            }
        }
        isScanning = false
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) return

        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    println("✅ Conectado a ${device.name}")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    println("❌ Desconectado de ${device.name}")
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(serviceUUID)
                    val characteristic = service?.getCharacteristic(characteristicUUID)

                    if (characteristic != null) {
                        characteristic.value = launchCode.toByteArray()
                        val success = gatt.writeCharacteristic(characteristic)
                        println("🚀 Enviando comando de lanzamiento: ${if (success) "OK" else "FALLÓ"}")
                        viewModel.startCountdown()
                    } else {
                        println("🚫 Característica no encontrada")
                    }
                } else {
                    println("🚫 Error descubriendo servicios: $status")
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    println("✅ Comando de lanzamiento escrito con éxito")
                } else {
                    println("🚫 Falló escritura del comando: $status")
                }
            }
        })
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroy() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothGatt?.close()
        }
        scanTimeoutHandler.removeCallbacks(scanTimeoutRunnable)
        super.onDestroy()
    }
}
