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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(navController: NavController) {
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
                    text = "Privacy and Policy",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Policy Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(Color(0xFF151E2B), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1F2C3B), RoundedCornerShape(12.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Your Privacy is our Priority",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Please read the following carefully to understand how your data is handled.",
                    color = Color(0xFF7B8E9F),
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color(0xFF1F2C3B))
                Spacer(modifier = Modifier.height(24.dp))

                PolicyPoint(
                    title = "1. Data Collection",
                    description = "We only collect necessary patient information and scan images required for accurate AI analysis."
                )
                PolicyPoint(
                    title = "2. Secure Storage",
                    description = "All medical data is securely encrypted and stored following strict compliance guidelines."
                )
                PolicyPoint(
                    title = "3. No Third-Party Sharing",
                    description = "We do not sell, share, or distribute any patient or diagnostic data to third-party marketing companies."
                )
                PolicyPoint(
                    title = "4. Data Deletion",
                    description = "You have the right to request permanent deletion of any saved reports or your entire account at any time."
                )
                PolicyPoint(
                    title = "5. AI Model Training",
                    description = "Uploaded scans may be anonymized and used to improve the diagnostic accuracy of our AI models, unless you opt-out."
                )
                PolicyPoint(
                    title = "6. Transparency",
                    description = "Any changes to our privacy practices will be communicated clearly and require your explicit consent."
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PolicyPoint(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = Color(0xFF7B8E9F),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
