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
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProviders
import com.sechnick.mileage_autotracker.database.MileageDatabase
import com.sechnick.mileage_autotracker.triptracker.TripTrackerViewModel
import com.sechnick.mileage_autotracker.triptracker.TripTrackerViewModelFactory


class MainActivity : AppCompatActivity() {

    lateinit var tripTrackerViewModel : TripTrackerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val dataSource = MileageDatabase.getInstance(application).mileageDatabaseDao
        val viewModelFactory = TripTrackerViewModelFactory(dataSource, application)
        tripTrackerViewModel =
                ViewModelProviders.of(
                        this, viewModelFactory).get(TripTrackerViewModel::class.java)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        Log.d("permissions", "requesting permissions at Activity")

        if (requestCode == tripTrackerViewModel.REQUEST_PERMISSION_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tripTrackerViewModel.locationPermissionGranted()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                tripTrackerViewModel.locationPermissionNotGranted()
            }
        }
    }

}
