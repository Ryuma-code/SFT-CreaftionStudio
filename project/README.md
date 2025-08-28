# EcotionBuddy - Environmental Sustainability Android App

EcotionBuddy adalah aplikasi Android yang dirancang untuk meningkatkan kesadaran lingkungan dan mendorong praktik berkelanjutan melalui gamifikasi dan teknologi AI.

## 🌱 Fitur Utama

### 1. **Pemindaian Sampah dengan AI**
- Deteksi otomatis kategori sampah menggunakan kamera
- Saran daur ulang yang dipersonalisasi
- Sistem poin untuk setiap pemindaian

### 2. **Sistem Misi Gamifikasi**
- Misi harian dan mingguan untuk mengumpulkan sampah
- Sistem level dan poin
- Reward dan achievement

### 3. **AI Chatbot Lingkungan**
- Konsultasi tentang pengelolaan sampah
- Tips dan trik ramah lingkungan
- Panduan daur ulang

### 4. **Pelacakan Statistik**
- Riwayat aktivitas pengguna
- Grafik perkembangan poin
- Statistik dampak lingkungan

### 5. **Profil Pengguna**
- Manajemen akun
- Pencapaian dan badge
- Pengaturan aplikasi

## 🏗️ Arsitektur Aplikasi

### **MVVM Architecture**
- **Model**: Data models dan repository pattern
- **View**: Activities, Fragments, dan UI components
- **ViewModel**: Business logic dan state management

### **Komponen Utama**
```
app/
├── data/
│   ├── database/          # Room database
│   ├── models/           # Data models
│   └── repository/       # Repository pattern
├── ui/
│   ├── home/            # Home screen
│   ├── scan/            # Waste scanning
│   ├── history/         # Activity history
│   └── account/         # User profile
└── utils/               # Utilities dan extensions
```

## 🛠️ Teknologi yang Digunakan

### **Core Android**
- **Kotlin** - Bahasa pemrograman utama
- **Material Design 3** - UI/UX framework
- **ViewBinding** - Type-safe view references
- **Fragments & Activities** - UI components

### **Database & Storage**
- **Room Database** - Local data persistence
- **SharedPreferences** - User preferences
- **SQLite** - Underlying database

### **Camera & Image Processing**
- **CameraX** - Modern camera API
- **Image Capture** - Photo capture functionality
- **Gallery Integration** - Image selection

### **Reactive Programming**
- **LiveData** - Observable data holder
- **Coroutines** - Asynchronous programming
- **Flow** - Reactive streams

## 🎨 Design System

### **Color Palette**
- **Primary**: Green shades (#006E2A, #7CDA85)
- **Secondary**: Complementary greens (#506350, #B7CCB5)
- **Background**: Light green tints (#F8FAF0)
- **Surface**: Clean whites and light grays

### **Typography**
- **Headlines**: Bold, clear hierarchy
- **Body Text**: Readable, accessible
- **Labels**: Consistent sizing

### **Components**
- **Material Cards** - Content containers
- **Chips** - Category tags
- **Buttons** - Primary and secondary actions
- **Bottom Navigation** - Main navigation

## 📱 Screens Overview

### **1. Home Screen**
- Welcome message dengan nama pengguna
- Display poin dan level saat ini
- Featured mission card
- Quick access buttons (AI Chat, Scan)

### **2. Scan Screen**
- Camera preview dengan overlay
- Flash toggle
- Gallery selection
- Real-time waste detection

### **3. Scan Result Screen**
- Detected waste category
- Confidence level
- Recycling suggestions
- Points earned

### **4. AI Chat Screen**
- Conversational interface
- Environmental tips
- Waste management guidance

### **5. History Screen**
- Activity timeline
- Points statistics
- Mission completion history
- Charts and graphs

### **6. Account Screen**
- User profile information
- Settings and preferences
- Achievement badges
- App information

## 🚀 Setup Instructions

### **Prerequisites**
- Android Studio Arctic Fox atau lebih baru
- Android SDK 26+ (Target SDK 35)
- Kotlin 1.9.24+

### **Installation**
1. Clone repository ini
2. Buka project di Android Studio
3. Sync Gradle files
4. Build dan run aplikasi

### **Permissions Required**
- `CAMERA` - Untuk pemindaian sampah
- `READ_EXTERNAL_STORAGE` - Untuk akses galeri
- `INTERNET` - Untuk AI features (future)

## 🔧 Configuration

### **Database**
Aplikasi menggunakan Room database dengan entities:
- `UserEntity` - Data pengguna
- `MissionEntity` - Data misi
- `HistoryEntity` - Riwayat aktivitas

### **AI Integration**
Saat ini menggunakan mock data untuk demonstrasi. Untuk implementasi production:
- Integrasikan dengan TensorFlow Lite untuk deteksi offline
- Atau gunakan cloud-based AI service

## 📊 Data Models

### **User Model**
```kotlin
data class User(
    val id: String,
    val name: String,
    val email: String,
    val points: Int,
    val level: Int,
    val totalMissionsCompleted: Int,
    val totalWasteCollected: Double
)
```

### **Mission Model**
```kotlin
data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val category: WasteCategory,
    val pointsReward: Int,
    val targetAmount: Double,
    val deadline: Long
)
```

## 🎯 Future Enhancements

### **Phase 2 Features**
- [ ] Real AI waste detection
- [ ] Social features (leaderboards, sharing)
- [ ] Location-based missions
- [ ] Push notifications
- [ ] Offline mode improvements

### **Phase 3 Features**
- [ ] Marketplace untuk produk daur ulang
- [ ] Community challenges
- [ ] Educational content library
- [ ] Integration dengan waste management services

## 🤝 Contributing

1. Fork repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Team

- **Muhammad Rafli Putra Persada** - Lead Developer
- **Nurian Alyasa** - Co-Developer

## 📞 Support

Untuk pertanyaan atau dukungan, silakan hubungi:
- Email: support@ecotionbuddy.com
- GitHub Issues: [Create an issue](https://github.com/your-repo/issues)

---

**EcotionBuddy** - Mengubah Sampah, Menyelamatkan Bumi 🌍♻️