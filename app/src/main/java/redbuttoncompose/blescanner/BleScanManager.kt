package redbuttoncompose.blescanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.os.Handler
import android.os.Looper
import redbuttoncompose.blescanner.model.BleScanCallback

class BleScanManager(
    btManager: BluetoothManager,
    private val scanPeriod: Long = DEFAULT_SCAN_PERIOD,
    private val scanCallback: BleScanCallback = BleScanCallback()
) {
    private val btAdapter = btManager.adapter
    private val bleScanner = btAdapter.bluetoothLeScanner
    private val handler = Handler(Looper.getMainLooper())
    private var scanning = false
    var beforeScanActions: MutableList<() -> Unit> = mutableListOf()
    var afterScanActions: MutableList<() -> Unit> = mutableListOf()


    @SuppressLint("MissingPermission")
    fun scanBleDevices() {
        fun stopScan() {
            scanning = false
            bleScanner.stopScan(scanCallback)

            // execute all the functions to execute after scanning
            executeAfterScanActions()
        }

        // scans for bluetooth LE devices
        if (scanning) {
            stopScan()
        } else {
            // stops scanning after scanPeriod millis
            handler.postDelayed({ stopScan() }, scanPeriod)
            // execute all the functions to execute before scanning
            executeBeforeScanActions()

            // starts scanning
            scanning = true
            bleScanner.startScan(scanCallback)
        }
    }

    companion object {
        const val DEFAULT_SCAN_PERIOD: Long = 10000

        private fun executeListOfFunctions(toExecute: List<() -> Unit>) {
            toExecute.forEach {
                it()
            }
        }
    }

    private fun executeBeforeScanActions() {
        executeListOfFunctions(beforeScanActions)
    }

    private fun executeAfterScanActions() {
        executeListOfFunctions(afterScanActions)
    }

}