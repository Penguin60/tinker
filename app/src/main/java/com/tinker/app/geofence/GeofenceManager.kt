package com.tinker.app.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.tinker.app.data.Rule

object GeofenceManager {

    private fun client(context: Context): GeofencingClient =
        LocationServices.getGeofencingClient(context.applicationContext)

    /** One shared intent for every geofence; the receiver tells them apart by request id. */
    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context.applicationContext, GeofenceReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(context.applicationContext, 0, intent, flags)
    }

    @SuppressLint("MissingPermission")
    fun register(context: Context, rule: Rule) {
        val geofence = Geofence.Builder()
            .setRequestId(rule.id)
            .setCircularRegion(rule.lat, rule.lng, rule.radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0) // don't fire if already inside when armed
            .addGeofence(geofence)
            .build()
        client(context).addGeofences(request, pendingIntent(context))
    }

    fun unregister(context: Context, id: String) {
        client(context).removeGeofences(listOf(id))
    }
}
