package com.example.mapboxtest

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)  // Start with the full map view

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<MapFragment>(R.id.fragment_map_container)
            }
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<NavigationUI>(R.id.fragment_directions_container)
            }
        }

        setupUI(findViewById(android.R.id.content))
        hideSystemUI(window)

        val apiKey = BuildConfig.placesAPIKey
        Log.d("MainActivity", "API Key: $apiKey") // Log the API Key for debugging

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        MapboxNavigationApp.detach(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupUI(view: View) {
        view.setOnTouchListener { v, _ ->
            if (v.id != R.id.searchFrame) {
                clearFocus()
            }
            false
        }
    }

    private fun clearFocus() {
        Log.e("MapboxLog: MainActivity", "Clearing EditBox focus")
        currentFocus?.clearFocus()
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUI(window: Window) {
        val controller = window.insetsController
        controller?.let {
            it.hide(WindowInsets.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

}

class SharedViewModel : ViewModel() {

    private val _selectedLocation = MutableLiveData<LatLng>()
    val selectedLocation: LiveData<LatLng> = _selectedLocation

    private val _selectedLocationName = MutableLiveData<String>()
    val selectedLocationName: LiveData<String> = _selectedLocationName

    private val _selectedAddress = MutableLiveData<String>()
    val selectedAddress: LiveData<String> = _selectedAddress

    private val _currentLocation = MutableLiveData<LatLng>()
    val currentLocation: LiveData<LatLng> = _currentLocation

    private val _clearFocusEvent = MutableLiveData<Unit>()
    val clearFocusEvent: LiveData<Unit> = _clearFocusEvent

    fun selectLocation(latLng: LatLng) {
        _selectedLocation.value = latLng
    }

    fun selectAddress(selectedLocationName: String, selectedAddress: String) {
        _selectedLocationName.value = selectedLocationName
        _selectedAddress.value = selectedAddress
    }

    fun updateCurrentLocation(latLng: LatLng) {
        _currentLocation.value = latLng
    }

    fun clearFocus() {
        _clearFocusEvent.value = Unit
    }

}

