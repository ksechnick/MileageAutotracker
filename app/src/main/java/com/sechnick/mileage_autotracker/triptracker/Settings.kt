package com.sechnick.mileage_autotracker.triptracker

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.sechnick.mileage_autotracker.R
import com.sechnick.mileage_autotracker.database.MileageDatabase
import com.sechnick.mileage_autotracker.databinding.FragmentSettingsBinding
import com.sechnick.mileage_autotracker.databinding.FragmentTripTrackerBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Settings.newInstance] factory method to
 * create an instance of this fragment.
 */
class TripSettingsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    val tripTrackerViewModel : TripTrackerViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val binding: FragmentSettingsBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_settings, container, false)

        val application = requireNotNull(this.activity).application

        // Create an instance of the ViewModel Factory.
        val dataSource = MileageDatabase.getInstance(application).mileageDatabaseDao

        binding.tripTrackerViewModel= tripTrackerViewModel
        binding.lifecycleOwner = this

        return binding.root
    }

}
