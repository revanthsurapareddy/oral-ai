package com.oralai.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var reenterPassword by remember { mutableStateOf("") }
    
    var newPasswordVisible by remember { mutableStateOf(false) }
    var reenterPasswordVisible by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

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
                    text = "Reset Password",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Please enter your email and the new password you wish to use.",
                    color = Color(0xFF7B8E9F),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = Color(0xFFFF4B4B), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
                }

                // Email
                Text("EMAIL", color = Color(0xFF7B8E9F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF00C6FF),
                        unfocusedBorderColor = Color(0xFF1F2C3B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        containerColor = Color(0xFF151E2B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = Color.Gray) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // New Password
                Text("NEW PASSWORD", color = Color(0xFF7B8E9F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF00C6FF),
                        unfocusedBorderColor = Color(0xFF1F2C3B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        containerColor = Color(0xFF151E2B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        val image = if (newPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Re-enter Password
                Text("RE-ENTER PASSWORD", color = Color(0xFF7B8E9F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reenterPassword,
                    onValueChange = { reenterPassword = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF00C6FF),
                        unfocusedBorderColor = Color(0xFF1F2C3B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        containerColor = Color(0xFF151E2B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (reenterPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        val image = if (reenterPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff
                        IconButton(onClick = { reenterPasswordVisible = !reenterPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(48.dp))

                val isFormValid = email.isNotBlank() && newPassword.isNotEmpty() && reenterPassword.isNotEmpty() && newPassword == reenterPassword

                // Save Button
                Button(
                    onClick = {
                        if (newPassword != reenterPassword) {
                            errorMessage = "Passwords do not match."
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            errorMessage = "Password must be at least 6 characters."
                            return@Button
                        }
                        
                        isLoading = true
                        errorMessage = null
                        
                        coroutineScope.launch {
                            // MOCKING PASSWORD RESET
                            // Since doing this directly is not possible without an admin key,
                            // we pretend the server accepted it for this UI iteration.
                            delay(1500) // simulate network request
                            isLoading = false
                            
                            // Navigate back to login
                            navController.popBackStack("login", false)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFormValid) Color(0xFF00C6FF) else Color(0xFF1F2C3B),
                        contentColor = Color.White
                    ),
                    enabled = isFormValid && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
