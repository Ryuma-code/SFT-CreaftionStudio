/* EcotionBuddy - Kontrol Penuh via Telegram (Perbaikan Final)
   -------------------------------------------------------------
   PERBAIKAN FINAL:
   - Mengubah cara pemeriksaan pesan Telegram menjadi non-blocking
     untuk mencegah program macet jika ada masalah koneksi internet.
   - Menambahkan timeout pada koneksi WiFiClientSecure.
*/

#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <UniversalTelegramBot.h>
#include <ArduinoJson.h>
#include <HTTPClient.h>
#include <ESP32Servo.h>
#include <PubSubClient.h>
#include "esp_camera.h"
#include "soc/soc.h"
#include "soc/rtc_cntl_reg.h"

// --- DEFINISI MODEL KAMERA ---
#define CAMERA_MODEL_AI_THINKER
#include "camera_pins.h"

// Deklarasi fungsi dari file app_httpd.cpp
void startCameraServer();

// -------- KONFIGURASI ----------
// PENTING: Pastikan WiFi ini memiliki koneksi INTERNET yang stabil.
const char* WIFI_SSID = "GREY"; 
const char* WIFI_PASS = "Grey12345";

// --- Konfigurasi Telegram ---
#define BOT_TOKEN "8462729221:AAGTxIl5sWVp_tsq-DOCEM7yxpOhNd53bcU"
#define CHAT_ID "6889536350"

// --- Konfigurasi Server Penerima Gambar & MQTT ---
// Gunakan domain stabil untuk HTTP API (via Cloudflare Tunnel). MQTT tetap LAN.
const char* http_server_url = "https://ecotionbuddy.ecotionbuddy.com/iot/camera/upload"; // kirim JPEG mentah (POST)
const char* HTTP_RESULT_URL = "https://ecotionbuddy.ecotionbuddy.com/iot/camera/result";  // kirim event JSON (POST)
const char* MQTT_SERVER = "192.168.137.252"; // broker MQTT lokal (tetap LAN)
const int   MQTT_PORT   = 1883;
const char* MQTT_USER   = "";
const char* MQTT_PASS   = "";
const char* MQTT_TOPIC_DEBUG_LOG = "ecotionbuddy/log";
const char* MQTT_TOPIC_EVENT     = "ecotionbuddy/events/disposal_complete"; // backend subscribe

// Identitas perangkat & bin (ubah sesuai kebutuhan)
const char* DEVICE_ID = "esp32cam-1";
const char* BIN_ID    = "bin-001";

// --- Pin Hardware & Perilaku Servo ---
const int PIN_SERVO = 13;
const int PIN_IR    = 4;
const int OPEN_ANGLE  = 180;
const int CLOSE_ANGLE = 0 ;
const unsigned long CLOSE_DELAY_MS = 500;
const bool IR_ACTIVE_LOW = true;

// ---------------- Globals ----------------
Servo servoMotor;
WiFiClientSecure client;
UniversalTelegramBot bot(BOT_TOKEN, client);
WiFiClient espClient;
PubSubClient mqttClient(espClient);

// Session & control state
String currentSessionId = "";
unsigned long scheduledCaptureAt = 0; // millis timestamp to trigger takeAndSendPhoto()

enum LidState { STATE_CLOSED, STATE_OPEN, STATE_WAITING_TO_CLOSE };
LidState currentLidState = STATE_CLOSED;
unsigned long stateChangeTimestamp = 0;
bool objectDetectedSinceOpen = false;

const unsigned long PHOTO_COOLDOWN_MS = 600;
unsigned long lastPhotoTimestamp = 0;

int botRequestDelay = 10;
unsigned long lastTimeBotChecked;

// --- FUNGSI LOGGING TERPUSAT (Serial, MQTT, Telegram) ---
void sendLog(String message) {
  Serial.println(message);
  if (mqttClient.connected()) {
    mqttClient.publish(MQTT_TOPIC_DEBUG_LOG, message.c_str());
  }
  bot.sendMessage(CHAT_ID, message, "");
}

