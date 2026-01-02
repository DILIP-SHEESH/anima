package com.example.anima.data.bluetooth

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BluetoothService : Service() {
    private lateinit var manager: BluetoothManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        // Pass BluetoothParser as the second parameter
        manager = BluetoothManager(this, BluetoothParser)

        // Start scanning automatically (ensure permissions already granted)
        scope.launch {
            manager.startScan()
        }

        // You can observe manager.sensorFlow here and send broadcasts or post to repository
        scope.launch {
            manager.sensorFlow.collect { sensorData ->
                // Handle sensor data
                // Example: Send broadcast, update repository, show notification, etc.
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        manager.close()
        scope.cancel() // Cancel coroutine scope
        super.onDestroy()
    }
}