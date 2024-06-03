package com.example.musicmap

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
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
import com.google.maps.android.SphericalUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val TAG = "MapsActivity"
    private val markers = mutableListOf<Marker>()
    private var userLocation: LatLng? = null

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

    @SuppressLint("PotentialBehaviorOverride")
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
        getDeviceLocation()

        mMap.setOnMarkerClickListener { marker ->
            userLocation?.let { userLatLng ->
                Log.d(TAG, "User location: ${userLatLng.latitude}, ${userLatLng.longitude}")
                Log.d(TAG, "Marker location: ${marker.position.latitude}, ${marker.position.longitude}")
                val distance = calculateDistance(userLatLng.latitude, userLatLng.longitude, marker.position.latitude, marker.position.longitude)
                Log.d(TAG, "Calculated distance: $distance meters")
                if (distance <= 5) {
                    showModal(marker)
                } else {
                    Log.d(TAG, "Marker is too far away to interact: $distance meters")
                }
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
                            userLocation = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
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

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val userLocation = LatLng(lat1, lng1)
        val markerLocation = LatLng(lat2, lng2)
        return SphericalUtil.computeDistanceBetween(userLocation, markerLocation)
    }

    private fun showModal(marker: Marker) {
        val spotifyUri = marker.tag as? String
        if (spotifyUri != null) {
            val modalFragment = ModalFragment.newInstance(marker.title ?: "Song", spotifyUri)
            modalFragment.show(supportFragmentManager, "ModalFragment")
        }
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
