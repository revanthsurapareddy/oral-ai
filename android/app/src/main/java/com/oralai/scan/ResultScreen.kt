package com.oralai.scan

import android.net.Uri
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asAndroidBitmap
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(navController: NavController) {
    val reportId = java.util.UUID.randomUUID().toString()

    val hasCancer = SessionManager.analysisHasCancer ?: false
    val imageBase64 = SessionManager.analysisImageBase64
    val originalImageUri = SessionManager.currentImageUri
    
    val riskLevel = SessionManager.analysisRiskLevel ?: if (hasCancer) "High" else "Low"
    val riskPercentage = SessionManager.analysisRiskPercentage ?: if (hasCancer) 85 else 0

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

            val bitmap = remember(imageBase64) {
                if (imageBase64 != null) {
                    try {
                        val base64Str = if (imageBase64.contains(",")) imageBase64.substringAfter(",") else imageBase64
                        val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }

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

                // Analyzed Image
                if (bitmap != null) {
                    Column {
                        Text("Analyzed Scan", color = Color(0xFF7B8E9F), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
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
                                bitmap = bitmap,
                                contentDescription = "Analyzed Scan",
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

            val context = LocalContext.current
            
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
