package ma.salat.maroc.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ma.salat.maroc.R
import ma.salat.maroc.data.Lang
import ma.salat.maroc.ui.MainViewModel
import ma.salat.maroc.ui.components.BigButton
import ma.salat.maroc.ui.i18n.LocalStrings
import ma.salat.maroc.ui.rememberLocationPermissionRequester

/** Premier lancement : 1) langue  2) GPS ou ville  3) (liste des villes si besoin). */
@Composable
fun OnboardingScreen(vm: MainViewModel) {
    val s = LocalStrings.current
    var step by rememberSaveable { mutableIntStateOf(0) }

    val requestLocation = rememberLocationPermissionRequester { granted ->
        vm.onLocationPermissionResult(granted)
        if (granted) vm.completeOnboarding() else step = 2
    }

    when (step) {
        0, 1 -> Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B6E4F)),
            )
            Text(
                if (step == 0) "Salat Maroc · صلاة المغرب" else s.appName,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            if (step == 0) {
                Text("اختر لغتك · Choisissez votre langue", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                BigButton("العربية", { vm.setLang(Lang.AR); step = 1 })
                BigButton("Français", { vm.setLang(Lang.FR); step = 1 })
            } else {
                Text(s.whereAreYou, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                BigButton(s.useMyLocation, requestLocation, icon = Icons.Filled.LocationOn)
                BigButton(s.chooseCity, { step = 2 }, icon = Icons.AutoMirrored.Filled.List, outlined = true)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (step == 0) "كل شيء يبقى في هاتفك · Tout reste sur votre téléphone" else s.onboardingPrivacy,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        else -> LocationScreen(vm, onDone = { vm.completeOnboarding() })
    }
}
