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
import androidx.compose.runtime.CompositionLocalProvider
import com.circle.app.data.ThemeMode
import com.circle.app.data.repo.CircleRepository
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.circle.app.navigation.CircleNavGraph
import com.circle.app.ui.launch.SessionViewModel
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.vm.FailureMessages
import com.circle.app.ui.vm.LocalMe
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel

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

/** App-wide state: the theme choice and your own profile (for avatars everywhere). */
class RootViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val theme = repo.theme().state(ThemeMode.SYSTEM)
    val me = repo.me().state(null)
}

@Composable
fun CircleApp(session: SessionViewModel, navController: NavHostController = rememberNavController()) {
    val root = circleViewModel { RootViewModel(it) }
    val theme by root.theme.collectAsStateWithLifecycle()
    val me by root.me.collectAsStateWithLifecycle()
    val night = when (theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.NIGHT -> true
        ThemeMode.DAY -> false
    }
    val start by session.start.collectAsStateWithLifecycle()
    CircleTheme(night = night) { CompositionLocalProvider(LocalMe provides me) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CircleTheme.colors.background,
            contentColor = CircleTheme.colors.text
        ) {
            FailureMessages()
            start?.let { CircleNavGraph(session, it, navController) }
        }
    } }
}
