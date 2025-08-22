package com.muaz.myapplication

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity(), NetworkScanner.OnIpSelectedListener {

    private lateinit var statusText: TextView
    private lateinit var progressText: TextView
    private lateinit var selectedIpText: TextView
    private lateinit var networkScanButton: Button
    private lateinit var portScanButton: Button
    private lateinit var stopScanButton: Button
    private lateinit var wifiScannerButton: Button

    private val networkScanner = NetworkScanner()
    private val portScanner = PortScanner()

    private var selectedIpAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        progressText = findViewById(R.id.progressText)
        selectedIpText = findViewById(R.id.selectedIpText)
        networkScanButton = findViewById(R.id.networkScanButton)
        portScanButton = findViewById(R.id.portScanButton)
        stopScanButton = findViewById(R.id.stopScanButton)
        wifiScannerButton = findViewById(R.id.wifiScannerButton)

        statusText.movementMethod = ScrollingMovementMethod.getInstance()
        portScanButton.isEnabled = false

        checkAndRequestAllPermissions() // ✅ Check all permissions on launch

        networkScanButton.setOnClickListener {
            if (checkNotificationPermission()) {
                startForegroundService()

                statusText.text = "🔍 Scanning started...\n"
                progressText.text = "Preparing to scan..."
                selectedIpAddress = null
                selectedIpText.text = "No IP selected"
                selectedIpText.setTextColor(android.graphics.Color.GRAY)
                portScanButton.isEnabled = false

                networkScanner.scanNetwork(
                    activity = this,
                    lifecycleScope = lifecycleScope,
                    statusText = statusText,
                    progressText = progressText,
                    ipListener = this
                )
            }
        }

        stopScanButton.setOnClickListener {
            stopForegroundService()
            networkScanner.stopScan()

            statusText.append("\n⛔ Scan stopped by user.")
            progressText.text = "Scan stopped."
            Toast.makeText(this, "Network scan stopped", Toast.LENGTH_SHORT).show()
            selectedIpAddress = null
            selectedIpText.text = "No IP selected"
            selectedIpText.setTextColor(android.graphics.Color.GRAY)
            portScanButton.isEnabled = false
        }

        portScanButton.setOnClickListener {
            if (selectedIpAddress != null) {
                statusText.append("\n🔍 Starting port scan for $selectedIpAddress...\n")
                statusText.append("⏳ Scanning common ports...\n")
                portScanner.scanPorts(lifecycleScope, statusText, selectedIpAddress!!)
            } else {
                Toast.makeText(this, "Please select an IP address first", Toast.LENGTH_LONG).show()
                statusText.append("\n❌ No IP address selected.\n")
            }
        }

        wifiScannerButton.setOnClickListener {
            val intent = Intent(this, WifiScannerActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onIpSelected(ip: String) {
        selectedIpAddress = ip
        selectedIpText.text = "Selected IP: $ip"
        selectedIpText.setTextColor(android.graphics.Color.GREEN)
        Toast.makeText(this, "✅ IP Selected: $ip", Toast.LENGTH_SHORT).show()
        statusText.append("\n🎯 Selected IP: $ip\n📡 Press 'Scan Ports' to begin port scanning.\n")
        portScanButton.isEnabled = true
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
                false
            } else true
        } else true
    }

    private fun checkAndRequestAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 101)
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, ForegroundService::class.java)
        intent.action = ForegroundService.ACTION_START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopForegroundService() {
        val intent = Intent(this, ForegroundService::class.java)
        intent.action = ForegroundService.ACTION_STOP
        startService(intent)
    }
}
