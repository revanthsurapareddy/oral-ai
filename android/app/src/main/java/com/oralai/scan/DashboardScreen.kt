package com.oralai.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri
import android.widget.VideoView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import com.oralai.scan.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val totalPatients = ReportRepository.reports.size.toString()
    var userName by remember { mutableStateOf("Doctor") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                // Fetch User Session
                val session = supabase.auth.currentSessionOrNull()
                if (session != null) {
                    val email = session.user?.email ?: ""
                    val nameFromMeta = session.user?.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
                    val extractedName = if (!nameFromMeta.isNullOrEmpty()) nameFromMeta else email.substringBefore('@')
                    if (extractedName.isNotEmpty()) {
                        userName = extractedName
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        bottomBar = { DashboardBottomNav(navController) },
        containerColor = Color(0xFF0B111A) // Very Dark Blue background
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Logo Icon
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

                    // Profile Avatar
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

            Spacer(modifier = Modifier.height(8.dp))

            // Welcome Section
            Text(text = "Welcome back, $userName", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Here's your clinical overview for today.", color = Color(0xFF7B8E9F), fontSize = 14.sp)

            Spacer(modifier = Modifier.height(24.dp))

            // New Scan Button
            Button(
                onClick = { navController.navigate("upload") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New AI Scan", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Person,
                    iconTint = Color(0xFF3B82F6),
                    value = totalPatients,
                    label = "Total Patients",
                    subLabel = "+12 this week"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.FavoriteBorder,
                    iconTint = Color(0xFF10B981),
                    value = "96.5%",
                    label = "Detection Accuracy",
                    subLabel = "+0.2% vs last month"
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    subLabel: String
) {
    Column(
        modifier = modifier
            .background(Color(0xFF151E2B), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1F2C3B), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(1.dp, Color(0xFF2A3B4C), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color(0xFFA0AEC0), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = subLabel, color = Color(0xFF6A7C92), fontSize = 11.sp)
    }
}

@Composable
fun DashboardBottomNav(navController: NavController) {
    NavigationBar(
        containerColor = Color(0xFF151E2B),
        contentColor = Color(0xFF7B8E9F),
        tonalElevation = 0.dp,
        modifier = Modifier.border(width = 1.dp, color = Color(0xFF1F2C3B), shape = RectangleShape) 
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp) },
            selected = true,
            onClick = { navController.navigate("dashboard") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF3B82F6),
                selectedTextColor = Color(0xFF3B82F6),
                unselectedIconColor = Color(0xFF7B8E9F),
                unselectedTextColor = Color(0xFF7B8E9F),
                indicatorColor = Color.Transparent
            )
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
            selected = false,
            onClick = { navController.navigate("patients") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 11.sp) },
            selected = false,
            onClick = { navController.navigate("settings") }
        )
    }
}
