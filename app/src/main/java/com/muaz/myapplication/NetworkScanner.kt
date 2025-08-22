package com.muaz.myapplication

import android.app.AlertDialog
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*

class NetworkScanner {

    interface OnIpSelectedListener {
        fun onIpSelected(ip: String)
    }

    interface OnScanCompleteListener {
        fun onNetworkScanComplete(devices: List<String>, csvFileName: String)
    }

    @Volatile
    private var isScanning = true

    fun stopScan() {
        isScanning = false
    }

    fun scanNetwork(
        activity: AppCompatActivity,
        lifecycleScope: LifecycleCoroutineScope,
        statusText: TextView,
        progressText: TextView,
        ipListener: OnIpSelectedListener,
        scanCompleteListener: OnScanCompleteListener? = null
    ) {
        val discoveredIps = mutableSetOf<String>()

        // Get local device IP and subnet prefix
        val localIp = getLocalIpAddress()
        val subnet = localIp?.substringBeforeLast('.')?.plus('.') ?: "192.168.1."
        if (localIp != null) {
            discoveredIps.add(localIp)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            while (isScanning) {
                val newIps = mutableListOf<String>()

                withContext(Dispatchers.Main) {
                    progressText.text = "🔄 Starting scan on $subnet..."
                }

                for (i in 1..254) {
                    if (!isScanning) break
                    val ip = "$subnet$i"
                    try {
                        val inet = InetAddress.getByName(ip)
                        val reachable = try {
                            inet.isReachable(500)
                        } catch (e: Exception) {
                            false
                        }

                        if (reachable && discoveredIps.add(ip)) {
                            newIps.add(ip)
                        }

                    } catch (_: Exception) {}

                    if (i % 20 == 0) {
                        withContext(Dispatchers.Main) {
                            progressText.text =
                                "Scanning $subnet\nProgress: $i/254 (${(i.toFloat() / 254 * 100).toInt()}%)\nDiscovered so far: ${discoveredIps.size}"
                        }
                    }
                }

                // Ensure local IP is always present
                if (localIp != null) {
                    discoveredIps.add(localIp)
                }

                withContext(Dispatchers.Main) {
                    val spannable = SpannableStringBuilder()
                    spannable.append("📡 Found ${discoveredIps.size} devices on network:\n\n")

                    discoveredIps.forEach { ip ->
                        val prefix = "✓ "
                        val suffix = " is reachable (tap to select)\n"
                        val start = spannable.length + prefix.length
                        val end = start + ip.length

                        spannable.append(prefix)
                        spannable.append(ip)
                        spannable.append(suffix)

                        val clickableSpan = object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                ipListener.onIpSelected(ip)
                            }

                            override fun updateDrawState(ds: TextPaint) {
                                super.updateDrawState(ds)
                                ds.isUnderlineText = true
                                ds.color = android.graphics.Color.BLUE
                            }
                        }
                        spannable.setSpan(clickableSpan, start, end, 0)
                    }

                    spannable.append("\n✅ Scan complete! Tap any IP to scan ports.")
                    spannable.append("\n⏰ Last updated: ${SimpleDateFormat("HH:mm:ss").format(Date())}")

                    statusText.text = spannable
                    statusText.movementMethod = LinkMovementMethod.getInstance()

                    statusText.post {
                        val scrollAmount = statusText.layout?.getLineTop(statusText.lineCount) ?: 0
                        if (scrollAmount > statusText.height) {
                            statusText.scrollTo(0, scrollAmount)
                        }
                    }

                    progressText.text = "✅ Waiting 10 seconds before next scan..."
                }

                // Save CSV after first full scan
                if (newIps.isNotEmpty() && discoveredIps.size == newIps.size) {
                    val csvFileName = "network_scan_${System.currentTimeMillis()}.csv"
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(activity)
                            .setTitle("Save CSV File")
                            .setMessage("Do you want to save the initial scan result as a CSV file?")
                            .setPositiveButton("Yes") { _, _ ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    saveNetworkScanToCsv(activity, discoveredIps.toList(), csvFileName)
                                    withContext(Dispatchers.Main) {
                                        scanCompleteListener?.onNetworkScanComplete(
                                            discoveredIps.toList(),
                                            csvFileName
                                        )
                                    }
                                }
                            }
                            .setNegativeButton("No") { _, _ ->
                                scanCompleteListener?.onNetworkScanComplete(discoveredIps.toList(), "")
                            }
                            .show()
                    }
                }

                delay(10000) // Wait before next scan
                if (!isScanning) break 
            }

            withContext(Dispatchers.Main) {
                progressText.text = "⛔ Scan stopped."
            }
        }
    }

    private fun saveNetworkScanToCsv(activity: AppCompatActivity, devices: List<String>, fileName: String) {
        val csvHeader = "IP Address\n"
        val csvBody = devices.joinToString("\n")
        val csvContent = csvHeader + csvBody
        val mimeType = "text/csv"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = activity.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray())
                    outputStream.flush()
                }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = java.io.File(downloadsDir, fileName)
            file.writeText(csvContent)
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is InetAddress) {
                        val ip = addr.hostAddress
                        if (ip.contains(".")) {
                            return ip
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            // Ignore
        }
        return null
    }
}
