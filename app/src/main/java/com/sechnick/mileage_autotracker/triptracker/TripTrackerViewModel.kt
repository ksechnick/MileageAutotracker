/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sechnick.mileage_autotracker.triptracker

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.sechnick.mileage_autotracker.SafeMutableLiveData
import com.sechnick.mileage_autotracker.database.MileageDatabaseDao
import com.sechnick.mileage_autotracker.database.RecordedPoint
import com.sechnick.mileage_autotracker.database.RecordedTrip
import com.sechnick.mileage_autotracker.service.TrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for TripTrackerFragment.
 */
class TripTrackerViewModel(
        dataSource: MileageDatabaseDao,
        val thisApplication: Application) : ViewModel() {

    companion object {

        var myService = TrackingService()

        private val _bound = MutableLiveData<Boolean>()
        val bound: LiveData<Boolean>
            get() = _bound
        fun setBound(value: Boolean){
            _bound.value = value
            Log.d("bind service", "bound =" +_bound.value)
        }

        private val _requestLocationPermission = MutableLiveData<Boolean>()
        val requestLocationPermission : LiveData<Boolean>
            get() = _requestLocationPermission

        private val _locationPermissionGranted = MutableLiveData<Boolean>()
        val locationPermissionGranted : LiveData<Boolean>
            get() = _locationPermissionGranted

        private val _permissionAck = MutableLiveData<Boolean>()
        val permissionAck : LiveData<Boolean>
            get() = _permissionAck

        fun locationPermissionGranted() {
            _locationPermissionGranted.value = true
            _requestLocationPermission.value = false
        }

        fun locationPermissionNotGranted() {
            _locationPermissionGranted.value = false
            _requestLocationPermission.value = false
        }

        val REQUEST_PERMISSION_LOCATION = 10


    }

    var activeTrip = SafeMutableLiveData(RecordedTrip())

    /**
     * Hold a reference to MileageDatabase via MileageDatabaseDao.
     */
    val database = dataSource

    /** Coroutine variables */

    /**
     * viewModelJob allows us to cancel all coroutines started by this ViewModel.
     */
    private var viewModelJob = Job()

    /**
     * A [CoroutineScope] keeps track of all coroutines started by this ViewModel.
     *
     * Because we pass it [viewModelJob], any coroutine started in this uiScope can be cancelled
     * by calling `viewModelJob.cancel()`
     *
     * By default, all coroutines started in uiScope will launch in [Dispatchers.Main] which is
     * the main thread on Android. This is a sensible default because most coroutines started by
     * a [ViewModel] update the UI after performing some processing.
     */
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)



    val trips = database.getAllTrips()


    /**
     * If tonight has not been set, then the START button should be visible.
     */
    val startButtonVisible = true //activeTrip.value.tripId == 0L


    /**
     * If tonight has been set, then the STOP button should be visible.
     */
    val stopButtonVisible = true //activeTrip.value.tripId != 0L

    private val _tracking = MutableLiveData<Boolean>()
    val tracking : LiveData<Boolean>
        get() = _tracking


    private val _businessClick = MutableLiveData<Boolean>()
    val businessClick : LiveData<Boolean>
        get() = _businessClick
    fun onBusinessClick(){
        _businessClick.value = true
    }
    fun businessClicked(){
        _businessClick.value = false
    }
    init {
        _businessClick.value = false
    }

    /**
     * If there are any nights in the database, show the CLEAR button.
     */
    val clearButtonVisible = Transformations.map(trips) {
        it?.isNotEmpty()
    }

    /**
     * Request a toast by setting this value to true.
     *
     * This is private because we don't want to expose setting this value to the Fragment.
     */
    private var _showSnackbarEvent = MutableLiveData<Boolean?>()

    /**
     * If this is true, immediately `show()` a toast and call `doneShowingSnackbar()`.
     */
    val showSnackBarEvent: LiveData<Boolean?>
        get() = _showSnackbarEvent

    /**
     * Variable that tells the Fragment to navigate to a specific [TripQualityFragment]
     *
     * This is private because we don't want to expose setting this value to the Fragment.
     */
    private val _navigateToActiveTrip = MutableLiveData<RecordedTrip>()

    /**
     * If this is non-null, immediately navigate to [TripQualityFragment] and call [doneNavigating]
     */
    val navigateToActiveTrip: LiveData<RecordedTrip>
        get() = _navigateToActiveTrip

    /**
     * Call this immediately after calling `show()` on a toast.
     *
     * It will clear the toast request, so if the user rotates their phone it won't show a duplicate
     * toast.
     */
    fun doneShowingSnackbar() {
        _showSnackbarEvent.value = null
    }

    /**
     * Call this immediately after navigating to [TripQualityFragment]
     *
     * It will clear the navigation request, so if the user rotates their phone it won't navigate
     * twice.
     */
    fun doneNavigating() {
        _navigateToActiveTrip.value = null
    }

    /**
     * Navigation for the TripDetails fragment.
     */
    private val _navigateToTripDetail = MutableLiveData<Long>()
    val navigateToTripDetail : LiveData<Long>
        get() = _navigateToTripDetail

    fun onTripDetailClicked(id: Long) {
        _navigateToTripDetail.value = id
    }

    fun onTripDetailNavigated() {
        _navigateToTripDetail.value = null
    }

    /**
     * variables for location services
     */



    private val _locationSnackbarText = MutableLiveData<String>()
    val locationSnackbarText : LiveData<String>
        get() = _locationSnackbarText

