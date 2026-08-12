package com.oralai.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientReportsScreen(navController: NavController, patientId: String?) {
    val reports = ReportRepository.reports.filter { it.patientId == patientId }
    val patientName = reports.firstOrNull()?.patientName ?: "Unknown Patient"

    Scaffold(
        containerColor = Color(0xFF0B111A) // Very Dark Blue background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "$patientName's Reports", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reports Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes remaining space
                    .background(Color(0xFF151E2B), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1F2C3B), RoundedCornerShape(12.dp))
                    .padding(24.dp)
            ) {
                Text("Saved Reports", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                if (reports.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No reports found for this patient.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(reports) { report ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate("view_report/${report.id}") }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFFE3F2FD), CircleShape), // Light blue for reports
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Description, contentDescription = null, tint = Color(0xFF1976D2))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(report.timestamp))
                                    Text(dateStr, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(report.id.take(8), color = Color(0xFF7B8E9F), fontSize = 13.sp) // Show part of report ID
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (report.analysisResult.contains("Cancer")) Color.Red else Color.Green, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        ReportRepository.removeReport(report.id)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete Report",
                                        tint = Color(0xFFFF4B4B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
