// file: app/src/main/java/ua/ernest/sshfilex/ui/screens/MainScreenWithDrawer.kt
package ua.ernest.sshfilex.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Logout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithDrawer(
    // колбэк для открытия drawer
    onOpenDrawer: () -> Unit = {},
    // колбэк для логаута
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // текущее «выбранное» значение — для примера
    var selectedFolder by remember { mutableStateOf(".") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .systemBarsPadding()             // учёт статус/навига‐баров
                    .widthIn(min = 240.dp, max = 320.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Верхний пункт меню
                    Column(modifier = Modifier.align(Alignment.TopStart)) {
                        NavigationDrawerItem(
                            label = { androidx.compose.material3.Text("backups") },
                            selected = selectedFolder == "backups",
                            onClick = {
                                selectedFolder = "backups"
                                scope.launch { drawerState.close() }
                            },
                            icon = { androidx.compose.material3.Icon(Icons.Default.Folder, contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    // Нижний пункт меню
                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        Divider()
                        NavigationDrawerItem(
                            label = { androidx.compose.material3.Text("Logout") },
                            selected = false,
                            onClick = {
                                // вызываем колбэк логаута
                                onLogout()
                                scope.launch { drawerState.close() }
                            },
                            icon = { androidx.compose.material3.Icon(Icons.Default.Logout, contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        // Основной контент — ваш RemoteBrowserScreen
        RemoteBrowserScreen(
            modifier = Modifier.fillMaxSize(),
            onOpenDrawer = { scope.launch { drawerState.open() } }
        )
    }
}
