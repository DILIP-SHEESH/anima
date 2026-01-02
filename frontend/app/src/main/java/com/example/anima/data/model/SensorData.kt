package com.example.anima.data.model

data class SensorData(
    val ir: Int? = null,   // 1 detected / 0 not detected
    val pr: Int? = null,   // raw pulse sensor value (analog)
    val hr: Int? = null,   // computed heart rate (BPM)
    val touch: Int? = null, // 1 touched / 0 not touched
    val isAnomaly: Boolean = false
) {
    override fun toString(): String {
        return "SensorData(ir=$ir, pr=$pr, hr=$hr, touch=$touch)"
    }
}
