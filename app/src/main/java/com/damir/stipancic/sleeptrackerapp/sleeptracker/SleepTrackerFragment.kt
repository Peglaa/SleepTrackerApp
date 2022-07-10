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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.damir.stipancic.sleeptrackerapp.R
import com.damir.stipancic.sleeptrackerapp.database.SleepDatabase
import com.damir.stipancic.sleeptrackerapp.databinding.FragmentSleepTrackerBinding
import com.google.android.material.snackbar.Snackbar

/**
 * A fragment with buttons to record start and end times for sleep, which are saved in
 * a database. Cumulative data is displayed in a simple scrollable TextView.
 * (Because we have not learned about RecyclerView yet.)
 */
class SleepTrackerFragment : Fragment() {

    /**
     * Called when the Fragment is ready to display content to the screen.
     *
     * This function uses DataBindingUtil to inflate R.layout.fragment_sleep_quality.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentSleepTrackerBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_sleep_tracker, container, false)

        val application = requireNotNull(this.activity).application

        val dataSource = SleepDatabase.getInstance(application).sleepDatabaseDao

        val viewModelFactory = SleepTrackerViewModelFactory(dataSource, application)

        val sleepTrackerModel = ViewModelProvider(this, viewModelFactory)[SleepTrackerViewModel::class.java]

        binding.sleepTrackerViewModel = sleepTrackerModel

        binding.lifecycleOwner = this

        val adapter = SleepNightAdapter(SleepNightListener {
            nightId -> sleepTrackerModel.onSleepNightClicked(nightId)
        })

        val manager = GridLayoutManager(activity, 3)
        binding.sleepList.layoutManager = manager
        binding.sleepList.adapter = adapter

        sleepTrackerModel.nights.observe(viewLifecycleOwner){
            it?.let {
                adapter.submitList(it)
            }

        }

        //WE NEED TO WRAP THIS IN A LET BLOCK WITH A NULL CHECK BECAUSE DONENAVIGATING() WILL TRIGGER OBSERVER AGAIN AND IF ITS NULL IT WONT EXECUTE AGAIN
        sleepTrackerModel.navigateToSleepQuality.observe(viewLifecycleOwner) {
            it?.let {
                val navAction = SleepTrackerFragmentDirections.actionSleepTrackerFragmentToSleepQualityFragment(it.nightId)
                findNavController().navigate(navAction)
                sleepTrackerModel.doneNavigating()
            }
        }

        sleepTrackerModel.navigateToSleepDataQuality.observe(viewLifecycleOwner){
            it?.let {
                this.findNavController().navigate(
                    SleepTrackerFragmentDirections.actionSleepTrackerFragmentToSleepDetailFragment(it))
                sleepTrackerModel.onSleepDataQualityNavigated()
            }
        }

        sleepTrackerModel.showSnackBarEvent.observe(viewLifecycleOwner) {
            if (it) { // Observed state is true.
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    getString(R.string.cleared_message),
                    Snackbar.LENGTH_SHORT // How long to display the message.
                ).show()
                sleepTrackerModel.doneShowingSnackbar()
            }
        }

        return binding.root
    }
}
