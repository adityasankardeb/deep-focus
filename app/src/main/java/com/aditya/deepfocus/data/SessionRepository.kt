package com.aditya.deepfocus.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class SessionRecord(
    val videoId: String,
    val durationSeconds: Int,
    val completedAt: Long,
    val wasCompleted: Boolean
) {
    val formattedDate: String get() {
        return SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault()).format(Date(completedAt))
    }
    val formattedDuration: String get() {
        val h = durationSeconds / 3600
        val m = (durationSeconds % 3600) / 60
        val s = durationSeconds % 60
        return if (h > 0) "${h}h ${m}m" else if (m > 0) "${m}m ${s}s" else "${s}s"
    }
    val dayKey: String get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(completedAt))
}

class SessionRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("deep_focus_sessions", Context.MODE_PRIVATE)

    fun saveSession(record: SessionRecord) {
        val sessions = getSessions().toMutableList()
        sessions.add(0, record)
        val arr = JSONArray()
        sessions.take(50).forEach { s ->
            arr.put(JSONObject().apply {
                put("videoId", s.videoId)
                put("durationSeconds", s.durationSeconds)
                put("completedAt", s.completedAt)
                put("wasCompleted", s.wasCompleted)
            })
        }
        prefs.edit().putString("sessions", arr.toString()).apply()
    }

    fun getSessions(): List<SessionRecord> {
        val arr = JSONArray(prefs.getString("sessions", "[]") ?: "[]")
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            SessionRecord(o.getString("videoId"), o.getInt("durationSeconds"), o.getLong("completedAt"), o.getBoolean("wasCompleted"))
        }
    }

    fun getCurrentStreak(): Int {
        val days = getSessions().filter { it.wasCompleted }.map { it.dayKey }.toSet()
        if (days.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        val yesterday = sdf.format(Date(System.currentTimeMillis() - 86400000L))
        if (!days.contains(today) && !days.contains(yesterday)) return 0
        var streak = 0
        val cal = Calendar.getInstance()
        var checkDay = if (days.contains(today)) today else yesterday
        while (days.contains(checkDay)) {
            streak++
            cal.time = sdf.parse(checkDay)!!
            cal.add(Calendar.DAY_OF_YEAR, -1)
            checkDay = sdf.format(cal.time)
        }
        return streak
    }

    fun getTotalFocusSeconds(): Long = getSessions().filter { it.wasCompleted }.sumOf { it.durationSeconds.toLong() }
}
