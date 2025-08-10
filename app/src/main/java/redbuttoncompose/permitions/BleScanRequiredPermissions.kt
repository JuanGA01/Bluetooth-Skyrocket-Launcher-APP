package redbuttoncompose.permitions

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.S)
val permissions = arrayOf(
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.BLUETOOTH_ADMIN,
    Manifest.permission.BLUETOOTH_CONNECT
)