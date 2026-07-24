package com.oralai.scan

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientInfoScreen(navController: NavController) {
    var patientId by remember { mutableStateOf(SessionManager.currentPatientId) }
    var patientName by remember { mutableStateOf(SessionManager.currentPatientName) }
    var patientAge by remember { mutableStateOf(SessionManager.currentPatientAge) }
    var patientGender by remember { mutableStateOf(SessionManager.currentPatientGender) }

    Scaffold(
        containerColor = Color(0xFF0B111A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
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
                    text = "Patient Information",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Patient ID", color = Color(0xFF7B8E9F), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = patientId,
                onValueChange = { patientId = it },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF1F2C3B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    containerColor = Color(0xFF151E2B)
                ),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g. PT-12345", color = Color.Gray) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Full Name", color = Color(0xFF7B8E9F), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF1F2C3B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    containerColor = Color(0xFF151E2B)
                ),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g. John Doe", color = Color.Gray) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Age", color = Color(0xFF7B8E9F), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = patientAge,
                onValueChange = { patientAge = it },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF1F2C3B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    containerColor = Color(0xFF151E2B)
                ),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g. 45", color = Color.Gray) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Gender", color = Color(0xFF7B8E9F), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = patientGender,
                onValueChange = { patientGender = it },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF1F2C3B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    containerColor = Color(0xFF151E2B)
                ),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g. Male / Female", color = Color.Gray) }
            )

            Spacer(modifier = Modifier.height(48.dp))

            val isFormComplete = patientId.isNotBlank() && patientName.isNotBlank() && patientAge.isNotBlank() && patientGender.isNotBlank()

            // Proceed Button
            Button(
                onClick = {
                    if (isFormComplete) {
                        SessionManager.currentPatientId = patientId
                        SessionManager.currentPatientName = patientName
                        SessionManager.currentPatientAge = patientAge
                        SessionManager.currentPatientGender = patientGender
                        navController.navigate("analyze")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormComplete) Color(0xFF3B82F6) else Color.DarkGray
                ),
                enabled = isFormComplete
            ) {
                Text("Proceed", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