//    private val _requestLocationPermission = MutableLiveData<Boolean>()
//    val requestLocationPermission : LiveData<Boolean>
//        get() = _requestLocationPermission
//
//    private val _locationPermissionGranted = MutableLiveData<Boolean>()
//    val locationPermissionGranted : LiveData<Boolean>
//        get() = _locationPermissionGranted
//
//    private val _permissionAck = MutableLiveData<Boolean>()
//    val permissionAck : LiveData<Boolean>
//        get() = _permissionAck



//    fun locationPermissionGranted() {
//        _locationPermissionGranted.value = true
//        _requestLocationPermission.value = false
//    }
//
//    fun locationPermissionNotGranted() {
//        _locationPermissionGranted.value = false
//        _requestLocationPermission.value = false
//    }



    private var currentPoint = RecordedPoint()
    private lateinit var previousPoint: RecordedPoint

    init {
        initializeTonight()

//        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            buildAlertMessageNoGps()
//        }
        _locationPermissionGranted.value = false
        _requestLocationPermission.value = false
        _locationSnackbarText.value = "nothing yet"
        _bound.value = false
        Log.d("bind service", "initialized bound =" +_bound.value)
        Log.d("bind service", "initial myService ="+ myService.toString())

    }

    private fun initializeTonight() {
        uiScope.launch {
            activeTrip.value = getCurrentTripFromDatabase()
        }
    }

    /**
     *  Handling the case of the stopped app or forgotten recording,
     *  the start and end times will be the same.j
     *
     *  If the start time and end time are not the same, then we do not have an unfinished
     *  recording.
     */
    private suspend fun getCurrentTripFromDatabase(): RecordedTrip {
        return withContext(Dispatchers.IO) {
            var thistrip = database.getCurrentTrip()
            if (thistrip == null) {
                thistrip = RecordedTrip()
            }
            thistrip
        }!!
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



    /**
     * Executes when the START button is clicked.
     */
    fun onStart() {
        uiScope.launch {
            // Create a new night, which captures the current time,
            // and insert it into the database.
            val trip = RecordedTrip()

            startTrip(trip)

            _tracking.value = true

            activeTrip.value = getCurrentTripFromDatabase()

            if (activeTrip.value.tripId != 0L) {

                // _requestLocationPermission.value = true

                Log.d("permissions", "permission request = " + _requestLocationPermission.value.toString())

                Log.d("permissions", "permission result = " + _locationPermissionGranted.value.toString())
                //if (_locationPermissionGranted.value == true) {
                if (true) {
                    // myService.startLocationUpdates()
                    _locationSnackbarText.value = "victory"
                } else {
                    _locationSnackbarText.value = "never set"
                }

                Log.d("TrackerFragmentCreate", "inside start listener")
                Log.d("bind service", "myService =$myService")
                TrackingService.startService(thisApplication, "I'm tracking now")
                myService.activeTrip = activeTrip.value
                myService.setTripDistance(activeTrip.value.calculatedDistance)

                //myService.activeTrip.vehicleId = 7
                //activeTrip.value.startMileage = 5
//                Log.d("start service compare check", "service trip instance: "+ myService.activeTrip.vehicleId)
//                Log.d("start service compare check", "service trip value instance: "+ myService.activeTrip.startMileage)
//                Log.d("start service compare check", "viewmodel trip instance: " + activeTrip.toString())
//                Log.d("start service compare check", "viewmodel trip value instance: "+ activeTrip.value.toString())
                _tracking.value = true
                //myService.startDBLogging(database)
                _navigateToActiveTrip.value = activeTrip.value
            } else {
                //TODO what if trip not created?
            }

        }
    }

    /**
     * Executes when the STOP button is clicked.
     */
    fun onStopTrackerScreen() {
        uiScope.launch {
            // In Kotlin, the return@label syntax is used for specifying which function among
            // several nested ones this statement returns from.
            // In this case, we are specifying to return from launch().

            activeTrip.value = myService.activeTrip
            val oldTrip = activeTrip.value

            // Update the night in the database to add the end time.
            oldTrip.endTimeMilli = System.currentTimeMillis()

            updateTrip(oldTrip)

            if (_locationPermissionGranted.value == true) {
                myService.stopLocationUpdates()
            }

            Log.d("TrackerFragmentCreate", "inside stop listener")
            TrackingService.stopService(thisApplication)
            _tracking.value = false
            // Set state to navigate to the ActiveTripFragment.
            _navigateToActiveTrip.value = oldTrip
        }
    }

    private val _navigateToTripTracker = MutableLiveData<Boolean?>()
    val navigateToTripTracker: LiveData<Boolean?>
        get() = _navigateToTripTracker
    fun onTrackerNavigated() {
        _navigateToTripTracker.value = null
    }


    fun onStopActiveScreen() {
        uiScope.launch {
            // In Kotlin, the return@label syntax is used for specifying which function among
            // several nested ones this statement returns from.
            // In this case, we are specifying to return from launch().
            val oldTrip = activeTrip.value

            // Update the night in the database to add the end time.
            oldTrip.endTimeMilli = System.currentTimeMillis()

            updateTrip(oldTrip)

            if (_locationPermissionGranted.value == true) {
                myService.stopLocationUpdates()
            }

            Log.d("ActiveFragmentCreate", "inside stop listener")
            TrackingService.stopService(thisApplication)
            _tracking.value = false
            // Set state to navigate to the ActiveTripFragment.
            _navigateToTripTracker.value = true
        }
    }

    fun onBackActiveScreen() {
            // Set state to navigate to the ActiveTripFragment.
            _navigateToTripTracker.value = true
    }


    /**
     * Executes when the CLEAR button is clicked.
     */
    fun onClear() {
//        uiScope.launch {
//            // Clear the database table.
//            clearTrips()
//
//            // And clear tonight since it's no longer in the database
//            activeTrip.value = RecordedTrip()
//
//            // Show a snackbar message, because it's friendly.
//            _showSnackbarEvent.value = true
//        }
    }

    /**
     * Called when the ViewModel is dismantled.
     * At this point, we want to cancel all coroutines;
     * otherwise we end up with processes that have nowhere to return to
     * using memory and resources.
     */
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    /**
     * Location services code
     */




//    fun onClickStartServiceButton() {
//        Log.d("TrackerFragmentCreate", "inside start listener")
//        Log.d("bind service", "myService =$myService")
//        TrackingService.startService(thisApplication,"I'm tracking now")
//        myService.startDBLogging(database)
//    }
//
//
//    fun onClickStopServiceButton() {
//        Log.d("TrackerFragmentCreate", "inside stop listener")
//        TrackingService.stopService(thisApplication)
//    }
//
// fun setBound() {
//     _bound.value = true
//     Log.d("bind service", "manually set bound =" +_bound.value)
// }




}