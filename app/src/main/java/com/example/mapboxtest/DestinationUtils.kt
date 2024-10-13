package com.example.mapboxtest

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mapbox.geojson.Point
import java.text.SimpleDateFormat
import java.util.*

data class DestinationEntry(
    val name: String,
    val address: String,
    var point: Point,
    val timestamps: MutableList<String>,
    var count: Int
) {
    fun getScore(): Double {
        val latestTimestamp = timestamps.maxOrNull()?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)?.time
        } ?: 0L
        val now = System.currentTimeMillis()
        val recencyScore = 1.0 / (now - latestTimestamp + 1)
        return count + recencyScore * 1e10 // adjust the weight of recencyScore as needed
    }
}

fun saveFinishedTrips(context: Context, trips: List<DestinationEntry>) {
    val sharedPreferences = context.getSharedPreferences("FinishedTrips", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val json = Gson().toJson(trips)
    editor.putString("trips", json)
    editor.apply()
}

fun getFinishedTrips(context: Context): List<DestinationEntry> {
    val sharedPreferences = context.getSharedPreferences("FinishedTrips", Context.MODE_PRIVATE)
    val json = sharedPreferences.getString("trips", null)
    return if (json != null) {
        val type = object : TypeToken<List<DestinationEntry>>() {}.type
        Gson().fromJson(json, type)
    } else {
        emptyList()
    }
}

fun getTopDestinations(context: Context): List<DestinationEntry> {
    val finishedTrips = getFinishedTrips(context)
    return finishedTrips.sortedByDescending { it.getScore() }.take(3)
}
