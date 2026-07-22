package com.tinker.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tinker.app.data.Rule
import com.tinker.app.data.RuleStore
import com.tinker.app.geofence.GeofenceManager
import com.tinker.app.service.WatchService
import com.tinker.app.ui.theme.AppTheme

@Composable
fun RuleEditorScreen(rule: Rule, onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember { RuleStore.get(context) }
    val existing = remember { store.rules.value.any { it.id == rule.id } }

    var name by remember { mutableStateOf(rule.name) }
    var phone by remember { mutableStateOf(rule.phone) }
    var message by remember { mutableStateOf(rule.message) }
    var lat by remember { mutableStateOf(rule.lat) }
    var lng by remember { mutableStateOf(rule.lng) }
    var radius by remember { mutableStateOf(rule.radius) }
    var hasLocation by remember { mutableStateOf(rule.hasLocation) }
    var showMap by remember { mutableStateOf(false) }

    fun save() {
        val updated = rule.copy(
            name = name.trim(), phone = phone.trim(), message = message,
            lat = lat, lng = lng, radius = radius, hasLocation = hasLocation,
        )
        store.upsert(updated)
        if (updated.enabled) GeofenceManager.register(context, updated)
        onClose()
    }

    fun delete() {
        GeofenceManager.unregister(context, rule.id)
        store.delete(rule.id)
        if (store.rules.value.any { it.enabled }) WatchService.start(context) else WatchService.stop(context)
        onClose()
    }

    if (showMap) {
        MapPickerScreen(
            initialLat = lat,
            initialLng = lng,
            initialRadius = radius,
            hasInitial = hasLocation,
            onConfirm = { la, lo, r -> lat = la; lng = lo; radius = r; hasLocation = true; showMap = false },
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
            SmallButton("←") { onClose() }
            Spacer(Modifier.width(AppTheme.space.md))
            AppText(if (existing) "Edit automation" else "New automation", AppTheme.type.heading)
            Spacer(Modifier.weight(1f))
            if (existing) SmallButton(::delete) { TrashIcon(c.danger) }
        }

        LabeledField("Name", name, { name = it }, "Home")
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

        PrimaryButton("Save") { save() }
    }
}

@Composable
private fun TrashIcon(color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val sw = size.minDimension * 0.11f
        drawLine(color, Offset(w * 0.12f, h * 0.26f), Offset(w * 0.88f, h * 0.26f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.38f, h * 0.12f), Offset(w * 0.62f, h * 0.12f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.38f, h * 0.12f), Offset(w * 0.38f, h * 0.26f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.62f, h * 0.12f), Offset(w * 0.62f, h * 0.26f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.22f, h * 0.26f), Offset(w * 0.30f, h * 0.88f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.78f, h * 0.26f), Offset(w * 0.70f, h * 0.88f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.30f, h * 0.88f), Offset(w * 0.70f, h * 0.88f), sw, StrokeCap.Round)
    }
}
