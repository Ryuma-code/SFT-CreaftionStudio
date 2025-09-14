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
            
            // Get classification result from intent (from real model inference)
            val label = intent.getStringExtra("classification_label") ?: "plastic"
            val confidence = intent.getFloatExtra("classification_confidence", 0.95f)
            
            // Map model labels to WasteCategory
            val category = when (label.lowercase()) {
                "plastic" -> WasteCategory.PLASTIC
                "paper" -> WasteCategory.PAPER
                "glass" -> WasteCategory.GLASS
                "metal" -> WasteCategory.METAL
                "cardboard" -> WasteCategory.PAPER
                else -> WasteCategory.PLASTIC
            }
            
            val suggestions = getSuggestionsForCategory(category)
            
            val detectionResult = WasteDetection(
                id = "detection_${System.currentTimeMillis()}",
                detectedCategory = category,
                confidence = confidence,
                suggestions = suggestions,
                pointsEarned = calculatePoints(category, confidence)
            )
            displayDetectionResult(detectionResult)
        }
    }
    
    private fun getSuggestionsForCategory(category: WasteCategory): List<RecyclingSuggestion> {
        return when (category) {
            WasteCategory.PLASTIC -> listOf(
                RecyclingSuggestion(
                    title = "Pot Tanaman dari Botol Plastik",
                    description = "Ubah botol plastik menjadi pot tanaman yang cantik",
                    steps = listOf(
                        "Cuci bersih botol plastik",
                        "Potong bagian atas botol",
                        "Buat lubang drainase di bagian bawah",
                        "Hias dengan cat atau kertas warna-warni",
                        "Isi dengan tanah dan tanaman"
                    ),
                    difficulty = "Mudah",
                    estimatedTime = "30 menit",
                    materialsNeeded = listOf("Gunting", "Cat", "Tanah", "Bibit tanaman")
                )
            )
            WasteCategory.PAPER -> listOf(
                RecyclingSuggestion(
                    title = "Kerajinan Origami",
                    description = "Buat kerajinan cantik dari kertas bekas",
                    steps = listOf(
                        "Potong kertas menjadi bentuk persegi",
                        "Lipat mengikuti pola origami",
                        "Bentuk sesuai kreativitas"
                    ),
                    difficulty = "Sedang",
                    estimatedTime = "20 menit",
                    materialsNeeded = listOf("Kertas", "Lem")
                )
            )
            WasteCategory.GLASS -> listOf(
                RecyclingSuggestion(
                    title = "Vas Bunga dari Botol Kaca",
                    description = "Jadikan botol kaca sebagai vas bunga unik",
                    steps = listOf(
                        "Bersihkan botol kaca",
                        "Hias dengan tali atau cat kaca",
                        "Isi dengan air dan bunga"
                    ),
                    difficulty = "Mudah",
                    estimatedTime = "15 menit",
                    materialsNeeded = listOf("Tali", "Cat kaca")
                )
            )
            WasteCategory.METAL -> listOf(
                RecyclingSuggestion(
                    title = "Tempat Pensil dari Kaleng",
                    description = "Ubah kaleng bekas menjadi tempat pensil",
                    steps = listOf(
                        "Bersihkan kaleng",
                        "Haluskan tepi yang tajam",
                        "Hias dengan kertas atau cat"
                    ),
                    difficulty = "Mudah",
                    estimatedTime = "25 menit",
                    materialsNeeded = listOf("Amplas", "Kertas hias", "Lem")
                )
            )
            else -> emptyList()
        }
    }
    
    private fun calculatePoints(category: WasteCategory, confidence: Float): Int {
        val basePoints = when (category) {
            WasteCategory.PLASTIC -> 50
            WasteCategory.PAPER -> 30
            WasteCategory.GLASS -> 40
            WasteCategory.METAL -> 60
            else -> 25
        }
        return (basePoints * confidence).toInt()
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