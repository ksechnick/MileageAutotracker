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

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.*
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
 * ViewModel for SleepTrackerFragment.
 */
class TripTrackerViewModel(
        dataSource: MileageDatabaseDao,
        val thisApplication: Application) : ViewModel() {

    /**
     * Hold a reference to SleepDatabase via SleepDatabaseDao.
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

    private var activeTrip = MutableLiveData<RecordedTrip?>()

    val trips = database.getAllTrips()


    /**
     * If tonight has not been set, then the START button should be visible.
     */
    val startButtonVisible = Transformations.map(activeTrip) {
        null == it
    }

    /**
     * If tonight has been set, then the STOP button should be visible.
     */
    val stopButtonVisible = Transformations.map(activeTrip) {
        null != it
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
     * Variable that tells the Fragment to navigate to a specific [SleepQualityFragment]
     *
     * This is private because we don't want to expose setting this value to the Fragment.
     */
    private val _navigateToSleepQuality = MutableLiveData<RecordedTrip>()

    /**
     * If this is non-null, immediately navigate to [SleepQualityFragment] and call [doneNavigating]
     */
    val navigateToSleepQuality: LiveData<RecordedTrip>
        get() = _navigateToSleepQuality

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
     * Call this immediately after navigating to [SleepQualityFragment]
     *
     * It will clear the navigation request, so if the user rotates their phone it won't navigate
     * twice.
     */
    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    /**
     * Navigation for the SleepDetails fragment.
     */
    private val _navigateToSleepDetail = MutableLiveData<Long>()
    val navigateToSleepDetail : LiveData<Long>
        get() = _navigateToSleepDetail

    fun onSleepNightClicked(id: Long) {
        _navigateToSleepDetail.value = id
    }

    fun onSleepDetailNavigated() {
        _navigateToSleepDetail.value = null
    }

    /**
     * variables for location services
     */
    val REQUEST_PERMISSION_LOCATION = 10


    private val _locationSnackbarText = MutableLiveData<String>()
    val locationSnackbarText : LiveData<String>
        get() = _locationSnackbarText

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



    /**
     * Executes when the START button is clicked.
     */
    fun onStart() {
        uiScope.launch {
            // Create a new night, which captures the current time,
            // and insert it into the database.
            val trip = RecordedTrip()

            startTrip(trip)

            activeTrip.value = getCurrentTripFromDatabase()

           // _requestLocationPermission.value = true

            //Log.d("permissions", "permission request = "+ _requestLocationPermission.value.toString())

//            Log.d("permissions", "permission result = "+ _locationPermissionGranted.value.toString())
//            //if (_locationPermissionGranted.value == true) {
//            if (true) {
//                startLocationUpdates()
//                _locationSnackbarText.value = "victory"
//            } else {
//                _locationSnackbarText.value = "never set"
//            }

        }
    }

    /**
     * Executes when the STOP button is clicked.
     */
    fun onStop() {
        uiScope.launch {
            // In Kotlin, the return@label syntax is used for specifying which function among
            // several nested ones this statement returns from.
            // In this case, we are specifying to return from launch().
            val oldTrip = activeTrip.value ?: return@launch

            // Update the night in the database to add the end time.
            oldTrip.endTimeMilli = System.currentTimeMillis()

//            if (_locationPermissionGranted.value == true) {
//                TrackingService. stopLocationUpdates()
//            }

            updateTrip(oldTrip)

            // Set state to navigate to the SleepQualityFragment.
            _navigateToSleepQuality.value = oldTrip
        }
    }

    /**
     * Executes when the CLEAR button is clicked.
     */
    fun onClear() {
        uiScope.launch {
            // Clear the database table.
            clearTrips()

            // And clear tonight since it's no longer in the database
            activeTrip.value = null

            // Show a snackbar message, because it's friendly.
            _showSnackbarEvent.value = true
        }
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




    fun onClickStartServiceButton() {
        Log.d("TrackerFragmentCreate", "inside start listener")
        TrackingService.startService(thisApplication,"I'm tracking now")
    }


    fun onClickStopServiceButton() {
        Log.d("TrackerFragmentCreate", "inside stop listener")
        TrackingService.stopService(thisApplication)
    }


}