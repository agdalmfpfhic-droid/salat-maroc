package ma.salat.maroc.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ma.salat.maroc.R
import ma.salat.maroc.ui.i18n.LocalStrings
import ma.salat.maroc.ui.screens.LocationScreen
import ma.salat.maroc.ui.screens.PrayersScreen
import ma.salat.maroc.ui.screens.PrivacyScreen
import ma.salat.maroc.ui.screens.QiblaScreen
import ma.salat.maroc.ui.screens.SettingsScreen

private enum class Route { HOME, SETTINGS, PRIVACY, LOCATION }

/** Écran principal unique avec deux onglets : Prières et Qibla. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel) {
    val s = LocalStrings.current
    var route by rememberSaveable { mutableStateOf(Route.HOME) }
    var locationFrom by rememberSaveable { mutableStateOf(Route.HOME) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    fun openLocation(from: Route) { locationFrom = from; route = Route.LOCATION }
    fun goBack() {
        route = when (route) {
            Route.PRIVACY -> Route.SETTINGS
            Route.LOCATION -> locationFrom
            else -> Route.HOME
        }
    }
    BackHandler(enabled = route != Route.HOME) { goBack() }

    val title = when (route) {
        Route.HOME -> s.appName
        Route.SETTINGS -> s.settings
        Route.PRIVACY -> s.privacyTitle
        Route.LOCATION -> s.secLocation
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    if (route != Route.HOME) {
                        IconButton(onClick = { goBack() }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, modifier = Modifier.size(30.dp))
                        }
                    }
                },
                actions = {
                    if (route == Route.HOME) {
                        TextButton(onClick = { route = Route.SETTINGS }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(s.settings, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (route == Route.HOME) {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(painterResource(R.drawable.ic_tab_prayers), contentDescription = null, modifier = Modifier.size(30.dp)) },
                        label = { Text(s.tabPrayers, style = MaterialTheme.typography.labelLarge) },
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(painterResource(R.drawable.ic_tab_qibla), contentDescription = null, modifier = Modifier.size(30.dp)) },
                        label = { Text(s.tabQibla, style = MaterialTheme.typography.labelLarge) },
                    )
                }
            }
        },
    ) { padding ->
        val m = Modifier.padding(padding)
        androidx.compose.foundation.layout.Box(m) {
            when (route) {
                Route.HOME -> if (tab == 0) {
                    PrayersScreen(vm, onChangePlace = { openLocation(Route.HOME) })
                } else {
                    QiblaScreen(vm, onChangePlace = { openLocation(Route.HOME) })
                }
                Route.SETTINGS -> SettingsScreen(
                    vm,
                    onChangePlace = { openLocation(Route.SETTINGS) },
                    onOpenPrivacy = { route = Route.PRIVACY },
                )
                Route.PRIVACY -> PrivacyScreen(vm)
                Route.LOCATION -> LocationScreen(vm, onDone = { goBack() })
            }
        }
    }
}
