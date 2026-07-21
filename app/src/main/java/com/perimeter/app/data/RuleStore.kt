package com.perimeter.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Rule(
    val phone: String = "",
    val message: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val radius: Float = 150f,
    val enabled: Boolean = false,
    val hasLocation: Boolean = false,
)

/** Persists the single rule and exposes it as state. Shared so the receiver and UI read the same store. */
class RuleStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("perimeter", Context.MODE_PRIVATE)
    private val _rule = MutableStateFlow(load())
    val rule: StateFlow<Rule> = _rule.asStateFlow()

    fun save(rule: Rule) {
        prefs.edit {
            putString("phone", rule.phone)
            putString("message", rule.message)
            putLong("lat", java.lang.Double.doubleToRawLongBits(rule.lat))
            putLong("lng", java.lang.Double.doubleToRawLongBits(rule.lng))
            putFloat("radius", rule.radius)
            putBoolean("enabled", rule.enabled)
            putBoolean("hasLocation", rule.hasLocation)
        }
        _rule.value = rule
    }

    private fun load() = Rule(
        phone = prefs.getString("phone", "") ?: "",
        message = prefs.getString("message", "") ?: "",
        lat = java.lang.Double.longBitsToDouble(prefs.getLong("lat", 0)),
        lng = java.lang.Double.longBitsToDouble(prefs.getLong("lng", 0)),
        radius = prefs.getFloat("radius", 150f),
        enabled = prefs.getBoolean("enabled", false),
        hasLocation = prefs.getBoolean("hasLocation", false),
    )

    companion object {
        @Volatile private var instance: RuleStore? = null
        fun get(context: Context): RuleStore =
            instance ?: synchronized(this) { instance ?: RuleStore(context).also { instance = it } }
    }
}
