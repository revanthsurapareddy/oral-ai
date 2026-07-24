package com.oralai.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(onNavigateToLogin: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B111A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Header Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(Brush.linearGradient(listOf(Color(0xFF2A81F6), Color(0xFF00C6FF))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.FavoriteBorder, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "OralAI", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            // Auth Card
            Column(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF151E2B), RoundedCornerShape(20.dp)).border(1.dp, Color(0xFF1F2C3B), RoundedCornerShape(20.dp)).padding(32.dp)
            ) {
                Text("Doctor Registration", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("Create a new secure portal account", color = Color(0xFF7B8E9F), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp).align(Alignment.CenterHorizontally))

                // FULL NAME
                Text("FULL NAME", color = Color(0xFF6A7C92), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF00C6FF), unfocusedBorderColor = Color.White, containerColor = Color(0xFF0B111A), focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // EMAIL
                Text("DOCTOR ID / EMAIL", color = Color(0xFF6A7C92), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF00C6FF), unfocusedBorderColor = Color.White, containerColor = Color(0xFF0B111A), focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // PASSWORD
                Text("PASSWORD", color = Color(0xFF6A7C92), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF00C6FF), unfocusedBorderColor = Color.White, containerColor = Color(0xFF0B111A), focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                supabase.auth.signUpWith(Email) {
                                    this.email = email
                                    this.password = password
                                }
                                successMessage = "Account created! Redirecting..."
                                delay(1500)
                                onNavigateToLogin() // Automatically trigger navigation
                            } catch (e: Exception) {
                                errorMessage = e.message
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF2A81F6), Color(0xFF00C6FF)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sign Up →", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (errorMessage != null) Text(errorMessage!!, color = Color(0xFFFF4B4B), fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp).align(Alignment.CenterHorizontally))
                if (successMessage != null) Text(successMessage!!, color = Color(0xFF00E676), fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp).align(Alignment.CenterHorizontally))

                Spacer(modifier = Modifier.height(24.dp))

                // Link back to Login
                Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Already have an account? ", color = Color(0xFF7B8E9F))
                    Text(
                        text = "Sign In",
                        color = Color(0xFF00C6FF),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }
            }
        }
    }
}
