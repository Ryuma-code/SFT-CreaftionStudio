package com.example.ecotionbuddy.utils

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Context Extensions
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

// Fragment Extensions
fun Fragment.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().showToast(message, duration)
}

// Number Extensions
fun Int.formatWithDots(): String {
    return NumberFormat.getNumberInstance(Locale("id", "ID")).format(this)
}

fun Double.formatWithDots(): String {
    return NumberFormat.getNumberInstance(Locale("id", "ID")).format(this)
}

// Date Extensions
fun Long.formatDate(pattern: String = "dd MMM yyyy"): String {
    val dateFormat = SimpleDateFormat(pattern, Locale("id", "ID"))
    return dateFormat.format(Date(this))
}

fun Long.formatDateTime(pattern: String = "dd MMM yyyy, HH:mm"): String {
    val dateFormat = SimpleDateFormat(pattern, Locale("id", "ID"))
    return dateFormat.format(Date(this))
}

// Time Extensions
fun Long.timeAgo(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    
    return when {
        diff < 60 * 1000 -> "Baru saja"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} menit yang lalu"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} jam yang lalu"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} hari yang lalu"
        else -> this.formatDate()
    }
}

// String Extensions
fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase() else it.toString() 
        }
    }
}