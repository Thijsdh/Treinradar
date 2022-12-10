package nl.thijsdh.treinradar

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.*
import com.google.android.gms.location.*
import com.google.gson.Gson
import kotlinx.coroutines.NonCancellable.start
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.config.Configuration.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var appRequestQueue: RequestQueue

    private val vehicles: MutableList<Vehicle> = mutableListOf()

    public override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Create a FusedLocationProviderClient.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Create a LocationCallback.
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Do work with the new location. In this case, we just log it.
                Log.d(MainActivity::class.java.simpleName, locationResult.lastLocation.toString())

                val lastLocation = locationResult.lastLocation ?: return
                // Center the map on the new location.
                map.controller.animateTo(
                    GeoPoint(
                        lastLocation.latitude,
                        lastLocation.longitude
                    )
                )

                // Request new vehicles.
                fetchTrainLocations()
            }
        }

        appRequestQueue = Volley.newRequestQueue(this)

        requestLocationPermission()

        setContentView(R.layout.activity_main)

        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        map = findViewById(R.id.map)
        map.controller.setZoom(15.0)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        map.setTileSource(TileSourceFactory.MAPNIK)

        fetchTrainLocations()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is already granted. Start the process of requesting location updates.
            return startLocationUpdates()
        }

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(ACCESS_FINE_LOCATION, false) -> {
                    // Precise location access granted.
                    startLocationUpdates()
                }
                permissions.getOrDefault(ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                    requestLocationPermission()
                }
                else -> {
                    // No location access granted.
                }
            }
        }

        locationPermissionRequest.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return requestLocationPermission()
        }
        fusedLocationClient.requestLocationUpdates(
            createLocationRequest(),
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun fetchTrainLocations() {
        val url = "http://10.0.2.2:3000/vehicles"
        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                run {
                    val responseObject = Gson().fromJson(response.toString(), VehiclesResponse::class.java)
                    Log.d(MainActivity::class.java.simpleName, responseObject.toString())
                    vehicles.addAll(responseObject.vehicles)
                    drawTrainLocations()
                }
            },
            { error -> Log.e("Volley", error.toString()) })
        appRequestQueue.add(request)
    }

    private fun drawTrainLocations() {
        map.overlays.clear()
        vehicles.forEach { vehicle ->
            val marker = Marker(map)
            marker.position = GeoPoint(vehicle.lat.toDouble(), vehicle.lng.toDouble())
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = vehicle.ritId
            map.overlays.add(marker)
        }
        map.invalidate()
    }
}
