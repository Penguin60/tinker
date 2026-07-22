package com.tinker.app.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.tinker.app.data.RuleStore
import com.tinker.app.sms.SmsSender

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val ids = event.triggeringGeofences?.map { it.requestId } ?: return
        RuleStore.get(context).rules.value
            .filter { it.id in ids && it.enabled && it.phone.isNotBlank() }
            .forEach { rule ->
                runCatching { SmsSender.send(context, rule.phone, rule.message) }
                    .onSuccess { SmsSender.notify(context, "Message sent", "To ${rule.phone}") }
                    .onFailure { SmsSender.notify(context, "Send failed", it.message ?: "Unknown error") }
            }
    }
}
