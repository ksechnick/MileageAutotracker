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

import androidx.lifecycle.LiveData
import androidx.room.*


/**
 * Defines methods for using the mileage tracking classes with Room.
 */
@Dao
interface MileageDatabaseDao {

    @Insert
    fun startTrip(trip: RecordedTrip)

    @Insert
    fun recordPoint(point: RecordedPoint)

    @Update
    fun updateTrip(trip: RecordedTrip)

//    @Transaction
//    @Query("SELECT * from recorded_trips WHERE tripId = :key")
//    fun getTripWithPoints(key: Long): tripWithPoints

    /**
     * Deletes all values from the table.
     *
     * This does not delete the table, only its contents.
     */
    @Query("DELETE FROM recorded_trips")
    fun clearTrips()

    /**
     * Selects and returns all rows in the table,
     *
     * sorted by start time in descending order.
     */
    @Transaction
    @Query("SELECT * FROM recorded_trips ORDER BY start_time_milli DESC")
    fun getAllTrips(): LiveData<List<RecordedTrip>>

    /**
     * Selects and returns the latest night.
     */
    @Query("SELECT * FROM recorded_trips ORDER BY start_time_milli DESC LIMIT 1")
    fun getCurrentTrip(): RecordedTrip?

    /**
     * Selects and returns the specified night.
     */
    @Query("SELECT * from recorded_trips WHERE tripId = :key")
    fun getTrip(key: Long): RecordedTrip

    @Query("SELECT * from recorded_trips WHERE tripId = :key")
    fun getTripWithId(key: Long): LiveData<RecordedTrip>
}
