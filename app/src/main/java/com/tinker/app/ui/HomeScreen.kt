package com.tinker.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.tinker.app.ui.theme.AppTheme

private fun granted(context: Context, perm: String) =
    ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
private fun fetchLocation(context: Context, onResult: (Double, Double) -> Unit) {
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

    fun arm() {
        store.save(Rule(phone.trim(), message, lat, lng, radius, enabled = true, hasLocation = true))
        GeofenceManager.register(context, lat, lng, radius)
        enabled = true
        status = "Armed — will text when you enter this area."
    }

    fun disarm() {
        store.save(Rule(phone.trim(), message, lat, lng, radius, enabled = false, hasLocation = hasLocation))
        GeofenceManager.unregister(context)
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

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (granted(context, Manifest.permission.ACCESS_FINE_LOCATION))
            fetchLocation(context) { la, lo -> lat = la; lng = lo; hasLocation = true; status = "Location set." }
        else status = "Location permission is required."
    }

    fun useCurrentLocation() {
        if (granted(context, Manifest.permission.ACCESS_FINE_LOCATION))
            fetchLocation(context) { la, lo -> lat = la; lng = lo; hasLocation = true; status = "Location set." }
        else locationLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
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

    val c = AppTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.space.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.md),
    ) {
        AppText("Tinker", AppTheme.type.title)
        AppText("Text a contact when you arrive somewhere.", color = c.muted)

        LabeledField("Phone number", phone, { phone = it }, "+1 555 123 4567", KeyboardType.Phone)
        LabeledField("Message", message, { message = it }, "I'm here!", singleLine = false, minHeight = 72.dp)

        SectionCard {
            AppText("Location", AppTheme.type.label, c.muted)
            Spacer(Modifier.height(AppTheme.space.sm))
            AppText(
                if (hasLocation) "%.5f, %.5f".format(lat, lng) else "No location set",
                color = if (hasLocation) c.onSurface else c.muted,
            )
            Spacer(Modifier.height(AppTheme.space.md))
            PrimaryButton("Use my current location", ::useCurrentLocation)

            Spacer(Modifier.height(AppTheme.space.md))
            AppText("Radius: ${radius.toInt()} m", AppTheme.type.label, c.muted)
            Spacer(Modifier.height(AppTheme.space.sm))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.space.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallButton("–") { radius = (radius - 50f).coerceAtLeast(50f) }
                SmallButton("+") { radius = (radius + 50f).coerceAtMost(2000f) }
            }
        }

        SectionCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.fillMaxWidth(0.8f)) {
                    AppText("Enabled")
                    AppText(
                        if (enabled) "Watching for arrival" else "Off",
                        AppTheme.type.label, c.muted,
                    )
                }
                Toggle(enabled) { onToggle(it) }
            }
        }

        if (status.isNotEmpty()) AppText(status, AppTheme.type.label, c.accent)
    }
}
