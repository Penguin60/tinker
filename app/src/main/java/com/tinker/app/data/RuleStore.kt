package com.tinker.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Rule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val phone: String = "",
    val message: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val radius: Float = 150f,
    val enabled: Boolean = false,
    val hasLocation: Boolean = false,
)

/** Persists the rules and exposes them as state. Shared so the receivers and UI read the same store. */
class RuleStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("tinker", Context.MODE_PRIVATE)
    private val _rules = MutableStateFlow(load())
    val rules: StateFlow<List<Rule>> = _rules.asStateFlow()

    fun upsert(rule: Rule) {
        val list = _rules.value.toMutableList()
        val i = list.indexOfFirst { it.id == rule.id }
        if (i >= 0) list[i] = rule else list += rule
        persist(list)
    }

    fun delete(id: String) = persist(_rules.value.filterNot { it.id == id })

    private fun persist(list: List<Rule>) {
        prefs.edit { putString("rules", JSONArray(list.map(::toJson)).toString()) }
        _rules.value = list
    }

    private fun load(): List<Rule> {
        val raw = prefs.getString("rules", null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }

    private fun toJson(r: Rule) = JSONObject().apply {
        put("id", r.id)
        put("name", r.name)
        put("phone", r.phone)
        put("message", r.message)
        put("lat", r.lat)
        put("lng", r.lng)
        put("radius", r.radius.toDouble())
        put("enabled", r.enabled)
        put("hasLocation", r.hasLocation)
    }

    private fun fromJson(o: JSONObject) = Rule(
        id = o.getString("id"),
        name = o.getString("name"),
        phone = o.getString("phone"),
        message = o.getString("message"),
        lat = o.getDouble("lat"),
        lng = o.getDouble("lng"),
        radius = o.getDouble("radius").toFloat(),
        enabled = o.getBoolean("enabled"),
        hasLocation = o.getBoolean("hasLocation"),
    )

    companion object {
        @Volatile private var instance: RuleStore? = null
        fun get(context: Context): RuleStore =
            instance ?: synchronized(this) { instance ?: RuleStore(context).also { instance = it } }
    }
}
