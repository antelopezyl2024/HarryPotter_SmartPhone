package com.harrypotter.smartphone

import android.app.Application
import java.util.UUID

class HarryPotterApp : Application() {

    val playerUuid: String by lazy {
        val prefs = getSharedPreferences("hp_prefs", MODE_PRIVATE)
        prefs.getString("player_uuid", null) ?: UUID.randomUUID().toString().also { uuid ->
            prefs.edit().putString("player_uuid", uuid).apply()
        }
    }
}
