package com.harrypotter.smartphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.harrypotter.smartphone.navigation.AppNavigation
import com.harrypotter.smartphone.ui.theme.HarryPotterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val playerUuid = (application as HarryPotterApp).playerUuid
        setContent {
            HarryPotterTheme {
                AppNavigation(playerUuid = playerUuid)
            }
        }
    }
}
