package com.circle.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.circle.app.data.CircleStore
import com.circle.app.data.ThemeMode
import com.circle.app.navigation.CircleNavGraph
import com.circle.app.ui.launch.SessionViewModel
import com.circle.app.ui.theme.CircleTheme

class MainActivity : ComponentActivity() {

    private val session: SessionViewModel by viewModels {
        viewModelFactory { initializer { SessionViewModel(AppGraph.sessionStore) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash stays only until the session is read (a few ms); nothing else shows before Home.
        installSplashScreen().setKeepOnScreenCondition { session.start.value == null }
        super.onCreate(savedInstanceState)
        setContent { CircleApp(session) }
    }
}

@Composable
fun CircleApp(session: SessionViewModel) {
    val night = when (CircleStore.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.NIGHT -> true
        ThemeMode.DAY -> false
    }
    val start by session.start.collectAsStateWithLifecycle()
    CircleTheme(night = night) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CircleTheme.colors.background,
            contentColor = CircleTheme.colors.text
        ) {
            start?.let { CircleNavGraph(session, it) }
        }
    }
}
