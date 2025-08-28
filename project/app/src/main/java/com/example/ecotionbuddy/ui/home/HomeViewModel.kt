package com.example.ecotionbuddy.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecotionbuddy.data.models.Mission
import com.example.ecotionbuddy.data.models.User
import com.example.ecotionbuddy.data.repository.MissionRepository
import com.example.ecotionbuddy.data.repository.UserRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val missionRepository: MissionRepository
) : ViewModel() {

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user
    
    private val _featuredMission = MutableLiveData<Mission>()
    val featuredMission: LiveData<Mission> = _featuredMission
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    init {
        loadUserData()
        loadFeaturedMission()
    }
    
    private fun loadUserData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // For demo purposes, create a sample user
                val sampleUser = User(
                    id = "user_1",
                    name = "Muhammad Rafli",
                    email = "rafli@email.com",
                    points = 150000,
                    level = 5,
                    totalMissionsCompleted = 12,
                    totalWasteCollected = 25.5
                )
                _user.value = sampleUser
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadFeaturedMission() {
        viewModelScope.launch {
            try {
                // For demo purposes, create a sample mission
                val sampleMission = Mission(
                    id = "mission_1",
                    title = "Kumpulkan Sampah Plastik",
                    description = "Ayo kumpulkan sampah plastik yang ada di sekitar rumahmu untuk mendapatkan poin tambahan!",
                    pointsReward = 1000,
                    targetAmount = 5.0,
                    currentProgress = 2.5,
                    deadline = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L), // 30 days
                    imageUrl = "https://images.pexels.com/photos/802221/pexels-photo-802221.jpeg"
                )
                _featuredMission.value = sampleMission
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}