package com.circle.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.circle.app.navigation.CircleNavGraph
import com.circle.app.ui.theme.CircleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CircleApp()
        }
    }
}

@Composable
fun CircleApp() {
    CircleTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CircleNavGraph()
        }
    }
}
