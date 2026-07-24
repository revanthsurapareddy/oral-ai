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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavController) {
    var userName by remember { mutableStateOf("Doctor") }
    var userEmail by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        val session = supabase.auth.currentSessionOrNull()
        if (session != null) {
            val email = session.user?.email ?: ""
            userEmail = email
            val nameFromMeta = session.user?.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
            val extractedName = if (!nameFromMeta.isNullOrEmpty()) nameFromMeta else email.substringBefore('@')
            if (extractedName.isNotEmpty()) {
                userName = extractedName
            }
        } else {
            userName = "Not logged in"
            userEmail = ""
        }
    }

    Scaffold(
        bottomBar = { SettingsBottomNav(navController) },
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
                            .border(1.dp, Color(0xFF1F2C3B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👨‍⚕️", fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF151E2B), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1F2C3B), RoundedCornerShape(12.dp))
                    .padding(24.dp)
            ) {
                Text("Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                // Profile Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFFFE0B2), CircleShape)
                            .border(2.dp, Color(0xFF1F2C3B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👨‍⚕️", fontSize = 32.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(userName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(userEmail, color = Color(0xFF7B8E9F), fontSize = 14.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color(0xFF1F2C3B))
                Spacer(modifier = Modifier.height(24.dp))

                // Settings List
                SettingsItem(
                    icon = Icons.Outlined.Person, 
                    iconTint = Color(0xFF3B82F6), 
                    text = "Profile",
                    onClick = { navController.navigate("profile") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsItem(
                    icon = Icons.Outlined.Security, 
                    iconTint = Color(0xFF10B981), 
                    text = "Privacy and Policy",
                    onClick = { navController.navigate("privacy") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsItem(
                    icon = Icons.Outlined.VpnKey, 
                    iconTint = Color(0xFFF59E0B), 
                    text = "Change Password",
                    onClick = { navController.navigate("change_password") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsItem(
                    icon = Icons.Outlined.HelpOutline, 
                    iconTint = Color(0xFF8B5CF6), 
                    text = "Help and Support",
                    onClick = { navController.navigate("help") }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                // Logout Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x0DEF4444), RoundedCornerShape(12.dp)) // subtle red background
                        .border(1.dp, Color(0x33EF4444), RoundedCornerShape(12.dp))
                        .clickable { 
                            coroutineScope.launch {
                                try {
                                    supabase.auth.signOut()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Transparent, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Logout", color = Color(0xFFEF4444), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, iconTint: Color, text: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A2536), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1F2C3B), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF0B111A), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF7B8E9F))
    }
}

@Composable
fun SettingsBottomNav(navController: NavController) {
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
            selected = false,
            onClick = { navController.navigate("patients") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 11.sp) },
            selected = true,
            onClick = { navController.navigate("settings") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF3B82F6),
                selectedTextColor = Color(0xFF3B82F6),
                unselectedIconColor = Color(0xFF7B8E9F),
                unselectedTextColor = Color(0xFF7B8E9F),
                indicatorColor = Color.Transparent
            )
        )
    }
}
