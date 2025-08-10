package redbuttoncompose.blescanner.model

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult

class BleScanCallback(
    private val allowedName: String? = null,
    private val onAllowedDeviceFound: (BluetoothDevice) -> Unit = {},
    private val onScanFailedAction: (Int) -> Unit = {}
) : ScanCallback() {

    override fun onScanResult(callbackType: Int, result: ScanResult?) {
        super.onScanResult(callbackType, result)
        result?.let {
            if (isAllowed(it)) {
                onAllowedDeviceFound(it.device)
            }
        }
    }

    override fun onBatchScanResults(results: MutableList<ScanResult>?) {
        super.onBatchScanResults(results)
        results?.forEach {
            if (isAllowed(it)) {
                onAllowedDeviceFound(it.device)
            }
        }
    }

    override fun onScanFailed(errorCode: Int) {
        super.onScanFailed(errorCode)
        onScanFailedAction(errorCode)
    }

    @SuppressLint("MissingPermission")
    private fun isAllowed(result: ScanResult): Boolean {
        val deviceName = result.device.name
        return allowedName == null || (deviceName != null && allowedName == deviceName)
    }

}
