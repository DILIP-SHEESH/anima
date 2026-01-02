package com.example.anima.data.bluetooth

import android.util.Log
import com.example.anima.data.model.SensorData
import java.util.*

object BluetoothParser { // 👈 Defined as an 'object' for easy injection
    private const val TAG = "BluetoothParser"

    // Example input: "I:1|PR:3050|HR:77|T:1"
    fun parseSensorString(s: String): SensorData {
        try {
            val trimmed = s.trim()
            if (trimmed.isEmpty()) return SensorData()
            val parts = trimmed.split("|")
            var ir: Int? = null
            var pr: Int? = null
            var hr: Int? = null
            var t: Int? = null

            for (p in parts) {
                val kv = p.split(":")
                if (kv.size != 2) continue
                val key = kv[0].trim().uppercase(Locale.US)
                val value = kv[1].trim()
                when (key) {
                    "I" -> ir = value.toIntOrNull()
                    "PR" -> pr = value.toIntOrNull()
                    "HR" -> hr = value.toIntOrNull()
                    "T" -> t = value.toIntOrNull()
                }
            }
            return SensorData(ir = ir, pr = pr, hr = hr, touch = t)
        } catch (ex: Exception) {
            Log.e(TAG, "parse error for '$s'", ex)
            return SensorData()
        }
    }
}