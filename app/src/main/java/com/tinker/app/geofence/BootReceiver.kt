package com.tinker.app.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tinker.app.data.RuleStore
import com.tinker.app.service.WatchService

/** Geofences are dropped on reboot; re-register the active rules so arming survives a restart. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val active = RuleStore.get(context).rules.value.filter { it.enabled && it.hasLocation }
        active.forEach { GeofenceManager.register(context, it) }
        if (active.isNotEmpty()) runCatching { WatchService.start(context) }
    }
}
