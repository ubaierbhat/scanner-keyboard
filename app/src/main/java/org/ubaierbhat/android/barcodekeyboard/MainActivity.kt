package org.ubaierbhat.android.barcodekeyboard

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import org.ubaierbhat.android.barcodekeyboard.service.BarcodeKeyboardService

class MainActivity : AppCompatActivity() {

    private lateinit var cameraStatusChip: TextView
    private lateinit var cameraActionButton: MaterialButton
    private lateinit var imeStatusChip: TextView
    private lateinit var imeActionButton: MaterialButton
    private lateinit var overallStatus: TextView
    private var cameraPermissionRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        cameraStatusChip = findViewById(R.id.camera_status_chip)
        cameraActionButton = findViewById(R.id.camera_action_button)
        imeStatusChip = findViewById(R.id.ime_status_chip)
        imeActionButton = findViewById(R.id.ime_action_button)
        overallStatus = findViewById(R.id.overall_status)

        val testFieldsButton = findViewById<MaterialButton>(R.id.open_test_fields_button)
        if (BuildConfig.DEBUG) {
            testFieldsButton.visibility = android.view.View.VISIBLE
            testFieldsButton.setOnClickListener {
                val intent = Intent().setComponent(
                    android.content.ComponentName(
                        packageName,
                        "org.ubaierbhat.android.barcodekeyboard.TestFieldsActivity",
                    ),
                )
                try {
                    startActivity(intent)
                } catch (e: android.content.ActivityNotFoundException) {
                }
            }
        }

        cameraActionButton.setOnClickListener {
            if (isCameraPermanentlyDenied()) {
                openAppSettings()
            } else {
                cameraPermissionRequested = true
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA
                )
            }
        }
        imeActionButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshStatus()
    }

    private fun refreshStatus() {
        val cameraGranted = isCameraGranted()
        val imeEnabled = isImeEnabled()

        if (cameraGranted) {
            cameraStatusChip.setText(R.string.status_granted)
            styleChip(cameraStatusChip, R.color.status_ok_text, R.color.status_ok_container)
        } else {
            cameraStatusChip.setText(R.string.status_not_granted)
            styleChip(cameraStatusChip, R.color.status_pending_text, R.color.status_pending_container)
        }

        if (isCameraPermanentlyDenied()) {
            cameraActionButton.setText(R.string.setup_step_camera_action_settings)
        } else {
            cameraActionButton.setText(R.string.setup_step_camera_action)
        }

        if (imeEnabled) {
            imeStatusChip.setText(R.string.status_enabled)
            styleChip(imeStatusChip, R.color.status_ok_text, R.color.status_ok_container)
        } else {
            imeStatusChip.setText(R.string.status_not_enabled)
            styleChip(imeStatusChip, R.color.status_pending_text, R.color.status_pending_container)
        }

        when {
            cameraGranted && imeEnabled -> overallStatus.setText(R.string.setup_overall_ready)
            !cameraGranted -> overallStatus.setText(R.string.setup_next_camera)
            else -> overallStatus.setText(R.string.setup_next_ime)
        }
    }

    private fun isCameraGranted(): Boolean = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED

    private fun isCameraPermanentlyDenied(): Boolean = !isCameraGranted() &&
        cameraPermissionRequested &&
        !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun isImeEnabled(): Boolean {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val componentName = ComponentName(this, BarcodeKeyboardService::class.java)
        return inputMethodManager.enabledInputMethodList.any {
            ComponentName(it.packageName, it.serviceName) == componentName
        }
    }

    private fun styleChip(chip: TextView, textColorRes: Int, containerColorRes: Int) {
        chip.setTextColor(ContextCompat.getColor(this, textColorRes))
        chip.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, containerColorRes))
    }

    private companion object {
        const val REQUEST_CAMERA = 100
    }
}
