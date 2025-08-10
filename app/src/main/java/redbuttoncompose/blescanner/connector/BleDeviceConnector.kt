package redbuttoncompose.blescanner.connector

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat

class BleDeviceConnector(context: Context) {
    private val appContext = context.applicationContext
    private var gatt: BluetoothGatt? = null
    private val bondReceiver = object: BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                // manejar BOND_BONDED, BOND_NONE, BOND_BONDING
            } else if (action == BluetoothDevice.ACTION_PAIRING_REQUEST) {
                // manejar PIN / confirmación si es necesario
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        }
        appContext.registerReceiver(bondReceiver, filter)
    }


    fun connect(device: BluetoothDevice) {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Sin BLUETOOTH_CONNECT — solicita permiso antes de conectar")
            return
        }

        if (device.bondState == BluetoothDevice.BOND_NONE) {
            val started = device.createBond()
            Log.d(TAG, "createBond started = $started")
        }

        gatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object: BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "GATT error: $status")
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected")
                gatt.close() // cerrar para liberar recursos
                this@BleDeviceConnector.gatt = null
            }
        }
        // ...
    }

    @SuppressLint("MissingPermission")
    fun cleanup() {
        try {
            appContext.unregisterReceiver(bondReceiver)
        } catch (e: IllegalArgumentException) { /* ignore */ }
        gatt?.let {
            it.disconnect()
            it.close()
            gatt = null
        }
    }

}

