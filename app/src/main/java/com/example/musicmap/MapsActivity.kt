package com.example.musicmap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import kotlin.math.pow
import kotlin.math.sqrt

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val TAG = "MapsActivity"
    private lateinit var targetMarker: Marker
    private val targetLatLng = LatLng(37.7749, -122.4194) // Przykładowa lokalizacja znacznika
    private val SPOTIFY_TRACK_URI = "spotify:track:YOUR_SPOTIFY_TRACK_ID" // Zastąp rzeczywistym URI utworu Spotify

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val btnFocusLocation: Button = findViewById(R.id.focus_bt)
        btnFocusLocation.setOnClickListener {
            focusOnMyLocation()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            getDeviceLocation()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        }

        targetMarker = mMap.addMarker(MarkerOptions().position(targetLatLng).title("Play Song Here"))!!

        mMap.setOnMarkerClickListener { marker ->
            if (marker == targetMarker) {
                playSongOnSpotify()
            }
            true
        }
    }

    private fun getDeviceLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                val locationResult: Task<android.location.Location> = fusedLocationClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude), 15f))
                            Log.d(TAG, "Current location: ${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}")
                            checkProximityToMarker(lastKnownLocation.latitude, lastKnownLocation.longitude)
                        } else {
                            Log.d(TAG, "Current location is null")
                        }
                    } else {
                        Log.d(TAG, "Task unsuccessful, cannot get location")
                        mMap.uiSettings.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
        }
    }

    private fun checkProximityToMarker(userLat: Double, userLng: Double) {
        val distance = calculateDistance(userLat, userLng, targetLatLng.latitude, targetLatLng.longitude)
        Log.d(TAG, "Distance to marker: $distance meters")
        if (distance <= 2) { // Sprawdź, czy użytkownik jest w odległości 2 metrów od znacznika
            playSongOnSpotify()
        }
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0 // W metrach
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2).pow(2.0) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2).pow(2.0)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun playSongOnSpotify() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/track/3LlvRrcCyEGfI88ztZQM5r?si=4b7c0dd409e74f82"))
        intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + this.packageName))
        startActivity(intent)
    }

    private fun focusOnMyLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                val locationResult: Task<android.location.Location> = fusedLocationClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude), 15f))
                            Log.d(TAG, "Focusing on location: ${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}")
                        } else {
                            Log.d(TAG, "Current location is null")
                        }
                    } else {
                        Log.d(TAG, "Task unsuccessful, cannot get location")
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                        mMap.isMyLocationEnabled = true
                        getDeviceLocation()
                    }
                } else {
                    Log.d(TAG, "Permission denied")
                }
                return
            }
        }
    }
}
