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
import androidx.lifecycle.LiveData
import com.google.android.gms.location.*
import com.sechnick.mileage_autotracker.*
import com.sechnick.mileage_autotracker.R
import com.sechnick.mileage_autotracker.database.MileageDatabase
import com.sechnick.mileage_autotracker.database.RecordedPoint
import com.sechnick.mileage_autotracker.database.RecordedTrip
import kotlinx.coroutines.*
import kotlin.math.absoluteValue

class TrackingService() : Service() {
    private val CHANNEL_ID = "ForegroundService Kotlin"
    private val binder = LocalBinder()

    val database = MileageDatabase.getInstance(this).mileageDatabaseDao

    private var serviceJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var currentTripID = 0L

    val _tripDistance = SafeMutableLiveData(0.0)
    fun setTripDistance(distance: Double){
        _tripDistance.value = distance
        Log.d("service livedata test","currentTrip distance: " + tripDistance.value + " for trip " + activeTrip.tripId)
    }
    val tripDistance : SafeLiveData<Double>
        get() = _tripDistance

    var activeTrip = RecordedTrip()

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
        uiScope.launch {
            val oldTrip = activeTrip
                if (oldTrip.endTimeMilli == oldTrip.startTimeMilli){
                // Update the night in the database to add the end time.
                    oldTrip.endTimeMilli = System.currentTimeMillis()

                    updateTrip(oldTrip)
                }
        }
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


    private val _point = SafeMutableLiveData(RecordedPoint())
    val point : LiveData<RecordedPoint>
     get() = _point

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
        locationRequest.interval = INTERVAL
        locationRequest.fastestInterval = FASTEST_INTERVAL

        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(applicationContext)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            return
        }
        fusedLocationProviderClient!!.requestLocationUpdates(locationRequest, locationCallback,
                Looper.myLooper())

//        Log.d("location services", "availability is successful: " + fusedLocationProviderClient!!.locationAvailability.isSuccessful.toString())
//        Log.d("location services", "availability is complete: " + fusedLocationProviderClient!!.locationAvailability.isComplete.toString())
//        Log.d("location services", "availability is canceled: " + fusedLocationProviderClient!!.locationAvailability.isCanceled.toString())
//        Log.d("location services", "last location is successful: " + fusedLocationProviderClient!!.lastLocation.isSuccessful)
//        Log.d("location services", "last location is complete: " + fusedLocationProviderClient!!.lastLocation.isComplete)
//        Log.d("location services", "last location is canceled: " + fusedLocationProviderClient!!.lastLocation.isCanceled)
//        Log.d("location services", "last location is?: " + fusedLocationProviderClient!!.lastLocation.toString())
//        Log.d("location services", "Location Provider is enabled: " + LocationManager.)
//        Log.d("location services", "looking to get last current location")
//
//        val startLocation = fusedLocationProviderClient!!.lastLocation.result!!
//        currentPoint.latitude = startLocation.latitude
//        currentPoint.longitude = startLocation.longitude
//        currentPoint.horizontalAccuracy = startLocation.accuracy
//        currentPoint.bearing = startLocation.bearing
//        currentPoint.bearingAccuracy = startLocation.bearing
//        currentPoint.elapsedTime = startLocation.elapsedRealtimeNanos
//        currentPoint.speed = startLocation.speed
//        currentPoint.speedAccuracy = startLocation.speedAccuracyMetersPerSecond

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
            currentLocation = location

                // if trip ID isn't the same (start of new trip) then set it to be
                if (activeTrip.tripId != currentTripID){
                    currentPoint = RecordedPoint()
                    currentTripID = activeTrip.tripId

                    currentPoint.latitude = currentLocation.latitude
                    currentPoint.longitude = currentLocation.longitude
                    currentPoint.horizontalAccuracy = currentLocation.accuracy
                    currentPoint.bearing = currentLocation.bearing
                    currentPoint.bearingAccuracy = currentLocation.bearing
                    currentPoint.elapsedTime = currentLocation.elapsedRealtimeNanos
                    currentPoint.speed = currentLocation.speed
                    currentPoint.speedAccuracy = currentLocation.speedAccuracyMetersPerSecond
                }
//                            //initialize currentPoint to current location if it doesn't exist
//                if (currentPoint.elapsedTime == 0L) {
//                    currentPoint.latitude = currentLocation.latitude
//                    currentPoint.longitude = currentLocation.longitude
//                    currentPoint.horizontalAccuracy = currentLocation.accuracy
//                    currentPoint.bearing = currentLocation.bearing
//                    currentPoint.bearingAccuracy = currentLocation.bearing
//                    currentPoint.elapsedTime = currentLocation.elapsedRealtimeNanos
//                    currentPoint.speed = currentLocation.speed
//                    currentPoint.speedAccuracy = currentLocation.speedAccuracyMetersPerSecond
//                }
                previousPoint = currentPoint

                newPoint.tripId = activeTrip.tripId
                newPoint.prevPoint = previousPoint.pointId
                newPoint.latitude = currentLocation.latitude
                newPoint.longitude = currentLocation.longitude
                newPoint.horizontalAccuracy = currentLocation.accuracy
                newPoint.bearing = currentLocation.bearing
                newPoint.bearingAccuracy = currentLocation.bearing
                newPoint.elapsedTime = currentLocation.elapsedRealtimeNanos
                newPoint.speed = currentLocation.speed
                newPoint.speedAccuracy = currentLocation.speedAccuracyMetersPerSecond

                var logString = "ID:" + previousPoint.pointId.toString() + ", Lat:" + previousPoint.latitude.toString() + ", Long:" + previousPoint.longitude.toString()
                Log.d("previousPoint", logString)

                logString = "ID:" + newPoint.pointId.toString() + ", Lat:" + newPoint.latitude.toString() + ", Long:" + newPoint.longitude.toString()
                Log.d("recordedPoint", logString)
                val distance = distanceBetween(newPoint.latitude, newPoint.longitude, previousPoint.latitude, previousPoint.longitude)
                Log.d("distance", distance.toString())

                setTripDistance(tripDistance.value +distance)
                // currentTrip.calculatedDistance += distance


                Log.d("service livedata test","activeTrip distance: " + activeTrip.calculatedDistance)

                newPoint.distanceFromLast = distance

                activeTrip.endTimeMilli = System.currentTimeMillis()

                recordPoint(newPoint)
                currentPoint = getCurrentPointFromDatabase()
                _point.value = currentPoint
            if (previousPoint.pointId != 0L) {
                previousPoint.nextPoint = currentPoint.pointId
                updatePoint(previousPoint)
            }
            activeTrip.calculatedDistance = tripDistance.value
            updateTrip(activeTrip)
            //activeTrip.value = currentTrip
        }
        }


    fun stopLocationUpdates() {
        fusedLocationProviderClient?.let {
            fusedLocationProviderClient!!.removeLocationUpdates(locationCallback)
            Log.d("location services", "Stopping Location Updates")
        }

        if (fusedLocationProviderClient == null){
            Log.d("location services", "location updates already stopped")
        }


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

//    fun startDBLogging(datasource: MileageDatabaseDao) {
//        database = datasource
//        Log.d("DB assignment", "DB is assigned in service = " + database.toString())
//    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): TrackingService = this@TrackingService
    }



}