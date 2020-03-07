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

import android.annotation.SuppressLint
import android.content.res.Resources
import android.location.Location
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.math.*

/**
 * These functions create a formatted string that can be set in a TextView.
 */

private val ONE_MINUTE_MILLIS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES)
private val ONE_HOUR_MILLIS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)

/**
 * Convert a duration to a formatted string for display.
 *
 * Examples:
 *
 * 6 seconds on Wednesday
 * 2 minutes on Monday
 * 40 hours on Thursday
 *
 * @param startTimeMilli the start of the interval
 * @param endTimeMilli the end of the interval
 * @param res resources used to load formatted strings
 */
fun convertDurationToFormatted(startTimeMilli: Long, endTimeMilli: Long): String {
 // TODO fix time so hours doesn't also include minutes
    var timeRemainder =endTimeMilli - startTimeMilli

    //at each step, calculate the integer number of hours, then convert back to ms and subtract from time to leave remainder at next set of units
    // without this you end up with silly notations like 1 hr 72 min, instead of 1hr 12 min
    val hours = TimeUnit.HOURS.convert(timeRemainder, TimeUnit.MILLISECONDS)
    timeRemainder -= TimeUnit.MILLISECONDS.convert(hours, TimeUnit.HOURS)
    val minutes = TimeUnit.MINUTES.convert(timeRemainder, TimeUnit.MILLISECONDS)
    timeRemainder -= TimeUnit.MILLISECONDS.convert(hours, TimeUnit.MINUTES)
    val seconds = TimeUnit.SECONDS.convert(timeRemainder, TimeUnit.MILLISECONDS)
    timeRemainder -= TimeUnit.MILLISECONDS.convert(hours, TimeUnit.SECONDS) // ms left over
    return hours.toString()+"hrs "+minutes.toString()+"min"
//    return when {
//        durationMilli < ONE_MINUTE_MILLIS -> {
//            val seconds = TimeUnit.SECONDS.convert(durationMilli, TimeUnit.MILLISECONDS)
//            res.getString(R.string.seconds_length, seconds, weekdayString)
//        }
//        durationMilli < ONE_HOUR_MILLIS -> {
//            val minutes = TimeUnit.MINUTES.convert(durationMilli, TimeUnit.MILLISECONDS)
//            res.getString(R.string.minutes_length, minutes, weekdayString)
//        }
//        else -> {
//            val hours = TimeUnit.HOURS.convert(durationMilli, TimeUnit.MILLISECONDS)
//            res.getString(R.string.hours_length, hours, weekdayString)
//        }
//    }
}

/**
 * Take the Long milliseconds returned by the system and stored in Room,
 * and convert it to a nicely formatted string for display.
 *
 * MM - Display the number of the month
 * dd-yyyy - day in month and full year numerically
 */
@SuppressLint("SimpleDateFormat")
fun convertLongToDateString(systemTime: Long): String {
    return SimpleDateFormat("MM/dd/yyyy")
            .format(systemTime).toString()
}

@SuppressLint("SimpleDateFormat")
fun convertLongToFancyDateString(systemTime: Long): String {
    return SimpleDateFormat("EEE MM-dd-yyyy' Time: 'HH:mm")
            .format(systemTime).toString()
}

@SuppressLint("SimpleDateFormat")
fun convertLongToTimeString(systemTime: Long): String {
    return SimpleDateFormat("HH:mm")
            .format(systemTime).toString()
}


fun distanceBetween(lat1: Double, lon1:Double, lat2: Double, lon2:Double) : Double{
    val R = 6371000 // Radius of the earth in km
    val dLat = deg2rad(lat2-lat1)  // deg2rad below
    val dLon = deg2rad(lon2-lon1)
    val a =
            sin(dLat/2) * sin(dLat/2) +
                    cos(deg2rad(lat1)) * cos(deg2rad(lat2)) *
                    sin(dLon/2) * sin(dLon/2)
    val c = 2 * atan2(sqrt(a), sqrt(1-a))
    val d = R * c // Distance in km
    return d

}

fun deg2rad(deg:Double) : Double{
    return deg*(Math.PI/180)
}

fun doLocationsOverlap(lat1: Double, long1:Double, horAcc1: Float, lat2: Double, long2:Double, horAcc2: Float) : Boolean {
    val distance= FloatArray(0)
    Location.distanceBetween(lat1, long1, lat2, long2, distance)
    return (distance[0] >= (horAcc1+horAcc2))
}

@Suppress("UNCHECKED_CAST")
class SafeMutableLiveData<T>(value: T) : SafeLiveData<T>(value) {

    override fun getValue(): T = super.getValue()
    public override fun setValue(value: T) = super.setValue(value)
    public override fun postValue(value: T) = super.postValue(value)
}

@Suppress("UNCHECKED_CAST")
open class SafeLiveData<T>(value: T) : LiveData<T>(value) {

    override fun getValue(): T = super.getValue() as T
}