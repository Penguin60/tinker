package com.tinker.app.ui

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tinker.app.data.Rule
import com.tinker.app.data.RuleStore
import com.tinker.app.geofence.GeofenceManager
import com.tinker.app.service.WatchService
import com.tinker.app.ui.theme.AppTheme

/**
 * Groups NANP numbers as they're typed — "+1 (555) 123-4567". Numbers on any other country code
 * keep their digits and are left unformatted.
 */
internal fun formatPhone(input: String): String {
    val intl = input.startsWith("+")
    val digits = input.filter(Char::isDigit)
    if (intl && !digits.startsWith("1")) return "+$digits"
    val hasCountryCode = intl || (digits.length > 10 && digits.startsWith("1"))
    val local = if (hasCountryCode) digits.drop(1) else digits
    val head = local.take(10)
    return (if (hasCountryCode) "+1 " else "") + when {
        head.length <= 3 -> head
        head.length <= 6 -> "(${head.take(3)}) ${head.drop(3)}"
        else -> "(${head.take(3)}) ${head.substring(3, 6)}-${head.drop(6)}"
    } + local.drop(10)
}

private fun significant(c: Char) = c.isDigit() || c == '+'

/**
 * Canonical stored form: digits, with a leading "+" whenever a country code is in play. Never drops
 * characters — [formatPhone] renders every one of them, which is what keeps the caret mapping exact.
 */
private fun cleanPhone(input: String): String {
    val digits = input.filter(Char::isDigit)
    val intl = input.startsWith("+") || (digits.length > 10 && digits.startsWith("1"))
    return if (intl) "+$digits" else digits
}

/**
 * Shows the raw digits grouped, leaving the stored text unformatted so the caret never has to be
 * re-derived from a rewritten string — it just tracks the digit it sits after.
 */
private val phoneTransformation = VisualTransformation { text ->
    val raw = text.text
    val formatted = formatPhone(raw)
    val caretAfter = IntArray(raw.length + 1)
    var seen = 0
    for (i in formatted.indices) {
        if (significant(formatted[i])) {
            seen++
            if (seen <= raw.length) caretAfter[seen] = i + 1
        }
    }
    for (i in seen + 1..raw.length) caretAfter[i] = formatted.length
    TransformedText(
        AnnotatedString(formatted),
        object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = caretAfter[offset.coerceIn(0, raw.length)]
            override fun transformedToOriginal(offset: Int) =
                formatted.take(offset.coerceIn(0, formatted.length)).count(::significant).coerceAtMost(raw.length)
        },
    )
}

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

    // ACTION_PICK on the phone URI grants read access to just the chosen row, so no READ_CONTACTS.
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri ->
            context.contentResolver
                .query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)
                ?.use { if (it.moveToFirst()) phone = cleanPhone(it.getString(0)) }
        }
    }

    fun save() {
        val updated = rule.copy(
            name = name.trim(), phone = phone, message = message,
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
        LabeledField(
            "Phone number", phone, { phone = cleanPhone(it) }, "(555) 123-4567", KeyboardType.Phone,
            visualTransformation = phoneTransformation,
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable {
                        contactPicker.launch(
                            Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        )
                    },
                contentAlignment = Alignment.Center,
            ) { ContactIcon(c.mute) }
        }
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
        }

        PrimaryButton("Save") { save() }
    }
}

@Composable
private fun ContactIcon(color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val sw = size.minDimension * 0.10f
        drawCircle(color, h * 0.17f, Offset(w / 2, h * 0.30f), style = Stroke(sw))
        drawArc(
            color, 200f, 140f, false,
            topLeft = Offset(w * 0.18f, h * 0.52f),
            size = Size(w * 0.64f, h * 0.62f),
            style = Stroke(sw, cap = StrokeCap.Round),
        )
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
