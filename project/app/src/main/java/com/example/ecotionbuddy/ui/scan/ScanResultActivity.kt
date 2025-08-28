package com.example.ecotionbuddy.ui.scan

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecotionbuddy.R
import com.example.ecotionbuddy.data.models.RecyclingSuggestion
import com.example.ecotionbuddy.data.models.WasteCategory
import com.example.ecotionbuddy.data.models.WasteDetection
import com.example.ecotionbuddy.databinding.ActivityScanResultBinding

class ScanResultActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityScanResultBinding
    private lateinit var suggestionsAdapter: RecyclingSuggestionsAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        processDetectionResult()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.scan_result_title)
    }
    
    private fun setupRecyclerView() {
        suggestionsAdapter = RecyclingSuggestionsAdapter()
        binding.recyclerViewSuggestions.apply {
            adapter = suggestionsAdapter
            layoutManager = LinearLayoutManager(this@ScanResultActivity)
        }
    }
    
    private fun processDetectionResult() {
        val imageUriString = intent.getStringExtra("image_uri")
        imageUriString?.let { uriString ->
            val imageUri = Uri.parse(uriString)
            binding.imageViewResult.setImageURI(imageUri)
            
            // Simulate AI detection result
            val detectionResult = createSampleDetection()
            displayDetectionResult(detectionResult)
        }
    }
    
    private fun createSampleDetection(): WasteDetection {
        val suggestions = listOf(
            RecyclingSuggestion(
                title = getString(R.string.suggestion_plastic_pot_title),
                description = getString(R.string.suggestion_plastic_pot_desc),
                steps = listOf(
                    "Cuci bersih botol plastik",
                    "Potong bagian atas botol",
                    "Buat lubang drainase di bagian bawah",
                    "Hias dengan cat atau kertas warna-warni",
                    "Isi dengan tanah dan tanaman"
                ),
                difficulty = getString(R.string.difficulty_easy),
                estimatedTime = getString(R.string.time_30_minutes),
                materialsNeeded = listOf("Gunting", "Cat", "Tanah", "Bibit tanaman")
            ),
            RecyclingSuggestion(
                title = getString(R.string.suggestion_piggy_bank_title),
                description = getString(R.string.suggestion_piggy_bank_desc),
                steps = listOf(
                    "Siapkan botol plastik bersih",
                    "Buat celah untuk memasukkan uang",
                    "Hias dengan stiker atau cat",
                    "Tambahkan tutup yang bisa dibuka tutup"
                ),
                difficulty = getString(R.string.difficulty_easy),
                estimatedTime = getString(R.string.time_20_minutes),
                materialsNeeded = listOf("Cutter", "Stiker", "Lem")
            )
        )
        
        return WasteDetection(
            id = "detection_1",
            detectedCategory = WasteCategory.PLASTIC,
            confidence = 0.95f,
            suggestions = suggestions,
            pointsEarned = 50
        )
    }
    
    private fun displayDetectionResult(detection: WasteDetection) {
        binding.apply {
            chipCategory.text = detection.detectedCategory.displayName
            tvConfidence.text = getString(R.string.confidence_format, (detection.confidence * 100).toInt())
            tvPointsEarned.text = getString(R.string.points_earned_format, detection.pointsEarned)
            
            suggestionsAdapter.submitList(detection.suggestions)
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}