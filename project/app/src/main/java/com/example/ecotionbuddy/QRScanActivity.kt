package com.example.ecotionbuddy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ecotionbuddy.databinding.ActivityQrScanBinding
import com.example.ecotionbuddy.data.network.RetrofitClient
import com.example.ecotionbuddy.data.network.StartSessionRequest
import com.example.ecotionbuddy.utils.PreferencesManager
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRScanActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityQrScanBinding
    private var imageAnalysis: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isProcessing = false
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Izin kamera diperlukan untuk memindai QR", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkCameraPermission()
        
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    private fun setupUI() {
        binding.backButton.setOnClickListener {
            finish()
        }
        
        binding.tvInstructions.text = "Arahkan kamera ke QR code tempat sampah untuk memulai sesi"
    }
    
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }
            
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrCode ->
                        if (!isProcessing) {
                            isProcessing = true
                            processQRCode(qrCode)
                        }
                    })
                }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Gagal memulai kamera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun processQRCode(qrCode: String) {
        runOnUiThread {
            binding.loadingIndicator.visibility = android.view.View.VISIBLE
            binding.tvInstructions.text = "Memproses QR code..."
        }
        
        lifecycleScope.launch {
            try {
                // Extract binId from QR code (format: ecotionbuddy://bin/{binId})
                val binId = extractBinId(qrCode)
                if (binId.isNullOrEmpty()) {
                    throw Exception("QR code tidak valid")
                }
                
                // Get logged in user
                val preferencesManager = PreferencesManager(this@QRScanActivity)
                val userId = preferencesManager.userId
                if (userId.isNullOrEmpty()) {
                    throw Exception("Silakan login terlebih dahulu")
                }
                
                // Start session with backend
                val sessionRequest = StartSessionRequest(
                    userId = userId,
                    binId = binId,
                    countdownMs = 3000
                )
                
                val response = RetrofitClient.api.startSession(sessionRequest)
                
                runOnUiThread {
                    binding.loadingIndicator.visibility = android.view.View.GONE
                    Toast.makeText(this@QRScanActivity, 
                        "Sesi dimulai! Silakan buang sampah ke tempat sampah.", 
                        Toast.LENGTH_LONG).show()
                    
                    // Navigate back to main activity
                    val intent = Intent(this@QRScanActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("session_started", true)
                        putExtra("session_id", response.sessionId)
                        putExtra("bin_id", binId)
                    }
                    startActivity(intent)
                    finish()
                }
                
            } catch (e: Exception) {
                runOnUiThread {
                    binding.loadingIndicator.visibility = android.view.View.GONE
                    binding.tvInstructions.text = "Arahkan kamera ke QR code tempat sampah"
                    Toast.makeText(this@QRScanActivity, 
                        "Gagal memulai sesi: ${e.message}", 
                        Toast.LENGTH_SHORT).show()
                    isProcessing = false
                }
            }
        }
    }
    
    private fun extractBinId(qrCode: String): String? {
        return try {
            // Support multiple QR formats:
            // 1. ecotionbuddy://bin/{binId}
            // 2. https://ecotionbuddy.com/bin/{binId}
            // 3. Just the binId directly
            
            when {
                qrCode.startsWith("ecotionbuddy://bin/") -> {
                    qrCode.substringAfter("ecotionbuddy://bin/")
                }
                qrCode.contains("/bin/") -> {
                    qrCode.substringAfter("/bin/").substringBefore("?")
                }
                qrCode.matches(Regex("^[a-zA-Z0-9-_]+$")) -> {
                    qrCode // Assume it's a direct binId
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    
    private class QRCodeAnalyzer(private val onQRCodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
        
        private val scanner = BarcodeScanning.getClient()
        
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            when (barcode.valueType) {
                                Barcode.TYPE_TEXT, Barcode.TYPE_URL -> {
                                    barcode.rawValue?.let { qrCode ->
                                        onQRCodeDetected(qrCode)
                                    }
                                }
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
}
