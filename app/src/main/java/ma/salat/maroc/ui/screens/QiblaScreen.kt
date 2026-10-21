package ma.salat.maroc.ui.screens

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.salat.maroc.astro.Qibla
import ma.salat.maroc.sensors.CompassReading
import ma.salat.maroc.sensors.CompassSensor
import ma.salat.maroc.ui.MainViewModel
import ma.salat.maroc.ui.components.BigButton
import ma.salat.maroc.ui.components.PlaceCard
import ma.salat.maroc.ui.components.SuccessGreen
import ma.salat.maroc.ui.components.WarningCard
import ma.salat.maroc.ui.i18n.LocalStrings
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Tolérance (degrés) pour considérer que l'on fait face à la Qibla. */
private const val ALIGN_TOLERANCE = 5.0

@Composable
fun QiblaScreen(vm: MainViewModel, onChangePlace: () -> Unit) {
    val s = LocalStrings.current
    val day by vm.day.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val st = day
    if (st == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val coords = st.place.coords
    val qibla = remember(coords) { Qibla.bearing(coords.lat, coords.lon) }
    val qiblaInt = qibla.roundToInt() % 360
    val distance = remember(coords) { Qibla.distanceKm(coords.lat, coords.lon).roundToInt().toLong() }

    // ----- Capteurs : démarrés seulement quand l'écran est visible -----
    val compass = remember { CompassSensor(context) }
    var reading by remember { mutableStateOf<CompassReading?>(null) }
    var dialAngle by remember { mutableFloatStateOf(0f) }   // rotation continue du cadran (= -cap)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(coords) { compass.setLocation(coords.lat, coords.lon) }
    DisposableEffect(lifecycleOwner, compass) {
        compass.onReading = { r ->
            reading = r
            val target = -r.heading
            dialAngle += Qibla.signedDelta(dialAngle.toDouble(), target.toDouble()).toFloat()
        }
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> compass.start()
                Lifecycle.Event.ON_PAUSE -> compass.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) compass.start()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            compass.stop()
            compass.onReading = null
        }
    }

    val r = reading
    val hasCompass = compass.isAvailable
    val delta = if (r != null) Qibla.signedDelta(r.heading.toDouble(), qibla) else 0.0
    val aligned = r != null && abs(delta) <= ALIGN_TOLERANCE
    var showCalibration by remember { mutableStateOf(false) }

    // Petite vibration quand on devient aligné.
    LaunchedEffect(aligned) {
        if (aligned && st.settings.vibration) vibrateShort(context)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlaceCard(st.place, s, onChangePlace)

        Text(s.qiblaDegrees(qiblaInt), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Text(
            "${s.direction(Qibla.compassPoint(qibla))} · ${s.fromNorth(qiblaInt)}",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        val turnText = when {
            !hasCompass || r == null -> null
            aligned -> s.facingQibla
            delta > 0 -> s.turnRight(abs(delta).roundToInt())
            else -> s.turnLeft(abs(delta).roundToInt())
        }

        QiblaDial(
            dialAngle = if (hasCompass) dialAngle else 0f,
            qibla = qibla.toFloat(),
            aligned = aligned,
            north = s.northLetter, east = s.eastLetter, south = s.southLetter, west = s.westLetter,
            description = turnText ?: s.qiblaDegrees(qiblaInt),
        )

        if (turnText != null) {
            Text(
                turnText,
                style = MaterialTheme.typography.headlineSmall,
                color = if (aligned) SuccessGreen else MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                // On annonce seulement « aligné / pas aligné » à TalkBack, pas chaque degré.
                modifier = Modifier.semantics { if (aligned) liveRegion = LiveRegionMode.Polite },
            )
        }

        if (!hasCompass) {
            WarningCard(title = null, text = s.noCompass)
        } else if (r != null) {
            if (r.needsCalibration || r.interference) {
                WarningCard(
                    title = s.calibrationTitle,
                    text = s.calibrationText + if (r.interference) "\n" + s.interference else "",
                    actionText = s.recalibrate,
                    onAction = { showCalibration = true },
                )
            } else if (r.tilted) {
                WarningCard(title = null, text = s.holdFlat)
            }
        }

        if (hasCompass) {
            BigButton(s.recalibrate, onClick = { showCalibration = true }, icon = Icons.Filled.Refresh, outlined = true)
        }

        Text(s.distanceToMecca(distance), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (showCalibration) {
        AlertDialog(
            onDismissRequest = { showCalibration = false; compass.reset() },
            title = { Text(s.recalibrate, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FigureEightAnimation()
                    Text(s.calibrationText, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalibration = false; compass.reset() }) {
                    Text(s.calibrationDone, style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

@Composable
private fun QiblaDial(
    dialAngle: Float,
    qibla: Float,
    aligned: Boolean,
    north: String, east: String, south: String, west: String,
    description: String,
) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val letterStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
    val northStyle = letterStyle.copy(color = Color(0xFFD32F2F))
    val arrowColor = if (aligned) SuccessGreen else colors.primary
    val gold = Color(0xFFC9A227)

    Canvas(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .aspectRatio(1f)
            .padding(8.dp)
            .semantics { contentDescription = description },
    ) {
        val radius = size.minDimension / 2f
        val c = center

        // Repère fixe en haut : la direction vers laquelle pointe le téléphone.
        val marker = Path().apply {
            moveTo(c.x, c.y - radius - 2f)
            lineTo(c.x - 16.dp.toPx(), c.y - radius + 22.dp.toPx())
            lineTo(c.x + 16.dp.toPx(), c.y - radius + 22.dp.toPx())
            close()
        }

        rotate(dialAngle, c) {
            drawCircle(colors.surfaceVariant, radius, c)
            drawCircle(colors.outline, radius, c, style = Stroke(width = 3.dp.toPx()))
            // Graduations tous les 15°
            for (i in 0 until 24) {
                val a = Math.toRadians(i * 15.0)
                val major = i % 6 == 0
                val inner = radius * (if (major) 0.84f else 0.90f)
                val outer = radius * 0.97f
                drawLine(
                    color = colors.onSurfaceVariant,
                    start = Offset(c.x + inner * sin(a).toFloat(), c.y - inner * cos(a).toFloat()),
                    end = Offset(c.x + outer * sin(a).toFloat(), c.y - outer * cos(a).toFloat()),
                    strokeWidth = (if (major) 4.dp else 2.dp).toPx(),
                    cap = StrokeCap.Round,
                )
            }
            // Flèche Qibla
            rotate(qibla, c) {
                val tipY = c.y - radius * 0.50f
                val shaftW = 14.dp.toPx()
                drawLine(arrowColor, Offset(c.x, c.y + radius * 0.30f), Offset(c.x, tipY + 30.dp.toPx()), strokeWidth = shaftW, cap = StrokeCap.Round)
                val head = Path().apply {
                    moveTo(c.x, tipY)
                    lineTo(c.x - 30.dp.toPx(), tipY + 44.dp.toPx())
                    lineTo(c.x + 30.dp.toPx(), tipY + 44.dp.toPx())
                    close()
                }
                drawPath(head, arrowColor)
                // Petite Kaaba au bout de la flèche
                val k = 22.dp.toPx()
                val kTop = Offset(c.x - k / 2, tipY - k - 4.dp.toPx())
                drawRect(Color(0xFF1B1B1B), topLeft = kTop, size = Size(k, k))
                drawRect(gold, topLeft = Offset(kTop.x, kTop.y + k * 0.25f), size = Size(k, k * 0.14f))
            }
            drawCircle(colors.onSurface, 10.dp.toPx(), c)
        }

        // Lettres cardinales (toujours droites)
        val labels = listOf(0f to north, 90f to east, 180f to south, 270f to west)
        for ((deg, label) in labels) {
            val a = Math.toRadians((deg + dialAngle).toDouble())
            val rr = radius * 0.76f
            val pos = Offset(c.x + rr * sin(a).toFloat(), c.y - rr * cos(a).toFloat())
            val layout = measurer.measure(label, style = if (deg == 0f) northStyle else letterStyle)
            drawText(layout, topLeft = Offset(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f))
        }
        drawPath(marker, if (aligned) SuccessGreen else colors.secondary)
    }
}

/** Animation d'un téléphone qui dessine un « 8 » (lemniscate de Bernoulli). */
@Composable
private fun FigureEightAnimation() {
    val transition = rememberInfiniteTransition(label = "eight")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
    val colors = MaterialTheme.colorScheme
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp),
    ) {
        val a = size.width * 0.38f
        fun point(tt: Double): Offset {
            val d = 1 + sin(tt) * sin(tt)
            return Offset(center.x + (a * cos(tt) / d).toFloat(), center.y + (a * sin(tt) * cos(tt) / d).toFloat())
        }
        val path = Path()
        for (i in 0..100) {
            val p = point(i / 100.0 * 2 * Math.PI)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, colors.outline, style = Stroke(width = 4.dp.toPx()))
        val p = point(t.toDouble())
        val w = 22.dp.toPx()
        val h = 38.dp.toPx()
        drawRoundRect(
            colors.primary,
            topLeft = Offset(p.x - w / 2, p.y - h / 2),
            size = Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
        )
    }
}

private fun vibrateShort(context: Context) {
    @Suppress("DEPRECATION")
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (!vibrator.hasVibrator()) return
    runCatching { vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)) }
}
