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

package com.sechnick.mileage_autotracker

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sechnick.mileage_autotracker.database.MileageDatabase
import com.sechnick.mileage_autotracker.service.TrackingService
import com.sechnick.mileage_autotracker.triptracker.TripTrackerViewModel
import com.sechnick.mileage_autotracker.triptracker.TripTrackerViewModelFactory
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    lateinit var tripTrackerViewModel : TripTrackerViewModel

//    companion object {
//
//        lateinit var globalViewModel : TripTrackerViewModel
//
//        fun setTripTrackerViewModel(item: TripTrackerViewModel){
//            globalViewModel = item
//        }
//        fun getTripTrackerViewModel(): TripTrackerViewModel{
//            return globalViewModel
//        }
//
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        nav_view

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.active_trip_fragment, R.id.trip_tracker_fragment,  R.id.settings))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        Log.d("bind service", "sending intent")
        Intent(this, TrackingService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        // Create an instance of the ViewModel Factory.
        val dataSource = MileageDatabase.getInstance(application).mileageDatabaseDao
        val viewModelFactory = TripTrackerViewModelFactory(dataSource, application) as ViewModelProvider.Factory

        // Get a reference to the ViewModel associated with this fragment.
        tripTrackerViewModel =
                ViewModelProvider(
                        this, viewModelFactory).get(TripTrackerViewModel::class.java)

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        Log.d("permissions", "requesting permissions at Activity")

        if (requestCode == TripTrackerViewModel.REQUEST_PERMISSION_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                TripTrackerViewModel.locationPermissionGranted()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                TripTrackerViewModel.locationPermissionNotGranted()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d("Activity Lifecycle", "destroying activity")
        unbindService(connection)
        TripTrackerViewModel.setBound(false)
    }


    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Log.d("bind service", "connecting service")
            val binder = service as TrackingService.LocalBinder
            TripTrackerViewModel.myService = binder.getService()
            TripTrackerViewModel.setBound(true)
            Log.d("bind service", "myService ="+TripTrackerViewModel.myService.toString())
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            TripTrackerViewModel.setBound(false)
            Log.d("bind service", "disconnecting service")
        }
    }

}
