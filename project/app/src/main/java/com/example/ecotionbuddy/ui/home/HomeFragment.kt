package com.example.ecotionbuddy.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.ecotionbuddy.R
import com.example.ecotionbuddy.ChatbotActivity
import com.example.ecotionbuddy.ScanActivity
import com.example.ecotionbuddy.databinding.FragmentHomeBinding
import java.util.Locale

class HomeFragment : Fragment() {

	private var _binding: FragmentHomeBinding? = null
	private val homeViewModel: HomeViewModel by viewModels()

	// This property is only valid between onCreateView and
	// onDestroyView.
	private val binding get() = _binding!!

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {

		_binding = FragmentHomeBinding.inflate(inflater, container, false)
		val root: View = binding.root

		setupObservers()
		setupClickListeners()
		
		return root
	}

	private fun setupObservers() {
		homeViewModel.user.observe(viewLifecycleOwner) { user ->
			user?.let {
				// Use the user's first name for a more personal greeting
				binding.tvGreeting.text = getString(R.string.greeting_format, it.name.split(" ").first())

				// FIX: Correctly format the points with dot separators
				binding.tvPoints.text = String.format(Locale("id", "ID"), "%,d", it.points)
			}
		}

		homeViewModel.featuredMission.observe(viewLifecycleOwner) { mission ->
			mission?.let {
				binding.tvMissionTitle.text = it.title
				binding.tvMissionDesc.text = it.description
				binding.tagPoints.text = "${it.pointsReward} Poin"
				binding.tagPlastik.text = it.category.displayName

				val daysRemaining = ((it.deadline - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()

				// BEST PRACTICE: Use a plurals resource for "days left"
				binding.tvDaysLeft.text = resources.getQuantityString(R.plurals.plural_days_left, daysRemaining, daysRemaining)
			}
		}
	}
	
	private fun setupClickListeners() {
		binding.btnTanyaAI.setOnClickListener {
			startActivity(Intent(requireContext(), ChatbotActivity::class.java))
		}
		
		binding.btnScanWaste.setOnClickListener {
			startActivity(Intent(requireContext(), ScanActivity::class.java))
		}
		
		binding.btnStartMission.setOnClickListener {
			// Navigate to mission details or start mission flow
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}