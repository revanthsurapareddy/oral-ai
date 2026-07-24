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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsScreen(navController: NavController) {
    val reports = ReportRepository.reports
    val patients = reports.distinctBy { it.patientId }

    Scaffold(
        bottomBar = { PatientsBottomNav(navController) },
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF2A81F6), Color(0xFF00C6FF))),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.FavoriteBorder, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "OralAI", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Notification icon removed
                    
                    
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFFE0B2), CircleShape)
                            .border(1.dp, Color(0xFF1F2C3B), CircleShape)
                            .clickable { navController.navigate("settings") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👨‍⚕️", fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Patients Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes remaining space
                    .background(Color(0xFF151E2B), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1F2C3B), RoundedCornerShape(12.dp))
                    .padding(24.dp)
            ) {
                Text("Saved Patients", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Search MRN or name...", color = Color(0xFF7B8E9F)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color(0xFF7B8E9F)) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = Color(0xFF151E2B),
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White,
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.dp, Color.White, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.FilterAlt, contentDescription = "Filter", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                if (patients.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No patients saved yet.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(patients) { patient ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate("patient_reports/${patient.patientId}") }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFFFFCDD2), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🧑🏼‍🦲", fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(patient.patientName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(patient.patientId, color = Color(0xFF7B8E9F), fontSize = 13.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (patient.analysisResult.contains("Cancer")) Color.Red else Color.Green, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PatientsBottomNav(navController: NavController) {
    NavigationBar(
        containerColor = Color(0xFF151E2B),
        contentColor = Color(0xFF7B8E9F),
        tonalElevation = 0.dp,
        modifier = Modifier.border(width = 1.dp, color = Color(0xFF1F2C3B), shape = RectangleShape) 
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp) },
            selected = false,
            onClick = { navController.navigate("dashboard") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.ArrowUpward, contentDescription = "Upload") }, 
            label = { Text("Upload", fontSize = 11.sp) },
            selected = false,
            onClick = { navController.navigate("upload") }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Person, contentDescription = "Patients") },
            label = { Text("Patients", fontSize = 11.sp) },
            selected = true,
            onClick = { navController.navigate("patients") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF3B82F6),
                selectedTextColor = Color(0xFF3B82F6),
                unselectedIconColor = Color(0xFF7B8E9F),
                unselectedTextColor = Color(0xFF7B8E9F),
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 11.sp) },
            selected = false,
            onClick = { navController.navigate("settings") }
        )
    }
}