// Publish disposal completion event with current session
void publishDisposalComplete(const char* label) {
  if (!mqttClient.connected()) return;
  StaticJsonDocument<256> ev;
  ev["deviceId"] = DEVICE_ID;
  ev["binId"] = BIN_ID;
  ev["label"] = label;
  ev["confidence"] = 1.0;
  if (currentSessionId.length() > 0) {
    ev["sessionId"] = currentSessionId;
  }
  char payload[256];
  size_t n = serializeJson(ev, payload, sizeof(payload));
  if (mqttClient.publish(MQTT_TOPIC_EVENT, (uint8_t*)payload, n)) {
    Serial.println("MQTT disposal_complete published");
  } else {
    Serial.println("MQTT disposal_complete publish failed");
  }
}

// Handle control commands from backend
void mqttCallback(char* topic, byte* payload, unsigned int length) {
  String body;
  body.reserve(length + 1);
  for (unsigned int i = 0; i < length; i++) body += (char)payload[i];
  Serial.print("CTRL msg topic="); Serial.print(topic); Serial.print(" body="); Serial.println(body);
  StaticJsonDocument<256> doc;
  DeserializationError err = deserializeJson(doc, body);
  if (err) {
    Serial.print("CTRL json error: "); Serial.println(err.c_str());
    return;
  }
  const char* action = doc["action"] | "";
  const char* sid = doc["sessionId"] | "";
  if (sid && strlen(sid) > 0) {
    currentSessionId = String(sid);
  }
  if (strcmp(action, "activate") == 0) {
    int countdownMs = doc["countdownMs"] | 3000;
    scheduledCaptureAt = millis() + (unsigned long)countdownMs;
    sendLog(String("SESSION activated, photo in ") + String(countdownMs) + " ms");
  } else if (strcmp(action, "open") == 0) {
    int angle = doc["angle"] | 180;
    (void)angle; // we use predefined OPEN_ANGLE
    openLid();
    currentLidState = STATE_OPEN;
    objectDetectedSinceOpen = false;
    sendLog("CTRL: Lid opened by backend");
  } else if (strcmp(action, "deactivate") == 0) {
    currentSessionId = "";
    sendLog("SESSION deactivated");
  }
}

// --- Variabel Global dan Fungsi Callback untuk Streaming Foto ---
camera_fb_t *fb_for_telegram = NULL;
int bytes_sent_for_telegram = 0;

bool moreDataAvailable() {
  if (fb_for_telegram != NULL && bytes_sent_for_telegram < fb_for_telegram->len) return true;
  return false;
}

byte getNextByte() {
  if (moreDataAvailable()) return fb_for_telegram->buf[bytes_sent_for_telegram++];
  return 0;
}

