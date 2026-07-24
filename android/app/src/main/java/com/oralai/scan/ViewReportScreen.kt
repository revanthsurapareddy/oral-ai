package com.oralai.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewReportScreen(navController: NavController, reportId: String?) {
    val report = reportId?.let { ReportRepository.getReportById(it) }

    if (report == null) {
        // Fallback if not found
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

    Scaffold(
        containerColor = Color(0xFF0B111A) // Very Dark Blue background
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
                    text = "Saved Report: ${report.patientName}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Image Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF151E2B))
                    .border(1.dp, Color(0xFF1F2C3B), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (report.analyzedImageBase64 != null) {
                    var bitmap: android.graphics.Bitmap? = null
                    try {
                        val base64String = report.analyzedImageBase64.substringAfter("base64,")
                        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                        bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Analyzed Scan",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("Error loading analyzed image", color = Color.Gray)
                    }
                } else if (report.imageUri != null) {
                    AsyncImage(
                        model = report.imageUri,
                        contentDescription = "Original Scan",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("No Image Found", color = Color.Gray)
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x33FF4B4B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFFF4B4B))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = report.analysisResult,
                            color = Color(0xFFFF4B4B),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color(0xFF1F2C3B))
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Patient Info",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ID: ${report.patientId}\nName: ${report.patientName}\nAge: ${report.patientAge}\nGender: ${report.patientGender}",
                    color = Color(0xFF7B8E9F),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            val context = LocalContext.current
            
            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Delete Button
                Button(
                    onClick = {
                        ReportRepository.removeReport(report.id)
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4B4B))
                ) {
                    Text("Delete Report", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                
                // Save PDF and Share row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            var origBmp: android.graphics.Bitmap? = null
                            if (report.imageUri != null) {
                                try {
                                    val uri = report.imageUri
                                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                                        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                                        origBmp = android.graphics.ImageDecoder.decodeBitmap(source)
                                    } else {
                                        origBmp = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            
                            val hasCancer = report.analysisResult.contains("Cancer Detected")
                            val riskLevel = when {
                                report.analysisResult.contains("High") -> "High"
                                report.analysisResult.contains("Medium") -> "Medium"
                                else -> "Low"
                            }
                            val riskPercentage = if (hasCancer) 85 else 0
                            
                            PdfGenerator.generateAndSavePdf(
                                context = context,
                                patientName = report.patientName.ifEmpty { "Unknown" },
                                patientAge = report.patientAge.ifEmpty { "N/A" },
                                patientGender = report.patientGender.ifEmpty { "N/A" },
                                riskLevel = riskLevel,
                                riskPercentage = riskPercentage,
                                hasCancer = hasCancer,
                                originalBitmap = origBmp,
                                analyzedBitmap = null
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
                            if (report.imageUri != null) {
                                try {
                                    val uri = report.imageUri
                                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                                        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                                        origBmp = android.graphics.ImageDecoder.decodeBitmap(source)
                                    } else {
                                        origBmp = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            
                            val hasCancer = report.analysisResult.contains("Cancer Detected")
                            val riskLevel = when {
                                report.analysisResult.contains("High") -> "High"
                                report.analysisResult.contains("Medium") -> "Medium"
                                else -> "Low"
                            }
                            val riskPercentage = if (hasCancer) 85 else 0
                            
                            PdfGenerator.generateAndSharePdf(
                                context = context,
                                patientName = report.patientName.ifEmpty { "Unknown" },
                                patientAge = report.patientAge.ifEmpty { "N/A" },
                                patientGender = report.patientGender.ifEmpty { "N/A" },
                                riskLevel = riskLevel,
                                riskPercentage = riskPercentage,
                                hasCancer = hasCancer,
                                originalBitmap = origBmp,
                                analyzedBitmap = null
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
