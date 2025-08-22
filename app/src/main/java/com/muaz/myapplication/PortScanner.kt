package com.muaz.myapplication

import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

class PortScanner {

    interface OnPortScanCompleteListener {
        fun onPortScanComplete(openPorts: List<Int>, csvFileName: String)
    }

    fun scanPorts(
        scope: LifecycleCoroutineScope,
        statusText: TextView,
        targetIp: String = "192.168.18.233",
        scanCompleteListener: OnPortScanCompleteListener? = null
    ) {
        scope.launch(Dispatchers.IO) {
            val openPorts = mutableListOf<Int>()
            val startPort = 1
            val endPort = 1024

            withContext(Dispatchers.Main) {
                @Suppress("SetTextI18n")
                statusText.text = "${statusText.text}Starting port scan on $targetIp\nScanning ports $startPort-$endPort...\n\n"
            }

            val startTime = System.currentTimeMillis()

            for (port in startPort..endPort) {
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(targetIp, port), 200)
                    socket.close()
                    openPorts.add(port)

                    withContext(Dispatchers.Main) {
                        @Suppress("SetTextI18n")
                        statusText.text = "${statusText.text}✓ Port $port is open (${getServiceName(port)})\n"
                    }
                } catch (_: Exception) {
                    // Closed or unreachable
                }

                if (port % 50 == 0) {
                    withContext(Dispatchers.Main) {
                        @Suppress("SetTextI18n")
                        statusText.text = "${statusText.text}Scanning... $port/$endPort (${(port.toFloat() / endPort * 100).toInt()}%)\n"
                    }
                }
            }

            val scanDuration = (System.currentTimeMillis() - startTime) / 1000.0

            withContext(Dispatchers.Main) {
                if (openPorts.isEmpty()) {
                    @Suppress("SetTextI18n")
                    statusText.text = "${statusText.text}\nScan complete. No open ports found on $targetIp\nScan duration: $scanDuration seconds\n"
                } else {
                    @Suppress("SetTextI18n")
                    statusText.text = "${statusText.text}\nScan complete. Found ${openPorts.size} open ports on $targetIp\nScan duration: $scanDuration seconds\n"
                }
            }

            // Ask user whether to save CSV
            val csvFileName = "port_scan_${targetIp.replace('.', '_')}_${System.currentTimeMillis()}.csv"

            withContext(Dispatchers.Main) {
                android.app.AlertDialog.Builder(statusText.context)
                    .setTitle("Save CSV File")
                    .setMessage("Do you want to save the port scan result as a CSV file?")
                    .setPositiveButton("Yes") { _, _ ->
                        scope.launch(Dispatchers.IO) {
                            savePortScanToCsv(statusText.context, openPorts, csvFileName)
                            withContext(Dispatchers.Main) {
                                scanCompleteListener?.onPortScanComplete(openPorts, csvFileName)
                            }
                        }
                    }
                    .setNegativeButton("No") { _, _ ->
                        scanCompleteListener?.onPortScanComplete(openPorts, "")
                    }
                    .show()
            }
        }
    }

    private fun savePortScanToCsv(context: android.content.Context, openPorts: List<Int>, fileName: String) {
        val csvHeader = "Port,Service\n"
        val csvBody = openPorts.joinToString("\n") { port -> "$port,${getServiceName(port)}" }
        val csvContent = csvHeader + csvBody
        val mimeType = "text/csv"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
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

    private fun getServiceName(port: Int): String {
        return when (port) {
            20 -> "FTP Data"
            21 -> "FTP"
            22 -> "SSH"
            23 -> "Telnet"
            25 -> "SMTP"
            53 -> "DNS"
            80 -> "HTTP"
            110 -> "POP3"
            139 -> "NetBIOS"
            143 -> "IMAP"
            443 -> "HTTPS"
            445 -> "SMB"
            3306 -> "MySQL"
            3389 -> "RDP"
            8080 -> "HTTP-Alt"
            else -> "Unknown"
        }
    }
}
