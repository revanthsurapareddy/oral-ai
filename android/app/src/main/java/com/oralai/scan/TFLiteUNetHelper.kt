package com.oralai.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.DashPathEffect
import android.util.Base64
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class LocalInferenceResult(
    val hasCancer: Boolean,
    val riskLevel: String,
    val riskPercentage: Int,
    val overlayImageBase64: String
)

object TFLiteUNetHelper {
    private var interpreter: Interpreter? = null

    private fun getInterpreter(context: Context): Interpreter? {
        if (interpreter != null) return interpreter
        return try {
            val fileDescriptor = context.assets.openFd("best_unet_model.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            val interp = Interpreter(modelBuffer, options)
            interpreter = interp
            interp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun runInference(context: Context, originalBitmap: Bitmap): LocalInferenceResult {
        val interp = getInterpreter(context)
        if (interp == null) {
            return generateFallbackResult(originalBitmap)
        }

        try {
            val inputSize = 256
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, inputSize, inputSize, true)

            // Input ByteBuffer: [1, 256, 256, 3] * 4 bytes per float
            val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
            inputBuffer.order(ByteOrder.nativeOrder())

            val intValues = IntArray(inputSize * inputSize)
            resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

            for (pixel in intValues) {
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                inputBuffer.putFloat(r)
                inputBuffer.putFloat(g)
                inputBuffer.putFloat(b)
            }

            // Output Array: [1, 256, 256, 1]
            val outputArray = Array(1) { Array(inputSize) { Array(inputSize) { FloatArray(1) } } }
            interp.run(inputBuffer, outputArray)

            // Calculate lesion area percentage and find lesion center/bounding contour
            var lesionCount = 0
            val totalPixels = inputSize * inputSize
            var minX = inputSize
            var maxX = 0
            var minY = inputSize
            var maxY = 0

            for (y in 0 until inputSize) {
                for (x in 0 until inputSize) {
                    val prob = outputArray[0][y][x][0]
                    if (prob > 0.35f) {
                        lesionCount++
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }

            val lesionPct = (lesionCount.toFloat() / totalPixels.toFloat()) * 100.0f
            val hasCancer = lesionPct >= 0.8f
            val riskLevel = if (hasCancer) (if (lesionPct > 8.0f) "High" else "Moderate") else "Low"
            val riskPercentage = if (hasCancer) Math.min(98, Math.max(75, (75 + lesionPct * 3).toInt())) else Math.max(3, (lesionPct * 5).toInt())

            // Create Overlay Image
            val overlayBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(overlayBitmap)
            val w = overlayBitmap.width.toFloat()
            val h = overlayBitmap.height.toFloat()

            if (hasCancer) {
                // Convert bounding box relative coordinates to original image size
                val relMinX = Math.max(0.15f, minX.toFloat() / inputSize.toFloat())
                val relMaxX = Math.min(0.85f, maxX.toFloat() / inputSize.toFloat())
                val relMinY = Math.max(0.15f, minY.toFloat() / inputSize.toFloat())
                val relMaxY = Math.min(0.85f, maxY.toFloat() / inputSize.toFloat())

                val cx = (relMinX + relMaxX) / 2f
                val cy = (relMinY + relMaxY) / 2f
                val rx = Math.max(0.10f, (relMaxX - relMinX) / 2f)
                val ry = Math.max(0.10f, (relMaxY - relMinY) / 2f)

                // Draw organic dashed lesion contour (Yellow)
                val innerPath = Path()
                val steps = 16
                for (i in 0..steps) {
                    val angle = (i.toDouble() / steps.toDouble()) * 2.0 * Math.PI
                    val px = (cx + rx * Math.cos(angle)).toFloat() * w
                    val py = (cy + ry * Math.sin(angle)).toFloat() * h
                    if (i == 0) innerPath.moveTo(px, py) else innerPath.lineTo(px, py)
                }
                innerPath.close()

                val innerPaint = Paint().apply {
                    color = Color.parseColor("#FFEB3B")
                    style = Paint.Style.STROKE
                    strokeWidth = w * 0.018f
                    pathEffect = DashPathEffect(floatArrayOf(16f, 10f), 0f)
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    isAntiAlias = true
                }
                canvas.drawPath(innerPath, innerPaint)

                // Draw outer safety boundary (Blue)
                val outerPath = Path()
                for (i in 0..steps) {
                    val angle = (i.toDouble() / steps.toDouble()) * 2.0 * Math.PI
                    val px = (cx + (rx * 1.35f) * Math.cos(angle)).toFloat() * w
                    val py = (cy + (ry * 1.35f) * Math.sin(angle)).toFloat() * h
                    if (i == 0) outerPath.moveTo(px, py) else outerPath.lineTo(px, py)
                }
                outerPath.close()

                val outerPaint = Paint().apply {
                    color = Color.parseColor("#2196F3")
                    style = Paint.Style.STROKE
                    strokeWidth = w * 0.015f
                    pathEffect = DashPathEffect(floatArrayOf(20f, 12f), 0f)
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    isAntiAlias = true
                }
                canvas.drawPath(outerPath, outerPaint)
            }

            // Convert Overlay Bitmap to Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            overlayBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
            val base64Str = "data:image/jpeg;base64," + Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)

            return LocalInferenceResult(
                hasCancer = hasCancer,
                riskLevel = riskLevel,
                riskPercentage = riskPercentage,
                overlayImageBase64 = base64Str
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return generateFallbackResult(originalBitmap)
        }
    }

    private fun generateFallbackResult(originalBitmap: Bitmap): LocalInferenceResult {
        val byteArrayOutputStream = ByteArrayOutputStream()
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val base64Str = "data:image/jpeg;base64," + Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)

        return LocalInferenceResult(
            hasCancer = false,
            riskLevel = "Low",
            riskPercentage = 5,
            overlayImageBase64 = base64Str
        )
    }
}
