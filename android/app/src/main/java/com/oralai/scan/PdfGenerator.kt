package com.oralai.scan

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PdfGenerator {
    fun generateAndSavePdf(
        context: Context,
        patientName: String,
        patientAge: String,
        patientGender: String,
        riskLevel: String,
        riskPercentage: Int,
        hasCancer: Boolean,
        originalBitmap: Bitmap?,
        analyzedBitmap: Bitmap?
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        paint.color = Color.BLACK
        
        // Draw Header
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("OralAI Clinical Scan Report", 140f, 60f, paint)
        
        // Draw Patient Info
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var currentY = 120f
        canvas.drawText("Patient Name: $patientName", 50f, currentY, paint); currentY += 25f
        canvas.drawText("Age: $patientAge", 50f, currentY, paint); currentY += 25f
        canvas.drawText("Gender: $patientGender", 50f, currentY, paint); currentY += 40f
        
        // Draw Risk Assessment
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val resultText = if (hasCancer) "Cancer Detected - $riskLevel Risk ($riskPercentage%)" else "Normal (No Cancer Detected)"
        if (hasCancer) {
            paint.color = if (riskLevel == "High") Color.RED else 0xFFFFA500.toInt()
        } else {
            paint.color = 0xFF4CAF50.toInt()
        }
        canvas.drawText("Diagnosis: $resultText", 50f, currentY, paint); currentY += 40f
        
        paint.color = Color.BLACK
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        // Draw Images
        val imageWidth = 250f
        val xCenter = (595f - imageWidth) / 2f
        
        if (originalBitmap != null) {
            val softwareBmp = originalBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: originalBitmap
            canvas.drawText("Original Scan:", 50f, currentY, paint); currentY += 20f
            val aspectRatio = softwareBmp.height.toFloat() / softwareBmp.width.toFloat()
            val scaledBitmap = Bitmap.createScaledBitmap(softwareBmp, imageWidth.toInt(), (imageWidth * aspectRatio).toInt(), true)
            canvas.drawBitmap(scaledBitmap, xCenter, currentY, null)
            currentY += scaledBitmap.height + 40f
        }
        
        if (analyzedBitmap != null) {
            val softwareBmp = analyzedBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: analyzedBitmap
            canvas.drawText("Analyzed Scan:", 50f, currentY, paint); currentY += 20f
            val aspectRatio = softwareBmp.height.toFloat() / softwareBmp.width.toFloat()
            val scaledBitmap = Bitmap.createScaledBitmap(softwareBmp, imageWidth.toInt(), (imageWidth * aspectRatio).toInt(), true)
            canvas.drawBitmap(scaledBitmap, xCenter, currentY, null)
        }
        
        document.finishPage(page)
        
        // Save logic
        try {
            val fileName = "OralAI_Report_${patientName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            var outputStream: OutputStream? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = resolver.openOutputStream(uri)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
            }

            outputStream?.use {
                document.writeTo(it)
                Toast.makeText(context, "PDF saved to Downloads folder!", Toast.LENGTH_LONG).show()
            } ?: run {
                Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            document.close()
        }
    }

    fun generateAndSharePdf(
        context: Context,
        patientName: String,
        patientAge: String,
        patientGender: String,
        riskLevel: String,
        riskPercentage: Int,
        hasCancer: Boolean,
        originalBitmap: Bitmap?,
        analyzedBitmap: Bitmap?
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        paint.color = Color.BLACK
        
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("OralAI Clinical Scan Report", 140f, 60f, paint)
        
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var currentY = 120f
        canvas.drawText("Patient Name: $patientName", 50f, currentY, paint); currentY += 25f
        canvas.drawText("Age: $patientAge", 50f, currentY, paint); currentY += 25f
        canvas.drawText("Gender: $patientGender", 50f, currentY, paint); currentY += 40f
        
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val resultText = if (hasCancer) "Cancer Detected - $riskLevel Risk ($riskPercentage%)" else "Normal (No Cancer Detected)"
        if (hasCancer) {
            paint.color = if (riskLevel == "High") Color.RED else 0xFFFFA500.toInt()
        } else {
            paint.color = 0xFF4CAF50.toInt()
        }
        canvas.drawText("Diagnosis: $resultText", 50f, currentY, paint); currentY += 40f
        
        paint.color = Color.BLACK
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        val imageWidth = 250f
        val xCenter = (595f - imageWidth) / 2f
        
        if (originalBitmap != null) {
            val softwareBmp = originalBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: originalBitmap
            canvas.drawText("Original Scan:", 50f, currentY, paint); currentY += 20f
            val aspectRatio = softwareBmp.height.toFloat() / softwareBmp.width.toFloat()
            val scaledBitmap = Bitmap.createScaledBitmap(softwareBmp, imageWidth.toInt(), (imageWidth * aspectRatio).toInt(), true)
            canvas.drawBitmap(scaledBitmap, xCenter, currentY, null)
            currentY += scaledBitmap.height + 40f
        }
        
        if (analyzedBitmap != null) {
            val softwareBmp = analyzedBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: analyzedBitmap
            canvas.drawText("Analyzed Scan:", 50f, currentY, paint); currentY += 20f
            val aspectRatio = softwareBmp.height.toFloat() / softwareBmp.width.toFloat()
            val scaledBitmap = Bitmap.createScaledBitmap(softwareBmp, imageWidth.toInt(), (imageWidth * aspectRatio).toInt(), true)
            canvas.drawBitmap(scaledBitmap, xCenter, currentY, null)
        }
        
        document.finishPage(page)
        
        try {
            val fileName = "OralAI_Shared_Report_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)

            outputStream.use {
                document.writeTo(it)
            }
            
            // Launch Share Intent
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Clinical Report"))
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            document.close()
        }
    }
}
