package com.example.musicmap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.pow
import kotlin.math.sqrt

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val TAG = "MapsActivity"
    private val markers = mutableListOf<Marker>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val btnFocusLocation: Button = findViewById(R.id.btnFocusLocation)
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
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        loadMarkersFromApi()

        mMap.setOnMarkerClickListener { marker ->
            val spotifyUri = marker.tag as? String
            if (spotifyUri != null) {
                val intent = Intent(this@MapsActivity, ModalActivity::class.java)
                intent.putExtra("songTitle", marker.title)
                intent.putExtra("spotifyUri", spotifyUri)
                startActivity(intent)
            }
            true
        }
    }


    private fun colorIntToFloatHue(colorInt: Int): Float {
        val hsv = FloatArray(3)
        Color.colorToHSV(colorInt, hsv)
        return hsv[0]
    }

    private fun loadMarkersFromApi() {
        RetrofitClient.api.getMarkers().enqueue(object : Callback<List<MarkerEntity>> {
            override fun onResponse(call: Call<List<MarkerEntity>>, response: Response<List<MarkerEntity>>) {
                Log.d(TAG, "Response received from API")
                if (response.isSuccessful) {
                    val color = colorIntToFloatHue(ContextCompat.getColor(this@MapsActivity, R.color.purple))
                    Log.d(TAG, "API call successful")
                    response.body()?.let { markerList ->
                        Log.d(TAG, "Received marker list size: ${markerList.size}")
                        for (markerEntity in markerList) {
                            Log.d(TAG, "Adding marker for ID: ${markerEntity.id}, Latitude: ${markerEntity.latitude}, Longitude: ${markerEntity.longitude}")
                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(LatLng(markerEntity.latitude, markerEntity.longitude))
                                    .title(markerEntity.description)
                                    .icon(BitmapDescriptorFactory.defaultMarker(color))
                            )
                            marker?.tag = markerEntity.spotifyUri
                            marker?.let {
                                Log.d(TAG, "Added marker with Spotify URI: ${it.tag}")
                                markers.add(it)
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<MarkerEntity>>, t: Throwable) {
                Log.e(TAG, "Error loading markers: ${t.message}")
            }
        })
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

    private fun checkProximityToMarkers(userLat: Double, userLng: Double) {
        for (marker in markers) {
            val markerPosition = marker.position
            val distance = calculateDistance(userLat, userLng, markerPosition.latitude, markerPosition.longitude)
            Log.d(TAG, "Distance to marker: $distance meters")
            if (distance <= 2) {
                val spotifyUri = marker.tag as? String
                if (spotifyUri != null) {
                    playSongOnSpotify(spotifyUri)
                }
            }
        }
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0 // In meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2).pow(2.0) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2).pow(2.0)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun playSongOnSpotify(spotifyUri: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUri))
        intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://${this.packageName}"))
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
            }
        }
    }
}
