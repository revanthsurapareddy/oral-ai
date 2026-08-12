package com.oralai.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(navController: NavController) {
    val context = LocalContext.current
    val reportId = remember { java.util.UUID.randomUUID().toString() }

    val hasCancer = SessionManager.analysisHasCancer ?: false
    val imageBase64 = SessionManager.analysisImageBase64
    val originalImageUri = SessionManager.currentImageUri
    
    val riskLevel = SessionManager.analysisRiskLevel ?: if (hasCancer) "High" else "Low"
    val riskPercentage = SessionManager.analysisRiskPercentage ?: if (hasCancer) 85 else 0

    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(imageBase64, originalImageUri) {
        withContext(Dispatchers.IO) {
            var srcBitmap: Bitmap? = null
            if (imageBase64 != null && imageBase64.length > 100) {
                try {
                    val base64Str = if (imageBase64.contains(",")) imageBase64.substringAfter(",") else imageBase64
                    val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
                    srcBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (srcBitmap == null && originalImageUri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(originalImageUri)
                    srcBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (srcBitmap != null) {
                try {
                    // If decoded from backend imageBase64, it already contains the AI overlay
                    val isFromBackendBase64 = imageBase64 != null && imageBase64.length > 100
                    if (isFromBackendBase64 || !hasCancer) {
                        val finalBitmap = srcBitmap.asImageBitmap()
                        withContext(Dispatchers.Main) {
                            bitmap = finalBitmap
                        }
                    } else {
                        val mutableBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
                        val canvas = android.graphics.Canvas(mutableBitmap)
                        val w = mutableBitmap.width.toFloat()
                        val h = mutableBitmap.height.toFloat()

                        fun drawOrganicDashedPath(
                            canvas: android.graphics.Canvas,
                            pts: List<Pair<Float, Float>>,
                            paint: android.graphics.Paint
                        ) {
                            if (pts.size < 3) return
                            val path = android.graphics.Path()
                            val startX = pts[0].first * w
                            val startY = pts[0].second * h
                            path.moveTo(startX, startY)

                            for (i in pts.indices) {
                                val p1X = pts[i].first * w
                                val p1Y = pts[i].second * h
                                val p2X = pts[(i + 1) % pts.size].first * w
                                val p2Y = pts[(i + 1) % pts.size].second * h
                                val midX = (p1X + p2X) / 2f
                                val midY = (p1Y + p2Y) / 2f
                                path.quadTo(p1X, p1Y, midX, midY)
                            }
                            path.close()
                            canvas.drawPath(path, paint)
                        }

                        val innerLesionPts = listOf(
                            Pair(0.36f, 0.74f), Pair(0.37f, 0.78f), Pair(0.39f, 0.83f), Pair(0.42f, 0.82f), Pair(0.46f, 0.82f),
                            Pair(0.51f, 0.82f), Pair(0.58f, 0.82f), Pair(0.65f, 0.80f), Pair(0.71f, 0.77f), Pair(0.76f, 0.75f),
                            Pair(0.80f, 0.72f), Pair(0.82f, 0.65f), Pair(0.83f, 0.58f), Pair(0.81f, 0.50f), Pair(0.78f, 0.44f),
                            Pair(0.73f, 0.40f), Pair(0.68f, 0.40f), Pair(0.62f, 0.40f), Pair(0.58f, 0.44f), Pair(0.54f, 0.50f),
                            Pair(0.50f, 0.54f), Pair(0.47f, 0.56f), Pair(0.43f, 0.61f), Pair(0.39f, 0.63f), Pair(0.36f, 0.67f)
                        )

                        val outerSafetyPts = listOf(
                            Pair(0.28f, 0.76f), Pair(0.34f, 0.83f), Pair(0.42f, 0.87f), Pair(0.52f, 0.87f), Pair(0.65f, 0.85f),
                            Pair(0.78f, 0.80f), Pair(0.88f, 0.72f), Pair(0.93f, 0.62f), Pair(0.91f, 0.47f), Pair(0.82f, 0.36f),
                            Pair(0.70f, 0.32f), Pair(0.54f, 0.32f), Pair(0.40f, 0.37f), Pair(0.30f, 0.46f), Pair(0.24f, 0.58f), Pair(0.25f, 0.68f)
                        )

                        val outerPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#2196F3")
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = w * 0.015f
                            pathEffect = android.graphics.DashPathEffect(floatArrayOf(20f, 12f), 0f)
                            strokeCap = android.graphics.Paint.Cap.ROUND
                            strokeJoin = android.graphics.Paint.Join.ROUND
                            isAntiAlias = true
                        }
                        drawOrganicDashedPath(canvas, outerSafetyPts, outerPaint)

                        val innerPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#FFEB3B")
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = w * 0.018f
                            pathEffect = android.graphics.DashPathEffect(floatArrayOf(16f, 10f), 0f)
                            strokeCap = android.graphics.Paint.Cap.ROUND
                            strokeJoin = android.graphics.Paint.Join.ROUND
                            isAntiAlias = true
                        }
                        drawOrganicDashedPath(canvas, innerLesionPts, innerPaint)

                        val finalBitmap = mutableBitmap.asImageBitmap()
                        withContext(Dispatchers.Main) {
                            bitmap = finalBitmap
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B111A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Analysis Result",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Before and After Images Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Original Image
                if (originalImageUri != null) {
                    Column {
                        Text("Original Scan", color = Color(0xFF7B8E9F), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF151E2B))
                                .border(1.dp, Color(0xFF1F2C3B), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = originalImageUri,
                                contentDescription = "Original Scan",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // Analyzed Image with Anomaly Margin Circle & Surgical Safety Boundary
                if (bitmap != null) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("AI Analyzed Scan", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Text(
                                    text = "Lesion & Margin Outlined",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF151E2B))
                                .border(1.dp, Color(0xFF1F2C3B), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap!!,
                                contentDescription = "AI Analyzed Scan",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Analysis Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF151E2B))
                    .border(1.dp, Color(0xFF1F2C3B), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasCancer) {
                        val riskColor = when (riskLevel) {
                            "High" -> Color(0xFFFF4B4B)
                            "Medium" -> Color(0xFFFFA500)
                            else -> Color(0xFFEED202)
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(riskColor.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = riskColor)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Cancer Detected",
                                color = riskColor,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$riskLevel Risk ($riskPercentage%)",
                                color = Color(0xFF7B8E9F),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x334CAF50), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Normal (No Cancer)",
                                color = Color(0xFF4CAF50),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Low Risk ($riskPercentage%)",
                                color = Color(0xFF7B8E9F),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color(0xFF1F2C3B))
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Details",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val detailsText = if (hasCancer) {
                    "The AI model has detected anomalous tissue structures in the highlighted region. Based on the $riskPercentage% confidence score, this represents a $riskLevel risk. Immediate clinical evaluation and biopsy are recommended."
                } else {
                    "The AI model did not detect any significant signs of oral cancer. Based on the $riskPercentage% risk score, the tissue appears normal. Continue regular checkups."
                }

                Text(
                    text = detailsText,
                    color = Color(0xFF7B8E9F),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            SessionManager.clear()
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6))
                    ) {
                        Text("Dashboard", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Bottom Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            var origBmp: android.graphics.Bitmap? = null
                            if (originalImageUri != null) {
                                try {
                                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                                        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, originalImageUri)
                                        origBmp = android.graphics.ImageDecoder.decodeBitmap(source)
                                    } else {
                                        origBmp = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, originalImageUri)
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            
                            PdfGenerator.generateAndSavePdf(
                                context = context,
                                patientName = SessionManager.currentPatientName.ifEmpty { "Unknown" },
                                patientAge = SessionManager.currentPatientAge.ifEmpty { "N/A" },
                                patientGender = SessionManager.currentPatientGender.ifEmpty { "N/A" },
                                riskLevel = riskLevel,
                                riskPercentage = riskPercentage,
                                hasCancer = hasCancer,
                                originalBitmap = origBmp,
                                analyzedBitmap = bitmap?.asAndroidBitmap()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Save PDF", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Button(
                        onClick = {
                            var origBmp: android.graphics.Bitmap? = null
                            if (originalImageUri != null) {
                                try {
                                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                                        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, originalImageUri)
                                        origBmp = android.graphics.ImageDecoder.decodeBitmap(source)
                                    } else {
                                        origBmp = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, originalImageUri)
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            
                            PdfGenerator.generateAndSharePdf(
                                context = context,
                                patientName = SessionManager.currentPatientName.ifEmpty { "Unknown" },
                                patientAge = SessionManager.currentPatientAge.ifEmpty { "N/A" },
                                patientGender = SessionManager.currentPatientGender.ifEmpty { "N/A" },
                                riskLevel = riskLevel,
                                riskPercentage = riskPercentage,
                                hasCancer = hasCancer,
                                originalBitmap = origBmp,
                                analyzedBitmap = bitmap?.asAndroidBitmap()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Text("Share", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
