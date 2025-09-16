# EcotionBuddy 🌱♻️

**Smart Waste Management System with AI-Powered Classification**

EcotionBuddy is an innovative IoT-based waste management solution that combines computer vision, mobile technology, and gamification to promote sustainable waste disposal practices. The system uses ESP32-CAM modules for real-time waste classification and an Android app for user engagement through missions and point rewards.

## 🚀 Features

### 📱 Android Mobile App
- **AI-Powered Waste Classification**: Real-time image classification using MobileNetV2
- **Mission System**: Gamified challenges to encourage recycling habits
- **Point Rewards**: Earn points for proper waste disposal and scanning
- **User Profiles**: Track progress, points, and completed missions
- **Cross-Network Support**: Works via ngrok tunneling or local network

### 🔧 IoT Hardware Integration
- **ESP32-CAM Integration**: Automated waste detection and classification
- **MQTT Communication**: Real-time device control and data streaming
- **Smart Bin Control**: Automated lid opening based on classification results
- **Multi-Device Support**: Scalable architecture for multiple smart bins

### 🖥️ Backend Services
- **FastAPI REST API**: High-performance async backend
- **MongoDB Database**: Scalable document storage for users, missions, and events
- **Real-time Classification**: TensorFlow/Keras model integration
- **Docker Containerization**: Easy deployment and scaling
- **MQTT Broker**: Device communication via Mosquitto

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Android App   │◄──►│  FastAPI Backend │◄──►│   MongoDB DB    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │  MQTT Broker    │
                       └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   ESP32-CAM     │
                       │   Smart Bins    │
                       └─────────────────┘
```

## 📁 Project Structure

```
EcotionBuddy/
├── project/                    # Android Application
│   ├── app/
│   │   ├── src/main/java/com/example/ecotionbuddy/
│   │   │   ├── ui/             # UI Components
│   │   │   ├── network/        # API Services
│   │   │   └── MainActivity.kt
│   │   └── build.gradle.kts
│   └── gradle/
├── backend/                    # FastAPI Backend
│   ├── app/
│   │   ├── main.py            # Main API endpoints
│   │   ├── classifier.py      # ML model integration
│   │   └── mqtt_worker.py     # MQTT handling
│   ├── model/                 # TensorFlow model files
│   └── Dockerfile
├── esp32/                     # ESP32-CAM Firmware
│   ├── Program_IOT_Semi_Final_SFT.ino
│   ├── app_httpd.cpp          # Camera web server
│   └── camera_index.h         # Web interface
├── mosquitto/                 # MQTT Broker Config
│   └── config/
└── docker-compose.dev.yml     # Development setup
```

## 🛠️ Setup & Installation

### Prerequisites
- **Android Studio** (for mobile app)
- **Docker & Docker Compose** (for backend services)
- **Arduino IDE** (for ESP32 programming)
- **Python 3.9+** (for local development)

### 1. Backend Setup

```bash
# Clone the repository
git clone https://github.com/yourusername/EcotionBuddy.git
cd EcotionBuddy

# Start backend services
docker-compose -f docker-compose.dev.yml up -d

# Verify services are running
docker-compose -f docker-compose.dev.yml ps
```

### 2. Android App Setup

```bash
# Open project in Android Studio
cd project/
# Import project and sync Gradle

# Configure API endpoint in build.gradle.kts
# For local development:
buildConfigField("String", "BASE_URL", "\"http://YOUR_LOCAL_IP:8000/\"")

# For external access (using ngrok):
buildConfigField("String", "BASE_URL", "\"https://YOUR_NGROK_URL.ngrok-free.app/\"")
```

### 3. ESP32-CAM Setup

```bash
# Install required libraries in Arduino IDE:
# - ESP32 Board Package
# - ArduinoJson
# - PubSubClient (for MQTT)

# Configure WiFi and MQTT settings in the .ino file
# Upload firmware to ESP32-CAM
```

### 4. External Access Setup (Optional)

For cross-network access, use ngrok:

```bash
# Install ngrok and authenticate
ngrok config add-authtoken YOUR_AUTHTOKEN

# Create tunnel to backend
ngrok http 8000

