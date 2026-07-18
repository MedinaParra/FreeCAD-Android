package com.medinaparra.freecadandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.medinaparra.freecadandroid.nativebridge.FreeCadNative
import com.medinaparra.freecadandroid.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize our native C++ CAD engine
        val storagePath = filesDir.absolutePath
        FreeCadNative.initialize(storagePath)

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shutdown engine to release JNI links and prevent native mem leaks
        FreeCadNative.shutdown()
    }
}
