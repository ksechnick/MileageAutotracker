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
fun convertDurationToFormatted(startTimeMilli: Long, endTimeMilli: Long, res: Resources): String {
    val durationMilli = endTimeMilli - startTimeMilli
    val seconds = TimeUnit.SECONDS.convert(durationMilli, TimeUnit.MILLISECONDS)
    val minutes = TimeUnit.MINUTES.convert(durationMilli, TimeUnit.MILLISECONDS)
    val hours = TimeUnit.HOURS.convert(durationMilli, TimeUnit.MILLISECONDS)
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
 * Returns a string representing the numeric quality rating.
 */
fun convertNumericQualityToString(quality: Int, resources: Resources): String {
    var qualityString = resources.getString(R.string.three_ok)
    when (quality) {
        -1 -> qualityString = "--"
        0 -> qualityString = resources.getString(R.string.zero_very_bad)
        1 -> qualityString = resources.getString(R.string.one_poor)
        2 -> qualityString = resources.getString(R.string.two_soso)
        4 -> qualityString = resources.getString(R.string.four_pretty_good)
        5 -> qualityString = resources.getString(R.string.five_excellent)
    }
    return qualityString
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
    ;
    var c = 2 * atan2(sqrt(a), sqrt(1-a))
    var d = R * c // Distance in km
    return d

}

fun deg2rad(deg:Double) : Double{
    return deg*(Math.PI/180)
}

fun doLocationsOverlap(lat1: Double, long1:Double, horAcc1: Float, lat2: Double, long2:Double, horAcc2: Float) : Boolean {
    var distance= FloatArray(0)
    Location.distanceBetween(lat1, long1, lat2, long2, distance)
    return (distance[0] >= (horAcc1+horAcc2))
}


/**
 * ViewHolder that holds a single [TextView].
 *
 * A ViewHolder holds a view for the [RecyclerView] as well as providing additional information
 * to the RecyclerView such as where on the screen it was last drawn during scrolling.
 */
class TextItemViewHolder(val textView: TextView): RecyclerView.ViewHolder(textView)