// --- FUNGSI PENGIRIMAN FOTO (HTTP & Telegram) ---
void takeAndSendPhoto() {
  if (millis() - lastPhotoTimestamp < PHOTO_COOLDOWN_MS) {
    sendLog("Cooldown foto aktif, coba lagi nanti.");
    return;
  }
  lastPhotoTimestamp = millis();
  sendLog("Mengambil gambar...");
  camera_fb_t* fb = esp_camera_fb_get();
  if (!fb) {
    sendLog("ERROR: Gagal mengambil gambar dari kamera.");
    return;
  }
  sendLog("INFO: Gambar diambil. Ukuran: " + String(fb->len) + " bytes");
  String uploadedUrl = ""; // URL gambar yang dikembalikan backend
  // Kirim gambar ke backend dengan deviceId & binId dalam query string
  HTTPClient http;
  String uploadUrl = String(http_server_url) + "?deviceId=" + DEVICE_ID + "&binId=" + BIN_ID;
  // gunakan WiFiClientSecure 'client' yang sudah setInsecure() untuk HTTPS
  http.begin(client, uploadUrl);
  http.addHeader("Content-Type", "image/jpeg");
  http.setTimeout(15000); // perbesar timeout total agar tidak cepat gagal ketika jaringan lambat
  int httpResponseCode = http.POST(fb->buf, fb->len);
  if (httpResponseCode > 0) {
    sendLog("INFO: HTTP POST ke server berhasil (kode: " + String(httpResponseCode) + ")");
    // Coba baca response JSON dan ambil field `url`
    String resp = http.getString();
    StaticJsonDocument<256> respDoc;
    DeserializationError jerr = deserializeJson(respDoc, resp);
    if (!jerr) {
      const char* url = respDoc["url"] | "";
      if (url && strlen(url) > 0) {
        uploadedUrl = String(url);
      }
    }
  } else {
    sendLog("ERROR: HTTP POST ke server gagal: " + http.errorToString(httpResponseCode));
  }
  http.end();

  // Publikasikan placeholder hasil CNN ke MQTT
  if (mqttClient.connected()) {
    StaticJsonDocument<256> ev;
    ev["deviceId"] = DEVICE_ID;
    ev["binId"] = BIN_ID;
    ev["label"] = "placeholder";     // TODO: ganti dengan label model CNN asli
    ev["confidence"] = 0.0;            // TODO: ganti dengan confidence asli
    if (uploadedUrl.length() > 0) {
      ev["imageUrl"] = uploadedUrl;
    }
    char payload[256];
    size_t n = serializeJson(ev, payload, sizeof(payload));
    if (mqttClient.publish(MQTT_TOPIC_EVENT, (uint8_t*)payload, n)) {
      Serial.println("MQTT event published");
    } else {
      Serial.println("MQTT publish failed");
    }
  }

  // Kirim juga event JSON ke backend via HTTP sebagai backup (opsional)
  {
    HTTPClient http2;
    // gunakan WiFiClientSecure 'client' untuk HTTPS
    http2.begin(client, HTTP_RESULT_URL);
    http2.addHeader("Content-Type", "application/json");
    http2.setTimeout(8000); // timeout moderat untuk event JSON
    StaticJsonDocument<256> ev2;
    ev2["deviceId"] = DEVICE_ID;
    ev2["binId"] = BIN_ID;
    ev2["label"] = "placeholder";
    ev2["confidence"] = 0.0;
    if (uploadedUrl.length() > 0) {
      ev2["imageUrl"] = uploadedUrl;
    }
    String body;
    serializeJson(ev2, body);
    int code2 = http2.POST(body);
    if (code2 > 0) {
      Serial.printf("HTTP JSON result posted: %d\n", code2);
    } else {
      Serial.printf("HTTP JSON post failed: %s\n", http2.errorToString(code2).c_str());
    }
    http2.end();
  }

  Serial.println("Mengirim foto ke Telegram...");
  fb_for_telegram = fb;
  bytes_sent_for_telegram = 0;
  String response = bot.sendMultipartFormDataToTelegram("sendPhoto", "photo", "esp32-cam.jpg", "image/jpeg", CHAT_ID, fb->len, moreDataAvailable, getNextByte, nullptr, nullptr);
  if (response.indexOf("\"ok\":true") > 0) {
    bot.sendMessage(CHAT_ID, "Foto dari EcotionBuddy.", "");
    sendLog("INFO: Foto berhasil dikirim ke Telegram.");
  } else {
    sendLog("ERROR: Gagal mengirim foto ke Telegram. Respons: " + response);
  }
  fb_for_telegram = NULL;
  esp_camera_fb_return(fb);
}

// --- FUNGSI KONTROL SERVO ---
void openLid() { servoMotor.write(OPEN_ANGLE); }
void closeLid() { servoMotor.write(CLOSE_ANGLE); }

