package com.example.ecotionbuddy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecotionbuddy.databinding.ItemMissionBinding
import com.example.ecotionbuddy.data.network.Mission

class MissionAdapter(
    private val onMissionClick: (Mission) -> Unit
) : ListAdapter<Mission, MissionAdapter.MissionViewHolder>(MissionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MissionViewHolder {
        val binding = ItemMissionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MissionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MissionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MissionViewHolder(
        private val binding: ItemMissionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mission: Mission) {
            binding.apply {
                tvMissionTitle.text = mission.title
                tvMissionDescription.text = mission.description
                tvMissionReward.text = "${mission.reward_points} poin"
                tvMissionDuration.text = "${mission.duration_days} hari"
                
                // Show progress if mission is active
                if (mission.progress > 0) {
                    progressBar.visibility = android.view.View.VISIBLE
                    tvProgress.visibility = android.view.View.VISIBLE
                    progressBar.max = mission.target
                    progressBar.progress = mission.progress
                    tvProgress.text = "${mission.progress}/${mission.target}"
                } else {
                    progressBar.visibility = android.view.View.GONE
                    tvProgress.visibility = android.view.View.GONE
                }
                
                // Set mission type icon
                when (mission.type) {
                    "scan" -> ivMissionIcon.setImageResource(R.drawable.ic_camera)
                    "dispose" -> ivMissionIcon.setImageResource(R.drawable.ic_delete)
                    else -> ivMissionIcon.setImageResource(R.drawable.ic_star)
                }
                
                root.setOnClickListener {
                    onMissionClick(mission)
                }
            }
        }
    }

    private class MissionDiffCallback : DiffUtil.ItemCallback<Mission>() {
        override fun areItemsTheSame(oldItem: Mission, newItem: Mission): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Mission, newItem: Mission): Boolean {
            return oldItem == newItem
        }
    }
}
