// file: app/src/main/java/ua/ernest/sshfilex/MainActivity.kt
package ua.ernest.sshfilex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ua.ernest.sshfilex.ui.MainNav
import ua.ernest.sshfilex.ui.theme.SSHFileXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SSHFileXTheme {
                MainNav()
            }
        }
    }
}
