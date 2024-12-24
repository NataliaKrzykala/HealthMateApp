package com.example.healthmate.ble

import android.content.Context
import android.location.LocationManager
import android.util.Log

fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return try {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    } catch (e: Exception) {
        Log.e("LocationCheck", "Error checking location state: ${e.message}")
        false
    }
}