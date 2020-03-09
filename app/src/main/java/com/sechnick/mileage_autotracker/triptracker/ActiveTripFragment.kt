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

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.sechnick.mileage_autotracker.R
import com.sechnick.mileage_autotracker.convertDurationToFormatted
import com.sechnick.mileage_autotracker.database.MileageDatabase
import com.sechnick.mileage_autotracker.databinding.FragmentActiveTripBinding
import kotlinx.android.synthetic.main.fragment_active_trip.*
import java.text.DecimalFormat

/**
 * Fragment that displays the active trip
 * allows for ending the trip, changing whether the trip is business or personal,
 * and displaying the elapsed trip time and distance
 */
class ActiveTripFragment : Fragment() {

    val tripTrackerViewModel : TripTrackerViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentActiveTripBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_active_trip, container, false)

        val application = requireNotNull(this.activity).application
        val arguments = arguments?.let { ActiveTripFragmentArgs.fromBundle(it) }

        // Create an instance of the ViewModel Factory.
        val dataSource = MileageDatabase.getInstance(application).mileageDatabaseDao

//        tripTrackerViewModel =
//                ViewModelProvider(
//                        activity as ViewModelStoreOwner, viewModelFactory).get(TripTrackerViewModel::class.java)

        // To use the View Model with data binding, you have to explicitly
        // give the binding object a reference to it.
        binding.tripTrackerViewModel = tripTrackerViewModel
        binding.trip = tripTrackerViewModel.activeTrip.value
      //  binding.distance = TripTrackerViewModel.myService.tripDistance.value

        binding.lifecycleOwner = viewLifecycleOwner

        // Add an Observer to the state variable for Navigating when a Quality icon is tapped.
        tripTrackerViewModel.navigateToTripTracker.observe(viewLifecycleOwner, Observer {
            if (it == true) { // Observed state is true.
                this.findNavController().navigate(
                        ActiveTripFragmentDirections.actionActiveTripFragmentToTripTrackerFragment())
                // Reset state to make sure we only navigate once, even if the device
                // has a configuration change.
                tripTrackerViewModel.onTrackerNavigated()
            }
        })



         return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        TripTrackerViewModel.myService.point.observe(viewLifecycleOwner, Observer {
            imageArrow.rotation = it.bearing
            Log.d("service point live data", "bearing = "+it.bearing)
            //tripTrackerViewModel.activeTrip.value = TripTrackerViewModel.myService.activeTrip.value
//            Log.d("service point live data", "viewmodel active trip updated distance = " + tripTrackerViewModel.activeTrip.value.calculatedDistance)
//            Log.d("service point live data", "service active trip updated distance = " + TripTrackerViewModel.myService.activeTrip.calculatedDistance)
            })

//        imageArrow.setOnClickListener{
//            imageArrow.rotation = TripTrackerViewModel.myService.point.bearing
//        }

        TripTrackerViewModel.myService.tripDistance.observe(viewLifecycleOwner, Observer {
  //          val df = DecimalFormat("####.##")
                textActiveDistance.text = DecimalFormat("####.##").format(it) + " meters"
                textActiveElapsedTime.text = convertDurationToFormatted(tripTrackerViewModel.activeTrip.value.startTimeMilli, System.currentTimeMillis())
        })

        TripTrackerViewModel.myService.thisDistance.observe(viewLifecycleOwner, Observer {
            //          val df = DecimalFormat("####.##")
            Log.d("thisDistance", "passed distance= " + it)
            Log.d("thisDistance", "passed distance= " + TripTrackerViewModel.myService.thisDistance.value)
            textThisDistance.text = DecimalFormat("####.##").format(it) + " meters"

        })

    }
}
