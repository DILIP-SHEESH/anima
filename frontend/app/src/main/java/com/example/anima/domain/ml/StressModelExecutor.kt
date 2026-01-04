package com.example.anima.domain.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class PredictionResult(
    val isStress: Boolean,
    val confidence: Float, // 0.0 to 1.0
    val anomalyReason: String? // "High HR", "Low HRV", etc.
)

class StressModelExecutor(private val context: Context) {

    private var tfliteInterpreter: Interpreter? = null
    private val modelName = "stress_model.tflite" // You can generate this later

    init {
        try {
            // Try to load the ML model if it exists in assets
            val modelFile = loadModelFile()
            if (modelFile != null) {
                tfliteInterpreter = Interpreter(modelFile)
                Log.d("AnimaML", "TFLite Model loaded successfully.")
            }
        } catch (e: Exception) {
            Log.w("AnimaML", "ML Model not found. Using Heuristic/Rule-Based Fallback. (Normal for Prototype)")
        }
    }

    /**
     * THE DUAL-MODEL LOGIC:
     * 1. Personalization: Uses 'baselineHR' and 'baselineHRV' from your Database history.
     * 2. Anomaly Detection: Checks current values against those baselines.
     */
    fun predictStress(
        currentHR: Float,
        currentHRV: Float,
        currentTemp: Float,
        baselineHR: Float, // From DB (e.g., avg of last 7 days)
        baselineHRV: Float // From DB
    ): PredictionResult {

        // --- LAYER 1: TFLite Model (The "Smart" Way) ---
        if (tfliteInterpreter != null) {
            try {
                // Prepare inputs: [HR, HRV, Temp, BaselineHR, BaselineHRV]
                val inputs = floatArrayOf(currentHR, currentHRV, currentTemp, baselineHR, baselineHRV)
                val output = Array(1) { FloatArray(1) } // Output probability

                tfliteInterpreter?.run(inputs, output)

                val probability = output[0][0]
                return PredictionResult(
                    isStress = probability > 0.5f,
                    confidence = probability,
                    anomalyReason = if (probability > 0.5f) "ML Detected Pattern" else null
                )
            } catch (e: Exception) {
                Log.e("AnimaML", "Inference failed", e)
            }
        }

        // --- LAYER 2: Heuristic Fallback (Table 3.1 from Report) ---
        // This ensures your demo ALWAYS works, even without a trained model file.

        val isTachycardia = currentHR > 100
        val isBradycardia = currentHR < 60
        val isHrvDrop = (currentHRV > 0) && (currentHRV < (baselineHRV * 0.75)) // 25% drop
        val isTempDrop = currentTemp < 34.0 // Vasoconstriction check

        var stressScore = 0.0f
        var reasons = mutableListOf<String>()

        if (isTachycardia) {
            stressScore += 0.4f
            reasons.add("High Heart Rate")
        }
        if (isHrvDrop) {
            stressScore += 0.5f // HRV is the strongest indicator (Section 2.5.1)
            reasons.add("Low HRV")
        }
        if (isTempDrop) {
            stressScore += 0.2f
            reasons.add("Skin Temp Drop")
        }

        // Personalization Check: Is this abnormal for THIS user?
        if (currentHR > (baselineHR + 15)) {
            stressScore += 0.3f
            reasons.add("Elevated vs Baseline")
        }

        // Cap score at 1.0
        val finalScore = stressScore.coerceAtMost(1.0f)

        return PredictionResult(
            isStress = finalScore >= 0.5f,
            confidence = finalScore,
            anomalyReason = if (reasons.isNotEmpty()) reasons.joinToString(", ") else null
        )
    }

    private fun loadModelFile(): MappedByteBuffer? {
        return try {
            val fileDescriptor = context.assets.openFd(modelName)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
        } catch (e: Exception) {
            null
        }
    }
}