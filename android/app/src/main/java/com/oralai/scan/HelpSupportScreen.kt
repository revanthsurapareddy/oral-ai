package com.oralai.scan

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(navController: NavController) {
    val context = LocalContext.current

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
                    text = "Help and Support",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Support Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(Color(0xFF151E2B), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1F2C3B), RoundedCornerShape(12.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "How can we assist you?",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                SupportPoint("1. General Navigation", "Use the bottom bar to switch between Home, Upload, Patients, and Settings easily.")
                SupportPoint("2. Uploading a Scan", "Click 'Upload', select an image, fill in patient details, and hit Analyze.")
                SupportPoint("3. AI Confidence Score", "The percentage shows how confident the AI is in detecting anomalies. It is a diagnostic aid, not a definitive conclusion.")
                SupportPoint("4. Managing Patients", "In the Patients tab, you can view past reports. Click any report to view or permanently delete it.")
                SupportPoint("5. Updating Profile", "Navigate to Settings > Profile to view your details. You can change your password via the Settings menu.")
                
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color(0xFF1F2C3B))
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Still need help?",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Contact our support team directly via email.",
                    color = Color(0xFF7B8E9F),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Email Button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@oralai.com") // Only email apps should handle this
                            putExtra(Intent.EXTRA_SUBJECT, "OralAI Support Request")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Icon(imageVector = Icons.Outlined.Email, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Email Support", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SupportPoint(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = Color(0xFF8B5CF6),
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
