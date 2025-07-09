// file: app/src/main/java/ua/ernest/sshfilex/ui/MainNav.kt
package ua.ernest.sshfilex.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import ua.ernest.sshfilex.ui.screens.LoginScreen
import ua.ernest.sshfilex.ui.screens.MainScreenWithDrawer

@Composable
fun MainNav() {
    // контроллер навигации
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        // экран логина
        composable("login") {
            LoginScreen {
                // при успешном логине переходим на главный экран и убираем login из back-stack
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
        // главный экран с drawer
        composable("main") {
            MainScreenWithDrawer(
                // здесь реализуем логаут — вернёмся на login и очистим back-stack
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}
