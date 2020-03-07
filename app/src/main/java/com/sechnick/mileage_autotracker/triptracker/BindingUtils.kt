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

import android.util.Log
import android.widget.Button
import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import com.google.android.material.chip.Chip
import com.sechnick.mileage_autotracker.*
import com.sechnick.mileage_autotracker.database.RecordedTrip
import java.text.DecimalFormat


@BindingAdapter("tripDuration")
fun TextView.setTripDuration(item: RecordedTrip?) {
    item?.let {
        text = convertDurationToFormatted(item.startTimeMilli, item.endTimeMilli)
    }
}

@BindingAdapter("customEnabled")
fun Button.setCustomEnabled(item: Boolean) {
    Log.d("data binding", "button visibility =$item")
    isEnabled = item
}


@BindingAdapter("tripBusinessPersonal")
fun TextView.setTripBusinessorPersonal(item: RecordedTrip?) {
    item?.let {
        text = if (it.businessTrip) {
            "Business"
        } else {
            "Personal"
        }
    }
}


@BindingAdapter("tripDate")
fun TextView.setTripDate(item: RecordedTrip?) {
    item?.let {
        text = convertLongToDateString(it.startTimeMilli)
    }
}

@BindingAdapter("tripStartTime")
fun TextView.setTripStartTime(item: RecordedTrip?) {
    item?.let {
        text = convertLongToTimeString(it.startTimeMilli) + "-" + convertLongToTimeString(it.endTimeMilli)
    }
}

@BindingAdapter("tripDistance")
fun TextView.setTripDistance(item: RecordedTrip?) {
    val df = DecimalFormat("####.##")
    item?.let {
        text = df.format(it.calculatedDistance) + " meters"
    }
}
@BindingAdapter("tripDistance")
fun TextView.setTripDistance(item: Double?) {
    val df = DecimalFormat("####.##")
    item?.let {
        text = df.format(it) + " meters"
    }
}



@BindingAdapter("tripLocations")
fun TextView.setTripLocations(item: RecordedTrip?) {
    var combinedText = ""
    item?.let {
        if (it.startAddress != ""){
            combinedText = "Start Address" +System.lineSeparator()+ it.startAddress + System.lineSeparator()+ System.lineSeparator()
        }

        if (it.startAddress != ""){
            combinedText = "End Address" +System.lineSeparator()+ it.endAddress + System.lineSeparator()
        }

        if (combinedText == ""){
            isVisible = false
            text = ""

        } else {
            isVisible = true
            text = combinedText
        }
    }
}

@BindingAdapter("trackingText")
fun TextView.setTrackingText(item: Boolean) {
    if (item) {
        text = "Tracking On"
    } else {
        text = "Tracking Off"
    }
}