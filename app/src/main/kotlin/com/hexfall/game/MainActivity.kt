package com.hexfall.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hexfall.game.ui.HexfallApp
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw the night sky behind the system bars; screens inset their own
        // content with safeDrawingPadding so nothing hides under the nav bar.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = RunStore(File(filesDir, "hexfall_run.txt"))
        setContent {
            HexfallApp(store)
        }
    }
}
