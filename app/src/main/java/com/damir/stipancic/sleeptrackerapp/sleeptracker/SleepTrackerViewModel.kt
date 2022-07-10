/*
 * Copyright 2018, The Android Open Source Project
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

package com.damir.stipancic.sleeptrackerapp.sleeptracker

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.damir.stipancic.sleeptrackerapp.database.SleepDatabaseDao
import com.damir.stipancic.sleeptrackerapp.database.SleepNight
import com.damir.stipancic.sleeptrackerapp.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

        override fun onCleared() {
                super.onCleared()
                viewModelScope.launch {
                        // Clear the database table.
                        clear()

                        // And clear tonight since it's no longer in the database
                        tonight.value = null
                }
        }

        private var tonight = MutableLiveData<SleepNight?>()

        val nights = database.getAllNights()

        val nightsString = Transformations.map(nights){nights ->
                formatNights(nights, application.resources)
        }

        private var _navigateToSleepQuality = MutableLiveData<SleepNight?>()
        val navigateToSleepQuality : LiveData<SleepNight?>
                get() = _navigateToSleepQuality

        fun doneNavigating(){
                _navigateToSleepQuality.value = null
                tonight.value = null
        }

        private fun initializeTonight() {
                viewModelScope.launch {
                        tonight.value = getTonightFromDatabase()
                }
        }

        init {
                initializeTonight()
        }

        val startButtonVisible = Transformations.map(tonight) {
                null == it
        }
        val stopButtonVisible = Transformations.map(tonight) {
                null != it
        }
        val clearButtonVisible = Transformations.map(nights) {
                it?.isNotEmpty()
        }

        private var _showSnackbarEvent = MutableLiveData<Boolean>()

        val showSnackBarEvent: LiveData<Boolean>
                get() = _showSnackbarEvent

        fun doneShowingSnackbar() {
                _showSnackbarEvent.value = false
        }

        private val _navigateToSleepDataQuality = MutableLiveData<Long>()
        val navigateToSleepDataQuality
                get() = _navigateToSleepDataQuality

        fun onSleepNightClicked(id : Long){
                _navigateToSleepDataQuality.value = id
        }

        fun onSleepDataQualityNavigated(){
                _navigateToSleepDataQuality.value = null
        }

        private suspend fun getTonightFromDatabase(): SleepNight? {
                return withContext(Dispatchers.IO){
                        var night = database.getTonight()
                        if(night?.endTimeMilli != night?.startTimeMilli)
                                night = null

                        night
                }
        }

        fun onStartTracking(){
                viewModelScope.launch {
                        val newNight = SleepNight()
                        insert(newNight)
                        tonight.value = getTonightFromDatabase()
                        Log.d("ViewModelInsert", "onStartTracking: " + tonight.value)
                }
        }

        private suspend fun insert(newNight : SleepNight){
                withContext(Dispatchers.IO){
                        database.insert(newNight)
                }

        }

        fun onStopTracking(){
                viewModelScope.launch {
                        val oldNight = tonight.value ?: return@launch

                        oldNight.endTimeMilli = System.currentTimeMillis()

                        update(oldNight)
                        Log.d("ViewModelInsert", "onStopTracking: $oldNight")
                        Log.d("ViewModelInsert", "onStopTrackingNav: " + _navigateToSleepQuality.value)
                        _navigateToSleepQuality.value = oldNight
                }

        }

        private suspend fun update(oldNight : SleepNight){
                withContext(Dispatchers.IO){
                        database.update(oldNight)
                }
        }

        fun onClear(){
                viewModelScope.launch {
                        clear()
                        tonight.value = null
                        _showSnackbarEvent.value = true
                }
        }

        private suspend fun clear(){
                withContext(Dispatchers.IO){
                        database.clear()
                }
        }
}

