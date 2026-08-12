package com.oralai.scan

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(navController: NavController) {
    val decodedUri = SessionManager.currentImageUri
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isAnalyzing by remember { mutableStateOf(false) }

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
                    text = "Confirm Scan",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Image Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(350.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF151E2B))
                    .border(1.dp, Color(0xFF1F2C3B), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (decodedUri != null) {
                    AsyncImage(
                        model = decodedUri,
                        contentDescription = "Uploaded Scan",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("No Image Found", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Analyze Button
            Button(
                onClick = {
                    if (decodedUri != null) {
                        isAnalyzing = true
                        coroutineScope.launch {
                            try {
                                val result = analyzeImageBackend(context, decodedUri)
                                if (result != null) {
                                    SessionManager.analysisHasCancer = result.first
                                    SessionManager.analysisImageBase64 = result.second
                                    navController.navigate("result")
                                } else {
                                    isAnalyzing = false
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                isAnalyzing = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                enabled = decodedUri != null && !isAnalyzing
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Analyze Image", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

suspend fun analyzeImageBackend(context: Context, uri: Uri): Pair<Boolean, String>? {
    return withContext(Dispatchers.IO) {
        try {

            // 1. Read input stream bytes ONCE to fix FileProvider stream closure bugs
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext runOnDeviceTFLiteAnalysis(context, uri)

            // 2. Decode bounds to downscale safely in memory
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)

            val maxDimension = 1024
            var sampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / sampleSize >= maxDimension && halfWidth / sampleSize >= maxDimension) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)

            val compressedStream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, compressedStream)
            val bytes = compressedStream.toByteArray() ?: rawBytes
            bitmap?.recycle()

            val base64Fallback = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

            // 3. Network Request to Backend (Localhost Wi-Fi IP, Emulator 10.0.2.2, Live Public Cloud Backends)
            val backendUrls = listOf(
                "http://192.168.137.68:8000/analyze",
                "http://10.0.2.2:8000/analyze",
                "http://127.0.0.1:8000/analyze",
                "https://suraparevi-oral-ai-backend.hf.space/analyze",
                "https://oral-ai-backend-revanthsurapareddy.koyeb.app/analyze",
                "https://oral-ai-backend.onrender.com/analyze"
            )

            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "upload.jpg",
                    bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            for (targetUrl in backendUrls) {
                try {
                    val request = Request.Builder()
                        .url(targetUrl)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val json = JSONObject(responseBody)
                            if (!json.has("status") || json.getString("status") != "error") {
                                val hasCancer = json.optBoolean("has_cancer", json.optBoolean("lesion_detected", true))
                                val imageBase64 = json.optString("overlay_image", json.optString("image_base64", json.optString("scan_image_url", base64Fallback)))
                                val riskLevel = json.optString("risk_level", if (hasCancer) "High" else "Low")
                                val riskPercentage = json.optInt("risk_percentage", json.optInt("lesion_percentage", if (hasCancer) 90 else 5))

                                SessionManager.analysisHasCancer = hasCancer
                                SessionManager.analysisImageBase64 = imageBase64
                                SessionManager.analysisRiskLevel = riskLevel
                                SessionManager.analysisRiskPercentage = riskPercentage

                                val report = SavedReport(
                                    id = java.util.UUID.randomUUID().toString(),
                                    patientId = SessionManager.currentPatientId,
                                    patientName = SessionManager.currentPatientName,
                                    patientAge = SessionManager.currentPatientAge,
                                    patientGender = SessionManager.currentPatientGender,
                                    imageUri = SessionManager.currentImageUri,
                                    analysisResult = if (hasCancer) "Cancer Detected ($riskLevel Risk)" else "Normal",
                                    analyzedImageBase64 = imageBase64
                                )
                                ReportRepository.addReport(report)

                                return@withContext Pair(hasCancer, imageBase64)
                            }
                        }
                    }
                } catch (netEx: Exception) {
                    netEx.printStackTrace()
                }
            }

            // 4. On-Device TFLite AI Model Inference (Offline / PC Off Mode)
            return@withContext runOnDeviceTFLiteAnalysis(context, uri)

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext runOnDeviceTFLiteAnalysis(context, uri)
        }
    }
}

fun runOnDeviceTFLiteAnalysis(context: Context, uri: Uri): Pair<Boolean, String> {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (bitmap != null) {
            val res = TFLiteUNetHelper.runInference(context, bitmap)

            SessionManager.analysisHasCancer = res.hasCancer
            SessionManager.analysisImageBase64 = res.overlayImageBase64
            SessionManager.analysisRiskLevel = res.riskLevel
            SessionManager.analysisRiskPercentage = res.riskPercentage

            val report = SavedReport(
                id = java.util.UUID.randomUUID().toString(),
                patientId = SessionManager.currentPatientId,
                patientName = SessionManager.currentPatientName,
                patientAge = SessionManager.currentPatientAge,
                patientGender = SessionManager.currentPatientGender,
                imageUri = SessionManager.currentImageUri,
                analysisResult = if (res.hasCancer) "Cancer Detected (${res.riskLevel} Risk)" else "Normal Scan",
                analyzedImageBase64 = res.overlayImageBase64
            )
            ReportRepository.addReport(report)

            return Pair(res.hasCancer, res.overlayImageBase64)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    SessionManager.analysisHasCancer = false
    SessionManager.analysisRiskLevel = "Low"
    SessionManager.analysisRiskPercentage = 5
    return Pair(false, "")
}
