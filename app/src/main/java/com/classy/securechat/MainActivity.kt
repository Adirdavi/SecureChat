package com.classy.securechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.classy.securechat.data.UserSharedPreferences
import com.classy.securechat.ui.ChatListScreen
import com.classy.securechat.ui.ChatScreen
import com.classy.securechat.ui.EntryScreen
import com.classy.securechat.ui.LoginScreen
import com.classy.securechat.ui.RegisterScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startDestination = if (UserSharedPreferences.isLoggedIn(this)) "chat_list" else "entry"

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = startDestination) {

                composable("entry") {
                    EntryScreen(
                        onLoginClick = { navController.navigate("login") },
                        onCreateAccountClick = { navController.navigate("register") }
                    )
                }

                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate("chat_list") {
                                popUpTo("entry") { inclusive = true }
                            }
                        },
                        onNavigateToRegister = { navController.navigate("register") }
                    )
                }

                composable("register") {
                    RegisterScreen(
                        onRegisterSuccess = {
                            navController.navigate("chat_list") {
                                popUpTo("entry") { inclusive = true }
                            }
                        },
                        onNavigateToLogin = { navController.navigate("login") }
                    )
                }

                composable("chat_list") {
                    ChatListScreen(
                        onChatClick = { name ->
                            navController.navigate("chat/$name")
                        },
                        onLogout = {

                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()


                            UserSharedPreferences.logout(this@MainActivity)


                            navController.navigate("entry") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    "chat/{userName}",
                    arguments = listOf(navArgument("userName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val userName = backStackEntry.arguments?.getString("userName") ?: "Unknown"
                    ChatScreen(
                        contactName = userName,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}