// --- FUNGSI PENANGANAN PESAN TELEGRAM ---
void handleNewMessages(int numNewMessages) {
  for (int i = 0; i < numNewMessages; i++) {
    String chat_id = String(bot.messages[i].chat_id);
    if (chat_id != CHAT_ID) {
      bot.sendMessage(chat_id, "Maaf, Anda tidak diizinkan menggunakan bot ini.", "");
      continue;
    }
    String text = bot.messages[i].text;
    String from_name = bot.messages[i].from_name;
    if (text == "/start") {
      String welcome = "Selamat datang, " + from_name + ".\n";
      welcome += "Ini adalah Bot Kontrol EcotionBuddy.\n\n";
      welcome += "/buka : Membuka tutup tempat sampah.\n";
      welcome += "/tutup : Menutup paksa tutup.\n";
      welcome += "/foto : Mengambil foto manual.\n";
      welcome += "/status : Cek status terkini.\n";
      bot.sendMessage(CHAT_ID, welcome, "");
    }
    if (text == "/buka") {
      if (currentLidState == STATE_CLOSED) {
        openLid();
        currentLidState = STATE_OPEN;
        objectDetectedSinceOpen = false;
        sendLog("STATUS: Tutup dibuka berdasarkan perintah dari " + from_name + ".");
      } else {
        bot.sendMessage(CHAT_ID, "INFO: Tutup sudah dalam keadaan terbuka.", "");
      }
    }
    if (text == "/tutup") {
      if (currentLidState != STATE_CLOSED) {
        closeLid();
        currentLidState = STATE_CLOSED;
        sendLog("STATUS: Tutup ditutup paksa oleh " + from_name + ".");
      } else {
        bot.sendMessage(CHAT_ID, "INFO: Tutup sudah dalam keadaan tertutup.", "");
      }
    }
    if (text == "/foto") { takeAndSendPhoto(); }
    if (text == "/status") {
      String statusMsg = "STATUS TERKINI:\n";
      statusMsg += "Alamat IP: http://" + WiFi.localIP().toString() + "\n";
      statusMsg += "Koneksi MQTT: " + String(mqttClient.connected() ? "Terhubung" : "Terputus") + "\n";
      statusMsg += "Status Tutup: ";
      if (currentLidState == STATE_OPEN) statusMsg += "TERBUKA.";
      if (currentLidState == STATE_CLOSED) statusMsg += "TERTUTUP.";
      if (currentLidState == STATE_WAITING_TO_CLOSE) statusMsg += "MENUNGGU UNTUK MENUTUP.";
      bot.sendMessage(CHAT_ID, statusMsg, "");
    }
  }
}

// --- Sisa kode (logika servo, setup, loop) disesuaikan ---
bool readIr() { return (digitalRead(PIN_IR) == (IR_ACTIVE_LOW ? LOW : HIGH)); }

void manageLidState() {
  bool detected = readIr();
  switch (currentLidState) {
    case STATE_CLOSED: break;
    case STATE_OPEN:
      if (detected && !objectDetectedSinceOpen) {
        sendLog("EVENT: Objek terdeteksi pertama kali.");
        takeAndSendPhoto();
        objectDetectedSinceOpen = true;
      }
      if (!detected && objectDetectedSinceOpen) {
        sendLog("EVENT: Objek hilang, memulai timer 5 detik untuk menutup.");
        currentLidState = STATE_WAITING_TO_CLOSE;
        stateChangeTimestamp = millis();
      }
      break;
    case STATE_WAITING_TO_CLOSE:
      if (detected) {
        sendLog("EVENT: Objek terdeteksi lagi, timer penutupan direset.");
        stateChangeTimestamp = millis();
      } else {
        if (millis() - stateChangeTimestamp > CLOSE_DELAY_MS) {
          closeLid();
          currentLidState = STATE_CLOSED;
          sendLog("STATUS: Tutup ditutup secara otomatis.");
          // Publish disposal completion when lid closes after an object passed
          if (objectDetectedSinceOpen) {
            publishDisposalComplete("compatible");
            objectDetectedSinceOpen = false;
          }
        }
      }
      break;
  }
}

