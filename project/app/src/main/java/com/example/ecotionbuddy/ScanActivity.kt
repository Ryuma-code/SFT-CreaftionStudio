package com.example.ecotionbuddy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ecotionbuddy.databinding.ActivityScanBinding
import com.example.ecotionbuddy.ui.scan.ScanResultActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityScanBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isFlashOn = false
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Izin kamera diperlukan untuk memindai sampah", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            processImageFromGallery(it)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkCameraPermission()
        
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    private fun setupUI() {
        binding.backButton.setOnClickListener {
            finish()
        }
        
        binding.flashButton.setOnClickListener {
            toggleFlash()
        }
        
        binding.scanButton.setOnClickListener {
            takePhoto()
        }
        
        binding.galleryButton.setOnClickListener {
            openGallery()
        }
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
            
            imageCapture = ImageCapture.Builder().build()
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Gagal memulai kamera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun toggleFlash() {
        isFlashOn = !isFlashOn
        // Update flash icon based on state
        val flashIcon = if (isFlashOn) {
            R.drawable.flash_camera2 // Use appropriate flash on icon
        } else {
            R.drawable.flash_camera2 // Use appropriate flash off icon
        }
        binding.flashButton.setIconResource(flashIcon)
    }
    
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        binding.loadingIndicator.visibility = android.view.View.VISIBLE
        binding.scanButton.isEnabled = false
        
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
        
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    binding.loadingIndicator.visibility = android.view.View.GONE
                    binding.scanButton.isEnabled = true
                    Toast.makeText(this@ScanActivity, "Gagal mengambil foto", Toast.LENGTH_SHORT).show()
                }
                
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    binding.loadingIndicator.visibility = android.view.View.GONE
                    binding.scanButton.isEnabled = true
                    
                    output.savedUri?.let { uri ->
                        processImage(uri)
                    }
                }
            }
        )
    }
    
    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }
    
    private fun processImage(imageUri: Uri) {
        // Simulate AI processing delay
        binding.loadingIndicator.visibility = android.view.View.VISIBLE
        
        // In a real app, this would call an AI service
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            binding.loadingIndicator.visibility = android.view.View.GONE
            
            // Navigate to scan result activity
            val intent = Intent(this, ScanResultActivity::class.java).apply {
                putExtra("image_uri", imageUri.toString())
            }
            startActivity(intent)
        }, 2000)
    }
    
    private fun processImageFromGallery(imageUri: Uri) {
        processImage(imageUri)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}