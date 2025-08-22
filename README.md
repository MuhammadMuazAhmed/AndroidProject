# 🔍 Android Network & Port Scanner

An Android app built with **Kotlin** that allows users to:

- ✅ Scan devices on the local network (LAN)  
- ✅ Select an IP address from scanned devices  
- ✅ Run a **port scan** on the selected IP (common ports)  
- ✅ Scan nearby Wi-Fi networks  
- ✅ Run continuously in the background using a **Foreground Service**  

---

## 🚀 Features

- 📡 **Network Scanning** – Detect active devices in the local network  
- 🔑 **Port Scanning** – Check open ports for selected devices  
- 📶 **Wi-Fi Scanner** – List nearby Wi-Fi networks (requires location permissions)  
- 🔔 **Foreground Service** – Keeps scanning active with a persistent notification  
- 🛡️ **Permissions Handling** – Dynamically requests necessary permissions for Android 12, 13, 14+  

---

## 📂 Project Structure

app/
└── java/com/muaz/myapplication/
├── MainActivity.kt # Main UI (start/stop scanning, port scanning, WiFi scanning)
├── ForegroundService.kt # Foreground service to keep scanning alive
├── NetworkScanner.kt # Handles network device scanning
├── PortScanner.kt # Handles port scanning logic
└── WifiScannerActivity.kt # Lists nearby Wi-Fi networks

---

## ⚙️ Setup & Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/network-port-scanner.git
Open the project in Android Studio.

Sync Gradle to install dependencies.

Build & run the app on a real device (⚠️ Wi-Fi scanning may not work on emulators).

🛡️ Permissions
This app requires multiple permissions depending on your Android version:

All Versions

ACCESS_FINE_LOCATION → Required for Wi-Fi scanning

Android 13 (Tiramisu, API 33) and above

POST_NOTIFICATIONS → To show Foreground Service notification

NEARBY_WIFI_DEVICES → To scan nearby Wi-Fi devices

Android 14 (UpsideDownCake, API 34) and above

FOREGROUND_SERVICE_LOCATION → To run background network scans

▶️ Usage
Open the app.

Tap "Scan Network" to find devices.

Select a device IP → It will show as “Selected IP”.

Tap "Scan Ports" to check open ports for that IP.

Tap "Stop Scan" to end scanning.

Tap "Wi-Fi Scanner" to list nearby Wi-Fi networks.

🛠️ Tech Stack
Language: Kotlin

Architecture: Activity + ForegroundService

UI: XML layouts + AndroidX + Material Components

Concurrency: Kotlin Coroutines (lifecycleScope)

Notifications: NotificationCompat with channels

⚠️ Disclaimer
This app is built for educational purposes only.
Use it only on networks you own or have permission to scan. Unauthorized scanning of networks or devices may be illegal.

👨‍💻 Author
Developed by MuhammadMuazAhmed ✨
📧 Contact: [muhammadmuazahmed@gmail.com]
