package com.sechnick.mileage_autotracker.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.sechnick.mileage_autotracker.MainActivity
import com.sechnick.mileage_autotracker.R
import com.sechnick.mileage_autotracker.database.MileageDatabaseDao
import com.sechnick.mileage_autotracker.database.RecordedPoint
import com.sechnick.mileage_autotracker.database.RecordedTrip
import com.sechnick.mileage_autotracker.triptracker.TripTrackerViewModel
import kotlinx.coroutines.*

class TrackingService() : Service() {
    private val CHANNEL_ID = "ForegroundService Kotlin"
    private val binder = LocalBinder()

    lateinit var database : MileageDatabaseDao

    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    companion object {
        fun startService(context: Context, message: String) {
            val startIntent = Intent(context, TrackingService::class.java)
            startIntent.putExtra("inputExtra", message)
            ContextCompat.startForegroundService(context, startIntent)
        }
        fun stopService(context: Context) {
            val stopIntent = Intent(context, TrackingService::class.java)
            context.stopService(stopIntent)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //do heavy work on a background thread
        val input = intent?.getStringExtra("inputExtra")
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
                this,
                0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service Kotlin Example")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_directions_car_black_24dp)
                .setContentIntent(pendingIntent)
                .build()
        startForeground(1, notification)
        //stopSelf();

        startLocationUpdates()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }




    lateinit var currentLocation : Location
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private val INTERVAL: Long = 2000
    private val FASTEST_INTERVAL: Long = 1000
    internal var locationRequest: LocationRequest

    //private var activeTrip = MutableLiveData<RecordedTrip?>()
    private var currentPoint = RecordedPoint()
    private lateinit var previousPoint: RecordedPoint

    init {
        locationRequest = LocationRequest()

    }

    protected fun startLocationUpdates() {

        // Create the location request to start receiving updates
        Log.d("location services", "Starting Location Updates")
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.setInterval(INTERVAL)
        locationRequest.setFastestInterval(FASTEST_INTERVAL)

        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(applicationContext)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            return
        }
        fusedLocationProviderClient!!.requestLocationUpdates(locationRequest, locationCallback,
                Looper.myLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // do work here
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
        }
    }

    fun onLocationChanged(location: Location) {
        uiScope.launch {
            val newPoint = RecordedPoint()

            // New location has now been determined
            previousPoint = currentPoint
            currentLocation = location

            newPoint.tripId = TripTrackerViewModel.activeTrip.value!!.tripId
            newPoint.prevPoint = previousPoint.pointId
            newPoint.latitude = currentLocation.latitude
            newPoint.longitude = currentLocation.longitude
            newPoint.horizontalAccuracy = currentLocation.accuracy
            newPoint.bearing = currentLocation.bearing
            newPoint.bearingAccuracy = currentLocation.bearing
            newPoint.elapsedTime = currentLocation.elapsedRealtimeNanos
            newPoint.speed = currentLocation.speed
            newPoint.speedAccuracy = currentLocation.speedAccuracyMetersPerSecond

            recordPoint(newPoint)
            currentPoint = getCurrentPointFromDatabase()
            previousPoint.nextPoint = currentPoint.pointId
            updatePoint(previousPoint)


            val logString = "ID:" + newPoint.pointId.toString() + ", Lat:" + newPoint.latitude.toString() + ", Long:" + newPoint.longitude.toString()
            Log.d("recordedPoint", logString)
        }
        }


    fun stopLocationUpdates() {
        fusedLocationProviderClient!!.removeLocationUpdates(locationCallback)
        Log.d("location services", "Stopping Location Updates")
    }

    private suspend fun getCurrentTripFromDatabase(): RecordedTrip? {
        return withContext(Dispatchers.IO) {
            var thistrip = database.getCurrentTrip()
            if (thistrip?.endTimeMilli != thistrip?.startTimeMilli) {
                thistrip = null
            }
            thistrip
        }
    }

    private suspend fun startTrip(trip: RecordedTrip) {
        withContext(Dispatchers.IO) {
            database.startTrip(trip)
        }
    }

    private suspend fun updateTrip(trip: RecordedTrip) {
        withContext(Dispatchers.IO) {
            database.updateTrip(trip)
        }
    }

    private suspend fun clearTrips() {
        withContext(Dispatchers.IO) {
            database.clearTrips()
        }
    }

    private suspend fun getCurrentPointFromDatabase(): RecordedPoint {
        return withContext(Dispatchers.IO) {
            var thisPoint = database.getCurrentPoint()
            if (thisPoint?.elapsedTime == 0L) {
                thisPoint = null
                //TODO("PROBLEM if emptyDB")
            }
            thisPoint!!
        }
    }

    private suspend fun recordPoint(point: RecordedPoint) {
        withContext(Dispatchers.IO) {
            database.recordPoint(point)
        }
    }

    private suspend fun updatePoint(point: RecordedPoint) {
        withContext(Dispatchers.IO) {
            database.updatePoint(point)
        }
    }

    fun startDBLogging(datasource: MileageDatabaseDao) {
        database = datasource
        Log.d("DB assignment", "DB is assigned in service = " + database.toString())
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): TrackingService = this@TrackingService
    }



}