package com.tinker.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.tinker.app.data.Rule
import com.tinker.app.data.RuleStore
import com.tinker.app.geofence.GeofenceManager
import com.tinker.app.service.WatchService
import com.tinker.app.ui.theme.AppTheme

internal fun granted(context: Context, perm: String) =
    ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

/** Ask to exempt the app from Doze so geofence delivery isn't delayed or dropped. */
private fun requestBatteryExemption(context: Context) {
    val pm = context.getSystemService(PowerManager::class.java)
    if (pm.isIgnoringBatteryOptimizations(context.packageName)) return
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"),
            )
        )
    }
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    )
}

@SuppressLint("MissingPermission")
internal fun fetchLocation(context: Context, onResult: (Double, Double) -> Unit) {
    LocationServices.getFusedLocationProviderClient(context)
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
        .addOnSuccessListener { loc -> if (loc != null) onResult(loc.latitude, loc.longitude) }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val store = remember { RuleStore.get(context) }
    val saved = remember { store.rule.value }

    var phone by remember { mutableStateOf(saved.phone) }
    var message by remember { mutableStateOf(saved.message) }
    var lat by remember { mutableStateOf(saved.lat) }
    var lng by remember { mutableStateOf(saved.lng) }
    var hasLocation by remember { mutableStateOf(saved.hasLocation) }
    var radius by remember { mutableStateOf(saved.radius) }
    var enabled by remember { mutableStateOf(saved.enabled) }
    var status by remember { mutableStateOf("") }
    var showMap by remember { mutableStateOf(false) }

    fun arm() {
        store.save(Rule(phone.trim(), message, lat, lng, radius, enabled = true, hasLocation = true))
        GeofenceManager.register(context, lat, lng, radius)
        WatchService.start(context)
        requestBatteryExemption(context)
        enabled = true
        status = "Armed — will text when you enter this area."
    }

    fun disarm() {
        store.save(Rule(phone.trim(), message, lat, lng, radius, enabled = false, hasLocation = hasLocation))
        GeofenceManager.unregister(context)
        WatchService.stop(context)
        enabled = false
        status = "Disabled."
    }

    // After granting background location, complete arming.
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { arm() }

    // Foreground permissions (location + SMS + notifications), then chain to background location.
    val armLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (!granted(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            status = "Location permission is required."
        } else if (!granted(context, Manifest.permission.SEND_SMS)) {
            status = "SMS permission is required."
        } else if (Build.VERSION.SDK_INT >= 29 &&
            !granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        ) {
            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else arm()
    }

    fun onToggle(want: Boolean) {
        if (!want) return disarm()
        when {
            phone.isBlank() -> status = "Enter a phone number."
            !hasLocation -> status = "Set a location first."
            else -> {
                val perms = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.SEND_SMS,
                )
                if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
                armLauncher.launch(perms.toTypedArray())
            }
        }
    }

    if (showMap) {
        MapPickerScreen(
            initialLat = lat,
            initialLng = lng,
            initialRadius = radius,
            hasInitial = hasLocation,
            onConfirm = { la, lo, r ->
                lat = la; lng = lo; radius = r; hasLocation = true; showMap = false
                status = "Location set."
            },
            onCancel = { showMap = false },
        )
        return
    }

    val c = AppTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.canvas)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.space.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.md),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppText("Tinker", AppTheme.type.title)
            Spacer(Modifier.weight(1f))
            if (enabled) LiveBadge()
        }
        AppText("Text someone the second you arrive somewhere.", color = c.body)

        LabeledField("Phone number", phone, { phone = it }, "+1 555 123 4567", KeyboardType.Phone)
        LabeledField("Message", message, { message = it }, "I'm here!", singleLine = false, minHeight = 72.dp)

        SectionCard {
            Overline("Location")
            Spacer(Modifier.height(AppTheme.space.sm))
            AppText(
                if (hasLocation) String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng) else "No spot picked yet",
                AppTheme.type.heading,
                if (hasLocation) c.ink else c.mute,
            )
            Spacer(Modifier.height(AppTheme.space.md))
            PrimaryButton(if (hasLocation) "Change location" else "Pick on map") { showMap = true }

            Spacer(Modifier.height(AppTheme.space.lg))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Overline("Radius")
                    Spacer(Modifier.height(AppTheme.space.xs))
                    AppText("${radius.toInt()} m", AppTheme.type.heading, c.ink)
                }
                SmallButton("–") { radius = (radius - 50f).coerceAtLeast(50f) }
                Spacer(Modifier.width(AppTheme.space.sm))
                SmallButton("+") { radius = (radius + 50f).coerceAtMost(2000f) }
            }
        }

        SectionCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    AppText("Arm it", AppTheme.type.heading)
                    AppText(if (enabled) "On watch — I've got it from here." else "Off duty", color = c.mute)
                }
                Toggle(enabled) { onToggle(it) }
            }
            if (enabled) {
                Spacer(Modifier.height(AppTheme.space.md))
                AppText(
                    "Texts not landing? Tap to set Tinker → Unrestricted in battery settings.",
                    AppTheme.type.caption, c.mute,
                    Modifier.clickable { openAppSettings(context) },
                )
            }
        }

        if (status.isNotEmpty()) AppText(status, AppTheme.type.caption, c.body)
    }
}

@Composable
private fun LiveBadge() {
    val c = AppTheme.colors
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(c.liveSoft).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(c.live))
        Spacer(Modifier.width(6.dp))
        AppText("LIVE", AppTheme.type.overline, c.live)
    }
}
