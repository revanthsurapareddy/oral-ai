package com.oralai.scan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import kotlin.time.Duration.Companion.seconds

val supabase = createSupabaseClient(
    supabaseUrl = "https://gduqgsxwcnrzdjqkextl.supabase.co",
    supabaseKey = "sb_publishable_1V-1Pqu_6ZKe4I3MDadz1w_0fUURFdo"
) {
    requestTimeout = 60.seconds
    install(Auth)
    install(Postgrest)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize persistent repository
        ReportRepository.init(this)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "splash") {
                        composable("splash") {
                            SplashScreen(onNavigateToNext = {
                                navController.navigate("login") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            })
                        }
                        composable("login") {
                            AuthScreen(
                                onAuthSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToSignUp = {
                                    navController.navigate("signup")
                                },
                                onNavigateToForgotPassword = {
                                    navController.navigate("forgot_password")
                                }
                            )
                        }
                        composable("forgot_password") {
                            ForgotPasswordScreen(navController)
                        }
                        composable("signup") {
                            SignUpScreen(onNavigateToLogin = {
                                navController.popBackStack("login", false)
                            })
                        }
                        composable("dashboard") {
                            DashboardScreen(navController)
                        }
                        composable("upload") {
                            UploadScreen(navController)
                        }
                        composable("patients") {
                            PatientsScreen(navController)
                        }
                        composable("settings") {
                            SettingsScreen(navController)
                        }
                        composable("profile") {
                            ProfileScreen(navController)
                        }
                        composable("privacy") {
                            PrivacyPolicyScreen(navController)
                        }
                        composable("change_password") {
                            ChangePasswordScreen(navController)
                        }
                        composable("help") {
                            HelpSupportScreen(navController)
                        }
                        composable("patient_info") {
                            PatientInfoScreen(navController)
                        }
                        composable("analyze") {
                            AnalyzeScreen(navController)
                        }
                        composable("result") {
                            ResultScreen(navController)
                        }
                        composable(
                            "patient_reports/{patientId}",
                            arguments = listOf(androidx.navigation.navArgument("patientId") { type = androidx.navigation.NavType.StringType })
                        ) { backStackEntry ->
                            val patientId = backStackEntry.arguments?.getString("patientId")
                            PatientReportsScreen(navController, patientId)
                        }
                        composable(
                            "view_report/{reportId}",
                            arguments = listOf(androidx.navigation.navArgument("reportId") { type = androidx.navigation.NavType.StringType })
                        ) { backStackEntry ->
                            val reportId = backStackEntry.arguments?.getString("reportId")
                            ViewReportScreen(navController, reportId)
                        }
                    }
                }
            }
        }
    }
}
