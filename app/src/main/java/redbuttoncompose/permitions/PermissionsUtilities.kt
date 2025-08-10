package redbuttoncompose.permitions

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionsUtilities {

    /**
     * Checks for a set of permissions. If not granted, the user is asked to grant them.
     */
    fun checkPermissions(activity: Activity, permissions: Array<out String>, requestCode: Int) {
        if (checkPermissionsGranted(activity, permissions)) return
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /**
     * Checks whether a set of permissions is granted or not.
     */
    fun checkPermissionsGranted(context: Context, permissions: Array<out String>): Boolean {
        return permissions.all { checkPermissionGranted(context, it) }
    }

    /**
     * Checks the result of a permission request, and dispatches the appropriate action.
     */
    fun dispatchOnRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray,
        onGrantedMap: Map<Int, () -> Unit>,
        onDeniedMap: Map<Int, () -> Unit>
    ) {
        if (checkGrantResults(grantResults)) {
            onGrantedMap[requestCode]?.invoke()
        } else {
            onDeniedMap[requestCode]?.invoke()
        }
    }

    /**
     * Checks whether a permission is granted in the context.
     */
    private fun checkPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks the results of a permission request.
     */
    private fun checkGrantResults(grantResults: IntArray): Boolean {
        return grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }

}