# Update Android app with ngrok URL
```

## 🔧 Configuration

### Environment Variables

Create a `.env` file in the root directory:

```env
# Database
MONGO_URI=mongodb://mongo:27017/ecotionbuddy
MONGO_DB=ecotionbuddy

# MQTT
MQTT_HOST=mqtt
MQTT_PORT=1883

# Model
MODEL_PATH=/app/model
MODEL_ENABLED=true

# Optional integrations
TELEGRAM_ENABLED=false
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_CHAT_ID=your_chat_id_here

# Storage
IMAGE_STORAGE=disk
UPLOADS_DIR=uploads

# Public endpoints (for external access)
PUBLIC_API_BASE=https://your-domain.com/
PUBLIC_MQTT_HOST=your-mqtt-host
PUBLIC_MQTT_PORT=1883
```

### Android App Configuration

Update `build.gradle.kts` with your API endpoints:

```kotlin
buildTypes {
    debug {
        buildConfigField("String", "BASE_URL", "\"http://192.168.1.100:8000/\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"your_gemini_api_key\"")
    }
}
```

## 📱 Usage

### Mobile App Features

1. **Waste Scanning**: Use camera to classify waste types
2. **Mission System**: Complete daily/weekly challenges
3. **Point Tracking**: Monitor your environmental impact
4. **Profile Management**: View statistics and achievements

### API Endpoints

Key backend endpoints:

```
GET  /missions                     # Available missions
GET  /users/{user_id}             # User profile
GET  /users/{user_id}/missions    # User's missions
POST /users/{user_id}/missions/{mission_id}/start  # Start mission
POST /events                      # Log user events
POST /classify                    # Image classification
POST /iot/camera/upload          # IoT image upload
```

### MQTT Topics

```
ecotionbuddy/ctrl/{device_id}     # Device control commands
ecotionbuddy/data/{device_id}     # Device data streams
ecotionbuddy/status/{device_id}   # Device status updates
```

## 🤖 Machine Learning

The system uses a pre-trained MobileNetV2 model for waste classification:

- **Input**: RGB images (224x224)
- **Output**: Waste category classification
- **Categories**: Plastic, Paper, Metal, Glass, Organic, etc.
- **Accuracy**: ~85% on test dataset

## 🎮 Gamification System

### Mission Types
- **Daily Scans**: Scan X items per day
- **Category Challenges**: Focus on specific waste types
- **Disposal Sessions**: Complete smart bin interactions
- **Eco Explorer**: Discover different waste categories

### Point System
- **Waste Scanning**: 25-50 points per scan
- **Mission Completion**: 100-500 points
- **Daily Streaks**: Bonus multipliers
- **Special Achievements**: Milestone rewards

## 🔒 Security & Privacy

- **Data Encryption**: All API communications use HTTPS
- **User Privacy**: No personal data stored beyond user ID
- **Image Processing**: Images processed locally, not stored permanently
- **Network Security**: Configurable network access controls

## 🚀 Deployment

### Production Deployment

```bash
# Build production images
docker-compose -f docker-compose.prod.yml build

# Deploy with environment variables
docker-compose -f docker-compose.prod.yml up -d

# Set up reverse proxy (nginx/traefik)
# Configure SSL certificates
# Set up monitoring and logging
```

### Scaling Considerations

- **Database**: MongoDB replica sets for high availability
- **Backend**: Horizontal scaling with load balancer
- **MQTT**: Clustered Mosquitto for device scalability
- **Storage**: Object storage for image files (S3/MinIO)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow Android/Kotlin coding standards
- Use FastAPI best practices for backend
- Write tests for new features
- Update documentation for API changes
- Follow semantic versioning

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **TensorFlow Team** for MobileNetV2 model
- **FastAPI** for the excellent web framework
- **ESP32 Community** for IoT integration examples
- **Android Jetpack** for modern UI components

## 📞 Support

For questions and support:

- **Issues**: [GitHub Issues](https://github.com/yourusername/EcotionBuddy/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/EcotionBuddy/discussions)
- **Documentation**: [Wiki](https://github.com/yourusername/EcotionBuddy/wiki)

---

**Made with 💚 for a sustainable future**