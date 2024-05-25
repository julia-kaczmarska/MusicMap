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

    //---------------------------------------------------------
    private lateinit var targetMarker: Marker
    private val targetLatLng = LatLng(50.2892914, 19.1234135)
    private val SPOTIFY_TRACK_URI = "https://open.spotify.com/track/453W8V5Ynwn6Tr28KuOwsO?si=e4abbd2970084672"
    //---------------------------------------------------------




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

        targetMarker = mMap.addMarker(MarkerOptions().position(targetLatLng).title("Play Song Here"))!!

        mMap.setOnMarkerClickListener { marker ->
            if (marker == targetMarker) {
                playSongOnSpotify()
            }
            true
        }
    }

    private fun playSongOnSpotify() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SPOTIFY_TRACK_URI))
        intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + this.packageName))
        startActivity(intent)
    }


    private fun loadMarkersFromApi() {
        RetrofitClient.api.getMarkers().enqueue(object : Callback<List<MarkerEntity>> {
            override fun onResponse(call: Call<List<MarkerEntity>>, response: Response<List<MarkerEntity>>) {
                Log.d(TAG, "Response received from API") // Log when response is received
                if (response.isSuccessful) {
                    Log.d(TAG, "API call successful") // Log success status
                    response.body()?.let { markerList ->
                        Log.d(TAG, "Received marker list size: ${markerList.size}") // Log the size of the received marker list
                        for (markerEntity in markerList) {
                            Log.d(TAG, "Adding marker for ID: ${markerEntity.id}, Latitude: ${markerEntity.latitude}, Longitude: ${markerEntity.longitude}") // Log each marker entity before adding
                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(LatLng(markerEntity.latitude, markerEntity.longitude))
                                    .title("Play Song")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) // Optional: Change icon color
                            )
                            marker?.tag = markerEntity.spotifyUri
                            marker?.let {
                                Log.d(TAG, "Added marker with Spotify URI: ${it.tag}") // Log the Spotify URI of the added marker
                                markers.add(it)
                            }
                        }

                        mMap.setOnMarkerClickListener { marker ->
                            val spotifyUri = marker.tag as? String
                            if (spotifyUri!= null) {
                                Log.d(TAG, "Marker clicked, playing song with Spotify URI: $spotifyUri") // Log when a marker is clicked
                                playSongOnSpotify(spotifyUri)
                            }
                            true
                        }
                    }
                } else {
                    Log.e(TAG, "Error: ${response.code()}") // Log the error code if the API call was not successful
                }
            }

            override fun onFailure(call: Call<List<MarkerEntity>>, t: Throwable) {
                Log.e(TAG, "Error loading markers: ${t.message}") // Log the failure message
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
                            checkProximityToMarkers(lastKnownLocation.latitude, lastKnownLocation.longitude)
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