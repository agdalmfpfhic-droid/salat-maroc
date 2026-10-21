package ma.salat.maroc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.salat.maroc.data.ThemeMode
import ma.salat.maroc.notif.Notifications
import ma.salat.maroc.ui.AppRoot
import ma.salat.maroc.ui.MainViewModel
import ma.salat.maroc.ui.i18n.LocalStrings
import ma.salat.maroc.ui.i18n.Strings
import ma.salat.maroc.ui.screens.OnboardingScreen
import ma.salat.maroc.ui.theme.SalatTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val current = settings
            val strings = Strings.of(current?.lang ?: ma.salat.maroc.data.Lang.FR)
            val context = LocalContext.current

            LaunchedEffect(strings.lang) { Notifications.createChannels(context, strings) }

            SalatTheme(current?.theme ?: ThemeMode.AUTO) {
                CompositionLocalProvider(
                    LocalStrings provides strings,
                    // Arabe : interface de droite à gauche. Français : gauche à droite.
                    LocalLayoutDirection provides if (strings.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        when {
                            current == null -> Unit // lecture des réglages (quelques millisecondes)
                            !current.onboardingDone -> Box(Modifier.safeDrawingPadding()) { OnboardingScreen(vm) }
                            else -> AppRoot(vm)
                        }
                    }
                }
            }
        }
    }
}