void setupMQTT() { mqttClient.setServer(MQTT_SERVER, MQTT_PORT); }

void maintainMQTTConnection() {
  if (mqttClient.connected()) return;
  unsigned long now = millis();
  static unsigned long lastAttempt = 0;
  if (now - lastAttempt < 5000) return;
  lastAttempt = now;
  String clientId = "ecotionbuddy-logger-";
  clientId += String((uint32_t)ESP.getEfuseMac(), HEX);
  if (mqttClient.connect(clientId.c_str(), MQTT_USER, MQTT_PASS)) {
    Serial.println("MQTT connected.");
    char ctrlTopic[64];
    snprintf(ctrlTopic, sizeof(ctrlTopic), "ecotionbuddy/ctrl/%s", DEVICE_ID);
    mqttClient.subscribe(ctrlTopic);
    Serial.print("MQTT subscribed: "); Serial.println(ctrlTopic);
  } else {
    Serial.print("MQTT connect failed, rc=");
    Serial.println(mqttClient.state());
  }
}

void setup() {
  WRITE_PERI_REG(RTC_CNTL_BROWN_OUT_REG, 0);
  Serial.begin(115200);
  Serial.setDebugOutput(true);
  Serial.println("\n\n=== EcotionBuddy (Kontrol Telegram v3) ===");
  pinMode(PIN_IR, INPUT_PULLUP);
  servoMotor.attach(PIN_SERVO, 500, 2500);
  closeLid();
  
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sccb_sda = SIOD_GPIO_NUM;
  config.pin_sccb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;
  config.frame_size = FRAMESIZE_SVGA;
  config.jpeg_quality = 12;
  config.fb_count = 2;
  config.grab_mode = CAMERA_GRAB_LATEST;
  config.fb_location = CAMERA_FB_IN_PSRAM;

  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Camera init failed with error 0x%x", err);
    return;
  }
  Serial.println("Camera initialized successfully.");
  
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  Serial.print("WiFi connecting");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected");
  
  // PERBAIKAN: Atur timeout untuk koneksi Telegram
  client.setInsecure(); // Gunakan jika Anda tidak perlu validasi sertifikat SSL
  client.setTimeout(10000); // Timeout 10 detik

  setupMQTT();
  mqttClient.setCallback(mqttCallback);
  startCameraServer();
  Serial.print("Camera Web Server Ready! Use 'http://");
  Serial.print(WiFi.localIP());
  Serial.println("' to connect");
  Serial.println("Sistem tempat sampah otomatis aktif.");
  
  // Pindahkan log startup setelah koneksi MQTT berhasil
  if (mqttClient.connect("startupClient")) {
     sendLog("Sistem EcotionBuddy telah online dan siap menerima perintah.");
     mqttClient.disconnect();
  }
}

void loop() {
  maintainMQTTConnection();
  mqttClient.loop();
  // Execute scheduled capture if any
  if (scheduledCaptureAt > 0 && (long)(millis() - scheduledCaptureAt) >= 0) {
    scheduledCaptureAt = 0;
    takeAndSendPhoto();
  }
  
  // PERBAIKAN: Gunakan bot.getUpdates() dengan cara yang lebih aman
  if (millis() > lastTimeBotChecked + botRequestDelay) {
    if (bot.getMe()) { // Cek koneksi ke Telegram dulu
      int numNewMessages = bot.getUpdates(bot.last_message_received + 1);
      while (numNewMessages) {
        handleNewMessages(numNewMessages);
        numNewMessages = bot.getUpdates(bot.last_message_received + 1);
      }
    } else {
      Serial.println("Gagal terhubung ke server Telegram.");
    }
    lastTimeBotChecked = millis();
  }
  
  manageLidState();
  delay(50);
}