package com.sechnick.mileage_autotracker.database

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

import androidx.room.*

/**
 * Represents one trip
 */
@Entity(tableName = "recorded_trips")
data class RecordedTrip(
        @PrimaryKey(autoGenerate = true)
        var tripId: Long = 0L,

        //allows multisegment trips to be linked together
        @ColumnInfo(name = "previous_trip")
        var prevTripId: Long = 0L,

        @ColumnInfo(name = "next_trip")
        var nextTripId: Long = 0L,

        @ColumnInfo(name = "start_time_milli")
        val startTimeMilli: Long = System.currentTimeMillis(),

        @ColumnInfo(name = "end_time_milli")
        var endTimeMilli: Long = startTimeMilli,

        @ColumnInfo(name = "start_address")
        var startAddress: String = "",

        //key in locations database, if exists
        @ColumnInfo(name = "start_location")
        var startLocation: Long = 0L,

        @ColumnInfo(name = "end_address")
        var endAddress: String = "",

        //key in locations database, if exists
        @ColumnInfo(name = "end_location")
        var endLocation: Long = 0L,

        //false = personal segment, true = business segment
        @ColumnInfo(name = "business_trip")
        var businessTrip: Boolean = false,

        // in meters?
        //calculated from system location
        @ColumnInfo(name = "calculated_distance")
        var calculatedDistance: Double = -1.0,

        // from internet
        @ColumnInfo(name = "routed_distance")
        var routedDistance: Double = -1.0,

        @ColumnInfo(name = "start_mileage")
        var startMileage: Int = -1,

        @ColumnInfo(name = "end_mileage")
        var endMileage: Int = -1,

        @ColumnInfo(name = "comments")
        var comments: String = "",

        @ColumnInfo(name = "vehicle_id")
        var vehicleId: Int = -1
)

/**
 * Represents one measured point of one trip
 */
@Entity(tableName = "recorded_points")
data class RecordedPoint(
    @PrimaryKey(autoGenerate = true)
    val pointId: Long = 0L,

    //links to trip
    @ColumnInfo(name = "trip_id")
    var tripId: Long = 0L,

    //links to previous point
    @ColumnInfo(name = "previous_point")
    var prevPoint: Long = 0L,

    //links to next point
    @ColumnInfo(name = "next_point")
    var nextPoint: Long = 0L,

    //degrees
    @ColumnInfo(name = "latitude")
    var latitude: Double = 1000.0,

    @ColumnInfo(name = "longitude")
    var longitude: Double = 1000.0,

    @ColumnInfo(name = "horizontal_accuracy")
    var horizontalAccuracy: Float = 0.0f,

    @ColumnInfo(name = "bearing")
    var bearing: Float = 0.0f,

    @ColumnInfo(name = "bearing_accuracy")
    var bearingAccuracy: Float = 0.0f,

    //nanoseconds
    @ColumnInfo(name = "elapsed_time")
    var elapsedTime: Long = 0L,

    @ColumnInfo(name = "elapsed_time_uncertainty")
    var elapsedTimeUncertainty: Double = 0.0,

    //meters/second
    @ColumnInfo(name = "speed")
    var speed: Float = 0.0f,

    @ColumnInfo(name = "speed_accuracy")
    var speedAccuracy: Float = 0.0f,

    //likelyhood of this point based on model, needed if submitting routes to be displayed
    @ColumnInfo(name = "likelihood_score")
    var likelihood_score: Float = 0.0f
)

/**
 * Represents one known destination
 */
@Entity(tableName = "known_destinations")
data class KnownDestination(
    @PrimaryKey(autoGenerate = true)
    var destinationId: Long = 0L,

    @ColumnInfo(name = "name")
    var name: String = "",

    //degrees
    @ColumnInfo(name = "latitude")
    val latitude: Double = 1000.0,

    @ColumnInfo(name = "longitude")
    var longitude: Double = 1000.0,

    @ColumnInfo(name = "horizontal_accuracy")
    var horizontalAccuracy: Float = 0.0f,

    @ColumnInfo(name = "address")
    var address: String = "",

    @ColumnInfo(name = "comments")
    var comments: String = ""
)

/**
 * Represents one mileage report
 */
@Entity(tableName = "reported_mileage")
data class MileageReport(
    @PrimaryKey(autoGenerate = true)
    var reportId: Long = 0L,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "mileage")
    var mileage: Int = 0
)

/**
 * Combines a trip with all of its points
 */
data class TripWithPoints(
        @Embedded val RecordedTrip: RecordedTrip,
        @Relation( parentColumn = "tripId", entityColumn ="trip_id" )
    val points: List<RecordedPoint>
)