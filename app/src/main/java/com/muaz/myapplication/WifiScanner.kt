package com.muaz.myapplication

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WifiScannerActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var scanButton: Button
    private lateinit var statusText: TextView
    private lateinit var wifiListRecyclerView: RecyclerView
    private lateinit var wifiAdapter: WifiAdapter

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val success = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) ?: false
            if (success) {
                scanSuccess()
            } else {
                scanFailure()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_scanner)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        scanButton = findViewById(R.id.wifiScanButton)
        statusText = findViewById(R.id.wifiStatusText)
        wifiListRecyclerView = findViewById(R.id.wifiListRecyclerView)

        wifiAdapter = WifiAdapter()
        wifiListRecyclerView.layoutManager = LinearLayoutManager(this)
        wifiListRecyclerView.adapter = wifiAdapter

        scanButton.setOnClickListener {
            if (checkAndRequestPermissions()) {
                startWifiScan()
            }
        }

        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)

        statusText.text = "📶 Ready to scan WiFi networks"
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        } else {
            // For Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }

        return if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 200)
            statusText.text = "⏳ Requesting WiFi permissions..."
            false
        } else {
            true
        }
    }

    private fun startWifiScan() {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Please enable WiFi first", Toast.LENGTH_SHORT).show()
            statusText.text = "❌ WiFi is disabled. Enable WiFi and try again."
            return
        }

        scanButton.isEnabled = false
        statusText.text = "🔍 Scanning for nearby WiFi networks..."
        wifiAdapter.clearNetworks()

        try {
            val success = wifiManager.startScan()
            if (!success) scanFailure()
        } catch (e: Exception) {
            scanFailure()
            Toast.makeText(this, "Scan error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    private fun scanSuccess() {
        try {
            val results = wifiManager.scanResults
            if (results.isNotEmpty()) {
                statusText.text = "✅ Found ${results.size} WiFi networks"
                wifiAdapter.updateNetworks(results)
            } else {
                statusText.text = "❌ No WiFi networks found"
            }
        } catch (e: Exception) {
            statusText.text = "❌ Error accessing WiFi results"
            Toast.makeText(this, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        scanButton.isEnabled = true
    }

    private fun scanFailure() {
        statusText.text = "❌ WiFi scan failed"
        Toast.makeText(this, "Failed to scan WiFi networks", Toast.LENGTH_SHORT).show()
        scanButton.isEnabled = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startWifiScan()
        } else {
            Toast.makeText(this, "Permission denied for WiFi scan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(wifiScanReceiver)
        } catch (_: Exception) {}
    }

    // ──────────────────────────────
    // Adapter Class for Displaying WiFi Networks
    // ──────────────────────────────
    inner class WifiAdapter : RecyclerView.Adapter<WifiAdapter.WifiViewHolder>() {
        private val wifiNetworks = mutableListOf<ScanResult>()

        fun updateNetworks(networks: List<ScanResult>) {
            wifiNetworks.clear()
            wifiNetworks.addAll(networks.sortedByDescending { it.level })
            notifyDataSetChanged()
        }

        fun clearNetworks() {
            wifiNetworks.clear()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return WifiViewHolder(view)
        }

        override fun onBindViewHolder(holder: WifiViewHolder, position: Int) {
            holder.bind(wifiNetworks[position])
        }

        override fun getItemCount(): Int = wifiNetworks.size

        inner class WifiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val text1: TextView = itemView.findViewById(android.R.id.text1)
            private val text2: TextView = itemView.findViewById(android.R.id.text2)

            fun bind(network: ScanResult) {
                val strength = when {
                    network.level >= -50 -> "Excellent"
                    network.level >= -60 -> "Good"
                    network.level >= -70 -> "Fair"
                    else -> "Weak"
                }

                val security = when {
                    network.capabilities.contains("WPA3") -> "WPA3"
                    network.capabilities.contains("WPA2") -> "WPA2"
                    network.capabilities.contains("WPA") -> "WPA"
                    network.capabilities.contains("WEP") -> "WEP"
                    else -> "Open"
                }

                val ssid = network.SSID.ifEmpty { "Hidden Network" }
                text1.text = "$ssid ($strength)"
                text2.text = "Signal: ${network.level} dBm | $security | ${network.frequency} MHz"
            }
        }
    }
}
