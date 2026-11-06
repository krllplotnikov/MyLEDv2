package com.example.myled

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myled.data.AppTheme
import com.example.myled.ui.screens.AppScreen
import com.example.myled.ui.theme.DarkNeuColors
import com.example.myled.ui.theme.LightNeuColors
import com.example.myled.ui.theme.MyLEDTheme
import androidx.compose.ui.graphics.toArgb

class MainActivity : ComponentActivity() {

    private val vm: LedViewModel by viewModels()

    private val requiredPermissions: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) ensureBluetoothOn()
            else Toast.makeText(this, R.string.bt_permission_needed, Toast.LENGTH_LONG).show()
        }

    private val enableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (vm.ble.isBluetoothEnabled) vm.onBluetoothReady()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by vm.theme.collectAsStateWithLifecycle()
            val dark = theme == AppTheme.DARK
            LaunchedEffect(dark) {
                val bg = (if (dark) DarkNeuColors else LightNeuColors).background.toArgb()
                val style = if (dark) SystemBarStyle.dark(bg) else SystemBarStyle.light(bg, bg)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            MyLEDTheme(dark = dark) {
                AppScreen(vm, onRequestBluetooth = ::startBluetooth)
            }
        }
        startBluetooth()
    }

    private fun startBluetooth() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray()) else ensureBluetoothOn()
    }

    private fun ensureBluetoothOn() {
        if (vm.ble.isBluetoothEnabled) {
            vm.onBluetoothReady()
        } else {
            try {
                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } catch (e: SecurityException) {
                Toast.makeText(this, R.string.bt_disabled, Toast.LENGTH_LONG).show()
            }
        }
    }
}
