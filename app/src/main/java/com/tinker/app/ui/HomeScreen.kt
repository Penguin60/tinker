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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
    val rules by store.rules.collectAsState()
    var editing by remember { mutableStateOf<Rule?>(null) }
    var pending by remember { mutableStateOf<Rule?>(null) }
    var status by remember { mutableStateOf("") }

    fun arm(rule: Rule) {
        store.upsert(rule.copy(enabled = true))
        GeofenceManager.register(context, rule)
        WatchService.start(context)
        requestBatteryExemption(context)
        status = ""
    }

    fun disarm(rule: Rule) {
        store.upsert(rule.copy(enabled = false))
        GeofenceManager.unregister(context, rule.id)
        // Restarting refreshes the notification's count; stop once nothing is left to watch.
        if (store.rules.value.any { it.enabled }) WatchService.start(context) else WatchService.stop(context)
        status = ""
    }

    // After granting background location, complete arming.
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { pending?.let { arm(it) } }

    // Foreground permissions (location + SMS + notifications), then chain to background location.
    val armLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val rule = pending
        if (!granted(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            status = "Location permission is required."
        } else if (!granted(context, Manifest.permission.SEND_SMS)) {
            status = "SMS permission is required."
        } else if (Build.VERSION.SDK_INT >= 29 &&
            !granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        ) {
            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else if (rule != null) arm(rule)
    }

    fun onToggle(rule: Rule, want: Boolean) {
        if (!want) return disarm(rule)
        when {
            rule.phone.isBlank() -> status = "Add a phone number to this automation first."
            !rule.hasLocation -> status = "Set a location for this automation first."
            else -> {
                pending = rule
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

    val target = editing
    if (target != null) {
        RuleEditorScreen(target) { editing = null; status = "" }
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
            if (rules.any { it.enabled }) LiveBadge()
        }
        AppText("Text someone the second you arrive somewhere.", color = c.body)

        if (rules.isEmpty()) {
            SectionCard {
                AppText("No automations yet", AppTheme.type.heading)
                Spacer(Modifier.height(AppTheme.space.xs))
                AppText("Add one to pick a spot and the message to send.", color = c.mute)
            }
        } else {
            rules.forEach { rule ->
                RuleCard(rule, onEdit = { editing = rule }, onToggle = { onToggle(rule, it) })
            }
        }

        PrimaryButton("New automation") { editing = Rule() }

        if (rules.any { it.enabled }) {
            AppText(
                "Texts not landing? Tap to set Tinker → Unrestricted in battery settings.",
                AppTheme.type.caption, c.mute,
                Modifier.clickable { openAppSettings(context) },
            )
        }
        if (status.isNotEmpty()) AppText(status, AppTheme.type.caption, c.body)
    }
}

@Composable
private fun RuleCard(rule: Rule, onEdit: () -> Unit, onToggle: (Boolean) -> Unit) {
    val c = AppTheme.colors
    val detail = when {
        rule.phone.isBlank() -> "Needs a phone number"
        !rule.hasLocation -> "Needs a location"
        else -> "→ ${formatPhone(rule.phone)} · ${rule.radius.toInt()} m"
    }
    SectionCard(Modifier.clickable { onEdit() }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                AppText(
                    rule.name.ifBlank { formatPhone(rule.phone).ifBlank { "Untitled automation" } },
                    AppTheme.type.heading,
                )
                Spacer(Modifier.height(AppTheme.space.xs))
                AppText(detail, AppTheme.type.caption, c.mute)
            }
            Spacer(Modifier.width(AppTheme.space.sm))
            Toggle(rule.enabled) { onToggle(it) }
        }
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
