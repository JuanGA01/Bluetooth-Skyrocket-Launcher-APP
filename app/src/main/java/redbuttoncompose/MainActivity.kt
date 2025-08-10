package redbuttoncompose

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
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

    // --- NUEVAS estructuras ---
    private val remoteDevicesMap: MutableMap<String, BluetoothDevice> = mutableMapOf()
    private val attemptedBondAddresses: MutableSet<String> = mutableSetOf()
    private var pendingDeviceToBond: BluetoothDevice? = null
    // --------------------------

    @SuppressLint("NotifyDataSetChanged", "MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bleDeviceConnector = BleDeviceConnector(this)

        // RecyclerView handling (igual que antes)
        val rvFoundDevices = findViewById<View>(R.id.rv_found_devices) as RecyclerView
        foundDevices = BleDevice.createBleDevicesList()
        val adapter = BleDeviceAdapter(foundDevices)
        rvFoundDevices.adapter = adapter
        rvFoundDevices.layoutManager = LinearLayoutManager(this)

        // Gesture detector / click listener (llama a onDeviceClicked de la Activity)
        val gestureDetector =
            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean { return true }
            })

        rvFoundDevices.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val child = rv.findChildViewUnder(e.x, e.y) ?: return false
                if (gestureDetector.onTouchEvent(e)) {
                    val position = rv.getChildAdapterPosition(child)
                    this@MainActivity.onDeviceClicked(position)
                    return true
                }
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })

        // BleManager creation y callback — guardamos el BluetoothDevice real y lanzamos bonding automático
        btManager = getSystemService(BluetoothManager::class.java)
        bleScanManager = BleScanManager(btManager, 5000, scanCallback = BleScanCallback(
            allowedName = "Rocket Launcher",
            onAllowedDeviceFound = { btDevice ->
                val address = btDevice.address ?: return@BleScanCallback

                // Guarda el BluetoothDevice real para operar después
                remoteDevicesMap[address] = btDevice

                // Actualiza RecyclerView
                val device = BleDevice(address)
                if (!foundDevices.contains(device)) {
                    foundDevices.add(device)
                    adapter.notifyItemInserted(foundDevices.size - 1)
                }

                // Intento automático de bonding
                attemptAutoBond(btDevice)
            }
        ))

        // Actions before/after scan
        bleScanManager.beforeScanActions.add { btnStartScan.isEnabled = false }
        bleScanManager.beforeScanActions.add {
            foundDevices.clear()
            adapter.notifyDataSetChanged()
        }
        bleScanManager.afterScanActions.add { btnStartScan.isEnabled = true }

        // Start button (backup)
        btnStartScan = findViewById(R.id.btn_start_scan)
        btnStartScan.setOnClickListener {
            when (PermissionsUtilities.checkPermissionsGranted(this, permissions)) {
                true -> bleScanManager.scanBleDevices()
                false -> PermissionsUtilities.checkPermissions(this, permissions, BLE_PERMISSION_REQUEST_CODE)
            }
        }

        // --- Auto start scan al arrancar (si permisos ya concedidos) ---
        if (PermissionsUtilities.checkPermissionsGranted(this, permissions)) {
            bleScanManager.scanBleDevices()
        } else {
            PermissionsUtilities.checkPermissions(this, permissions, BLE_PERMISSION_REQUEST_CODE)
        }
    }

    // Manejar click en item: (usa el BluetoothDevice guardado)
    @SuppressLint("MissingPermission")
    private fun onDeviceClicked(position: Int) {
        if (position < 0 || position >= foundDevices.size) return
        val address = foundDevices[position].name
        val btDevice = remoteDevicesMap[address]
        if (btDevice == null) {
            Toast.makeText(this@MainActivity, "Device object not available", Toast.LENGTH_SHORT).show()
            return
        }

        // Evitar reintentos
        if (attemptedBondAddresses.contains(address)) {
            Toast.makeText(this@MainActivity, "Bond already attempted for $address", Toast.LENGTH_SHORT).show()
            return
        }

        // Si faltan permisos, guarda en pending y pide permisos
        if (!PermissionsUtilities.checkPermissionsGranted(this, permissions)) {
            pendingDeviceToBond = btDevice
            PermissionsUtilities.checkPermissions(this, permissions, BLE_PERMISSION_REQUEST_CODE)
            return
        }

        // Lanzar bonding/connect
        attemptedBondAddresses.add(address)
        bleDeviceConnector.connect(btDevice)
        Toast.makeText(this@MainActivity, "Attempting to bond/connect to $address", Toast.LENGTH_SHORT).show()
    }

    // Intento automático invocado desde el callback de scan
    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("MissingPermission")
    private fun attemptAutoBond(btDevice: BluetoothDevice) {
        val address = btDevice.address ?: return

        // No reintentar si ya intentado
        if (attemptedBondAddresses.contains(address)) return
        attemptedBondAddresses.add(address)

        // Si faltan permisos, guarda y pide (se reintentará en onRequestPermissionsResult)
        if (!PermissionsUtilities.checkPermissionsGranted(this, permissions)) {
            pendingDeviceToBond = btDevice
            PermissionsUtilities.checkPermissions(this, permissions, BLE_PERMISSION_REQUEST_CODE)
            return
        }

        // Conectar / crear bond
        bleDeviceConnector.connect(btDevice)
        Toast.makeText(this@MainActivity, "Automatic bonding started for $address", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        dispatchOnRequestPermissionsResult(
            requestCode,
            grantResults,
            onGrantedMap = mapOf(BLE_PERMISSION_REQUEST_CODE to {
                // Si hay un dispositivo pendiente de bond, conéctalo; si no, inicia el escaneo
                pendingDeviceToBond?.let { device ->
                    // comprobación extra por si acaso
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED) {
                        bleDeviceConnector.connect(device)
                    } else {
                        // permiso aún no concedido (raro), pedir de nuevo o notificar
                        PermissionsUtilities.checkPermissions(this, permissions, BLE_PERMISSION_REQUEST_CODE)
                    }
                    pendingDeviceToBond = null
                } ?: run {
                    // No había pendiente: arranca el escaneo (comportamiento previo)
                    bleScanManager.scanBleDevices()
                }
            }),
            onDeniedMap = mapOf(BLE_PERMISSION_REQUEST_CODE to {
                Toast.makeText(this,
                    "Some permissions were not granted, please grant them and try again",
                    Toast.LENGTH_LONG).show()
                pendingDeviceToBond = null
            })
        )
    }

    companion object {
        private const val BLE_PERMISSION_REQUEST_CODE = 1
    }
}
