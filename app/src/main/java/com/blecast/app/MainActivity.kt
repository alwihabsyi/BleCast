package com.blecast.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.blecast.app.databinding.ActivityMainBinding
import com.blecast.app.datastore.PreferencesDataStore
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferencesDataStore: PreferencesDataStore
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var isCoroutineLaunched = false
    private var serial = ""
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            val isGranted = it.value
            if (!isGranted && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(
                    this, "Harap berikan izin untuk melanjutkan", Toast.LENGTH_SHORT
                ).show()
                return@registerForActivityResult
            }
        }

        checkDevice()
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra("MESSAGE")?.let { message ->
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferencesDataStore = PreferencesDataStore.getInstance(this)

        if (!hasPermissions()) {
            requestPermissionsLauncher.launch(permissions)
            return
        } else {
            checkDevice()
        }
    }

    private fun hasPermissions(): Boolean = permissions.all {
        ContextCompat.checkSelfPermission(
            this, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkDevice() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        CoroutineScope(Dispatchers.Main).launch {
            serial = preferencesDataStore.getString(SERIAL).first()
            if (bluetoothAdapter.state == BluetoothAdapter.STATE_ON) {
                if (serial.isEmpty()) {
                    askForSerial()
                } else {
                    startCastService(serial)
                    binding.tvCastingInfo.text = getString(R.string.cast_with_id, serial)
                }
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Nyalakan bluetooth untuk melanjutkan",
                    Toast.LENGTH_SHORT
                ).show()
            }

            isCoroutineLaunched = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun askForSerial() {
        if (!isCoroutineLaunched) {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_serial_input, null)
            val editText = dialogView.findViewById<EditText>(R.id.et_serial)

            val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()

            dialogView.findViewById<MaterialButton>(R.id.btn_send).setOnClickListener {
                val inputText = editText.text.toString()
                lifecycleScope.launch { preferencesDataStore.setString(SERIAL, inputText) }
                binding.tvCastingInfo.text = getString(R.string.cast_with_id, inputText)
                bluetoothAdapter.setName(inputText)
                startCastService(inputText)
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun startCastService(serial: String) {
        val filter = IntentFilter("com.blecast.app.ADVERTISE_CALLBACK")
        registerReceiver(broadcastReceiver, filter)

        val intent = Intent(this, CastService::class.java).apply {
            putExtra("SERIAL", serial)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        val SERIAL = stringPreferencesKey("serial")
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }

            else -> {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            }
        }
    }
}