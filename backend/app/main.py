import os
import asyncio
import contextlib
import logging
import shutil
import io
from datetime import datetime, timedelta
from typing import Optional, Any, Dict, List
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field
from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorDatabase, AsyncIOMotorGridFSBucket
from bson import ObjectId
from fastapi.responses import StreamingResponse

from .mqtt_worker import MQTTWorker
from .classifier import initialize_classifier, get_classifier
from asyncio_mqtt import Client as MQTTClient  # publish control commands

# Optional Telegram support
try:  # Lazy import: keep backend running if package missing
    from telegram import Bot  # type: ignore
    TELEGRAM_AVAILABLE = True
except Exception:  # pragma: no cover
    Bot = None  # type: ignore
    TELEGRAM_AVAILABLE = False

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ecotionbuddy.backend")


def _get_database(client: AsyncIOMotorClient, uri: str) -> AsyncIOMotorDatabase:
    # Try to use database from URI if provided, else fallback to env or default
    db_name_env: Optional[str] = os.getenv("MONGO_DB")
    if db_name_env:
        return client[db_name_env]
    try:
        db = client.get_default_database()
        if db is not None:
            return db
    except Exception:
        pass
    return client["ecotionbuddy"]


# -------- Optional integrations (Telegram, Network Share) --------
TELEGRAM_BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")
TELEGRAM_CHAT_ID = os.getenv("TELEGRAM_CHAT_ID")
# Master switch to disable Telegram even if token/chat are provided
TELEGRAM_ENABLED = os.getenv("TELEGRAM_ENABLED", "false").lower() == "true"
# Windows UNC path or mounted path inside container
# e.g. r"\\\\192.168.1.104\\Roblox" (double-escaped in env) or "/mnt/share/Roblox"
MIRROR_SHARE_PATH = os.getenv("MIRROR_SHARE_PATH") or os.getenv("TARGET_PC_FOLDER")

# Image storage backend: "disk" (default) or "gridfs"
IMAGE_STORAGE = os.getenv("IMAGE_STORAGE", "disk").lower()

# Model configuration
MODEL_PATH = os.getenv("MODEL_PATH", "/app/model")
MODEL_ENABLED = os.getenv("MODEL_ENABLED", "true").lower() == "true"

# Public endpoints/hosts for external clients (Android/ESP32) to discover
PUBLIC_API_BASE = os.getenv("PUBLIC_API_BASE")  # e.g. https://ecotionbuddy.ecotionbuddy.com/
PUBLIC_MQTT_HOST = os.getenv("PUBLIC_MQTT_HOST")  # e.g. 192.168.1.144
PUBLIC_MQTT_PORT = int(os.getenv("PUBLIC_MQTT_PORT", os.getenv("MQTT_PORT", "1883")))


async def _send_telegram_photo(local_path: str, caption: str) -> bool:
    if not (TELEGRAM_ENABLED and TELEGRAM_AVAILABLE and TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID):
        return False
    try:
        assert Bot is not None  # for type checkers
        bot = Bot(token=TELEGRAM_BOT_TOKEN)
        # Open file in a thread to avoid blocking loop
        def _open_bytes() -> bytes:
            with open(local_path, "rb") as f:
                return f.read()
        photo_bytes = await asyncio.to_thread(_open_bytes)
        await bot.send_photo(chat_id=TELEGRAM_CHAT_ID, photo=photo_bytes, caption=caption)
        logger.info("Telegram photo sent: %s", os.path.basename(local_path))
        return True
    except Exception as e:  # noqa: BLE001
        logger.exception("Failed to send Telegram photo: %s", e)
        return False


async def _mirror_to_share(local_path: str, filename: str) -> bool:
    if not MIRROR_SHARE_PATH:
        return False
    try:
        # Validate dest directory exists (inside host or container namespace)
        if not os.path.exists(MIRROR_SHARE_PATH):
            logger.warning("Mirror path not accessible: %s", MIRROR_SHARE_PATH)
            return False
        dest_path = os.path.join(MIRROR_SHARE_PATH, filename)
        await asyncio.to_thread(shutil.copy2, local_path, dest_path)
        logger.info("Mirrored file to share: %s", dest_path)
        return True
    except Exception as e:  # noqa: BLE001
        logger.exception("Failed to mirror to share: %s", e)
        return False


@asynccontextmanager
async def lifespan(app: FastAPI):
    mongo_uri = os.getenv("MONGO_URI", "mongodb://mongo:27017/ecotionbuddy")
    mqtt_host = os.getenv("MQTT_HOST", "mqtt")
    mqtt_port = int(os.getenv("MQTT_PORT", "1883"))

    logger.info("Connecting to Mongo: %s", mongo_uri)
    mongo_client = AsyncIOMotorClient(mongo_uri)
    db = _get_database(mongo_client, mongo_uri)
    app.state.db = db
    # Initialize GridFS bucket if requested
    if IMAGE_STORAGE == "gridfs":
        try:
            app.state.gridfs = AsyncIOMotorGridFSBucket(db)
            logger.info("GridFS storage enabled")
        except Exception as e:  # noqa: BLE001
            logger.exception("Failed to init GridFS bucket: %s", e)
            raise

    # expose for endpoints/publishers
    app.state.mqtt_host = mqtt_host
    app.state.mqtt_port = mqtt_port

    worker = MQTTWorker(db=db, host=mqtt_host, port=mqtt_port)
    app.state.mqtt_worker = worker
    mqtt_task = asyncio.create_task(worker.run(), name="mqtt_worker")

    # Initialize ML model if enabled
    if MODEL_ENABLED:
        logger.info(f"Initializing classifier from {MODEL_PATH}")
        model_success = initialize_classifier(MODEL_PATH)
        if model_success:
            logger.info("Classifier initialized successfully")
        else:
            logger.warning("Classifier initialization failed - using placeholder labels")
    else:
        logger.info("Model disabled - using placeholder labels")

    try:
        yield
    finally:
        worker.stop()
        mqtt_task.cancel()
        with contextlib.suppress(asyncio.CancelledError):
            await mqtt_task
        mongo_client.close()
        logger.info("Backend shutdown complete")


app = FastAPI(title="EcotionBuddy Backend", version="0.1.0", lifespan=lifespan)

# CORS for Android app and IoT devices
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Static uploads mount
UPLOADS_DIR = os.getenv("UPLOADS_DIR", "uploads")
os.makedirs(UPLOADS_DIR, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=UPLOADS_DIR), name="uploads")


class ClaimRequest(BaseModel):
    userId: str
    binId: str


@app.get("/", tags=["meta"]) 
async def root():
    return {"name": "EcotionBuddy Backend", "status": "ok", "time": datetime.utcnow().isoformat()}


@app.get("/health", tags=["meta"]) 
async def health():
    return {"status": "ok", "time": datetime.utcnow().isoformat()}


@app.post("/claim")
async def claim(req: ClaimRequest):
    # Simplified claim logic for dev/testing purposes
    points = 10
    doc = {
        "userId": req.userId,
        "binId": req.binId,
        "points": points,
        "status": "awarded",
        "ts": datetime.utcnow(),
    }
    await app.state.db.claims.insert_one(doc)
    return {"awardedPoints": points, "status": "ok"}


# ===== IoT Camera and Android integration =====

class IoTCameraResult(BaseModel):
    deviceId: Optional[str] = Field(default=None)
    binId: Optional[str] = Field(default=None)
    label: str
    confidence: float
    imageUrl: Optional[str] = Field(default=None)
    extra: Optional[Dict[str, Any]] = Field(default=None)


class AndroidEvent(BaseModel):
    userId: str
    action: str
    binId: Optional[str] = Field(default=None)
    payload: Optional[Dict[str, Any]] = Field(default=None)


# ===== Helper: MQTT publish (ad-hoc connection) =====
async def mqtt_publish(app: FastAPI, topic: str, payload: Dict[str, Any]) -> None:
    host: str = getattr(app.state, "mqtt_host", "mqtt")
    port: int = getattr(app.state, "mqtt_port", 1883)
    try:
        async with MQTTClient(host, port) as client:
            import json as _json
            await client.publish(topic, _json.dumps(payload))
            logger.info("Published MQTT to %s: %s", topic, payload)
    except Exception as e:  # noqa: BLE001
        logger.exception("Failed to publish MQTT to %s: %s", topic, e)


@app.post("/iot/camera/upload", tags=["iot"])  # Content-Type: image/jpeg, raw body
async def iot_camera_upload(request: Request, deviceId: Optional[str] = None, binId: Optional[str] = None, sid: Optional[str] = None):
    try:
        data = await request.body()
        if not data:
            raise HTTPException(status_code=400, detail="Empty body")
        ts = datetime.utcnow()
        fname = ts.strftime("%Y%m%dT%H%M%S%f") + ".jpg"
        fpath = os.path.join(UPLOADS_DIR, fname)
        with open(fpath, "wb") as f:
            f.write(data)
        # Decide storage URL and optionally upload to GridFS
        url = f"/uploads/{fname}"
        gridfs_id: Optional[ObjectId] = None
        if IMAGE_STORAGE == "gridfs":
            try:
                gridfs_id = await app.state.gridfs.upload_from_stream(
                    fname,
                    io.BytesIO(data),
                    metadata={
                        "deviceId": deviceId,
                        "binId": binId,
                        "contentType": "image/jpeg",
                        "ts": ts,
                        "origin": "iot",
                    },
                )
                url = f"/images/{str(gridfs_id)}"
                logger.info("Stored image in GridFS: %s", gridfs_id)
            except Exception as e:  # noqa: BLE001
                logger.exception("GridFS upload failed, falling back to disk url: %s", e)

        doc = {
            "deviceId": deviceId,
            "binId": binId,
            "filename": fname,
            "path": fpath,
            "url": url,
            "size": len(data),
            "contentType": "image/jpeg",
            "ts": ts,
            "origin": "iot",
            "storage": IMAGE_STORAGE,
        }
        if gridfs_id is not None:
            doc["gridfsId"] = str(gridfs_id)
        if sid:
            doc["sessionId"] = sid
        insert_res = await app.state.db.images.insert_one(doc)
        image_id = insert_res.inserted_id

        # Try to attach to active session by binId if no sid provided
        active_session: Optional[Dict[str, Any]] = None
        if not sid and binId:
            active_session = await app.state.db.sessions.find_one({
                "binId": binId,
                "status": "active",
            })
            if active_session:
                sid = str(active_session.get("_id"))
                doc["sessionId"] = sid
                await app.state.db.images.update_one({"_id": image_id}, {"$set": {"sessionId": sid}})

        # ML model classification
        classifier = get_classifier()
        if classifier and MODEL_ENABLED:
            try:
                label, confidence = classifier.predict(data)
                logger.info(f"Model prediction: {label} (confidence: {confidence:.3f})")
            except Exception as e:
                logger.exception(f"Model inference failed: {e}")
                label = "unknown"
                confidence = 0.0
        else:
            # Fallback to placeholder
            label = "placeholder"
            confidence = 0.099

        # If session exists and classified compatible, command device to open
        try:
            if binId:
                # resolve deviceId mapping if missing
                if not deviceId:
                    dev_map = await app.state.db.devices.find_one({"binId": binId})
                    if dev_map:
                        deviceId = dev_map.get("deviceId")
                target_device = deviceId or "esp32cam-1"
                topic = f"ecotionbuddy/ctrl/{target_device}"
                payload = {"action": "open", "angle": 180, "reason": "classification", "binId": binId}
                if sid:
                    payload["sessionId"] = sid
                await mqtt_publish(app, topic, payload)
        except Exception:  # noqa: BLE001
            logger.exception("Failed to publish open command")
        # Schedule best-effort side effects (non-blocking)
        caption = f"Deteksi baru pada {ts.isoformat()}"
        try:
            asyncio.create_task(_send_telegram_photo(fpath, caption))
        except Exception:  # noqa: BLE001
            logger.exception("Failed to schedule telegram task")
        try:
            asyncio.create_task(_mirror_to_share(fpath, fname))
        except Exception:  # noqa: BLE001
            logger.exception("Failed to schedule mirror task")

        return {
            "status": "ok",
            "url": doc["url"],
            "size": doc["size"],
            "deviceId": deviceId,
            "binId": binId,
            "sessionId": sid,
            "sideEffects": {
                "telegramScheduled": bool(TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID and TELEGRAM_AVAILABLE),
                "mirrorScheduled": bool(MIRROR_SHARE_PATH),
            },
            "storage": IMAGE_STORAGE,
            "label": label,
            "confidence": confidence,
        }
    except HTTPException:
        raise
    except Exception as e:  # noqa: BLE001
        logging.exception("Upload failed: %s", e)
        raise HTTPException(status_code=500, detail="Upload failed")


@app.post("/iot/camera/result", tags=["iot"])  # JSON event from ESP32-CAM (placeholder or real)
async def iot_camera_result(evt: IoTCameraResult):
    doc = evt.model_dump()
    doc["origin"] = "iot"
    doc["ts"] = datetime.utcnow()
    await app.state.db.events.insert_one(doc)
    return {"status": "ok"}


# ===== QR-driven sessions =====
class StartSessionRequest(BaseModel):
    userId: str
    binId: str
    countdownMs: int = 3000
    deviceId: Optional[str] = None


@app.post("/session/start", tags=["session"])
async def start_session(req: StartSessionRequest):
    ts = datetime.utcnow()
    # resolve deviceId mapping
    device_id = req.deviceId
    if not device_id:
        dev_map = await app.state.db.devices.find_one({"binId": req.binId})
        if dev_map:
            device_id = dev_map.get("deviceId")
    if not device_id:
        device_id = "esp32cam-1"

    # create session
    session = {
        "userId": req.userId,
        "binId": req.binId,
        "deviceId": device_id,
        "status": "active",
        "startedAt": ts,
        "lastActionAt": ts,
    }
    result = await app.state.db.sessions.insert_one(session)
    sid = str(result.inserted_id)

    # publish activation to device
    topic = f"ecotionbuddy/ctrl/{device_id}"
    payload = {"action": "activate", "sessionId": sid, "binId": req.binId, "countdownMs": req.countdownMs}
    await mqtt_publish(app, topic, payload)

    return {"status": "ok", "sessionId": sid, "deviceId": device_id}


class EndSessionRequest(BaseModel):
    sessionId: str
    reason: Optional[str] = None


@app.post("/session/end", tags=["session"])
async def end_session(req: EndSessionRequest):
    sid = req.sessionId
    sdoc = await app.state.db.sessions.find_one({"_id": ObjectId(sid)})
    if not sdoc:
        raise HTTPException(status_code=404, detail="Session not found")
    await app.state.db.sessions.update_one({"_id": ObjectId(sid)}, {"$set": {"status": "ended", "endedAt": datetime.utcnow(), "reason": req.reason or "client_end"}})
    # Optionally notify device
    try:
        device_id = sdoc.get("deviceId") or "esp32cam-1"
        await mqtt_publish(app, f"ecotionbuddy/ctrl/{device_id}", {"action": "deactivate", "sessionId": sid})
    except Exception:
        logger.exception("Failed to publish deactivate")
    return {"status": "ok"}


@app.post("/events", tags=["android"])  # Generic Android event
async def post_event(evt: AndroidEvent):
    doc = evt.model_dump()
    doc["origin"] = "android"
    doc["ts"] = datetime.utcnow()
    await app.state.db.events.insert_one(doc)
    
    # Award points for scan events
    if evt.action == "scan" and evt.payload and "points" in evt.payload:
        points_to_add = evt.payload["points"]
        await app.state.db.users.update_one(
            {"userId": evt.userId},
            {"$inc": {"points": points_to_add}},
            upsert=True
        )
    
    return {"status": "ok"}


@app.get("/events/latest", tags=["events"])  # Fetch recent events for app UI
async def get_latest_events(limit: int = 50):
    if limit <= 0:
        limit = 1
    if limit > 200:
        limit = 200
    cursor = app.state.db.events.find().sort("ts", -1).limit(limit)
    docs: List[Dict[str, Any]] = await cursor.to_list(length=limit)
    for d in docs:
        if "_id" in d:
            d["_id"] = str(d["_id"])
    return {"events": docs}


class UserRegistrationRequest(BaseModel):
    userId: str
    name: str
    email: str

@app.post("/users/register", tags=["users"])
async def register_user(req: UserRegistrationRequest):
    # Check if user already exists
    existing_user = await app.state.db.users.find_one({"userId": req.userId})
    if existing_user:
        raise HTTPException(status_code=400, detail="User ID already exists")
    
    # Check if email already exists
    existing_email = await app.state.db.users.find_one({"email": req.email})
    if existing_email:
        raise HTTPException(status_code=400, detail="Email already registered")
    
    # Create new user
    new_user = {
        "userId": req.userId,
        "name": req.name,
        "email": req.email,
        "points": 0,
        "level": 1,
        "claimsCount": 0,
        "completedMissions": [],
        "activeMissions": [],
        "createdAt": datetime.utcnow(),
        "lastActive": datetime.utcnow()
    }
    
    result = await app.state.db.users.insert_one(new_user)
    new_user["_id"] = str(result.inserted_id)
    
    return {
        "status": "success",
        "message": "User registered successfully",
        "user": {
            "userId": new_user["userId"],
            "name": new_user["name"],
            "email": new_user["email"],
            "points": new_user["points"],
            "level": new_user["level"]
        }
    }

@app.get("/users/{user_id}", tags=["users"])
async def get_user(user_id: str):
    user_doc = await app.state.db.users.find_one({"userId": user_id})
    if not user_doc:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Update last active
    await app.state.db.users.update_one(
        {"userId": user_id}, 
        {"$set": {"lastActive": datetime.utcnow()}}
    )
    
    # Get recent claims
    recent_claims = await app.state.db.events.find(
        {"userId": user_id, "eventType": "claim"}
    ).sort("ts", -1).limit(5).to_list(length=5)
    
    return {
        "userId": user_doc["userId"],
        "name": user_doc.get("name", ""),
        "email": user_doc.get("email", ""),
        "points": user_doc.get("points", 0),
        "level": user_doc.get("level", 1),
        "claimsCount": user_doc.get("claimsCount", 0),
        "completedMissions": user_doc.get("completedMissions", []),
        "activeMissions": user_doc.get("activeMissions", []),
        "recentClaims": [_jsonify_id(claim) for claim in recent_claims]
    }


@app.get("/users/{user_id}/history", tags=["users"])
async def get_user_history(user_id: str, limit: int = 50):
    """Get user's activity history including scans, missions, and achievements"""
    if limit <= 0:
        limit = 1
    if limit > 200:
        limit = 200
    
    # Get user's events and sessions
    user_events = []
    
    # Get scan events
    scan_cursor = app.state.db.events.find({
        "userId": user_id,
        "eventType": {"$in": ["scan", "classification"]}
    }).sort("ts", -1).limit(limit)
    scan_docs = await scan_cursor.to_list(length=limit)
    
    for doc in scan_docs:
        user_events.append({
            "id": str(doc["_id"]),
            "type": "waste_scanned",
            "title": f"Scanned {doc.get('category', 'Unknown')} Waste",
            "description": f"Classification: {doc.get('label', 'Unknown')} ({doc.get('confidence', 0):.1%} confidence)",
            "pointsEarned": doc.get("points", 50),
            "timestamp": int(doc["ts"].timestamp() * 1000),
            "category": doc.get("category", "unknown").lower()
        })
    
    # Get session events (mission completions)
    session_cursor = app.state.db.sessions.find({
        "userId": user_id,
        "status": "ended"
    }).sort("createdAt", -1).limit(limit)
    session_docs = await session_cursor.to_list(length=limit)
    
    for doc in session_docs:
        user_events.append({
            "id": str(doc["_id"]),
            "type": "mission_completed",
            "title": f"Completed Disposal Session",
            "description": f"Successfully disposed waste at bin {doc.get('binId', 'Unknown')}",
            "pointsEarned": doc.get("points", 100),
            "timestamp": int(doc["createdAt"].timestamp() * 1000),
            "category": "general"
        })
    
    # Sort by timestamp and limit
    user_events.sort(key=lambda x: x["timestamp"], reverse=True)
    user_events = user_events[:limit]
    
    return {"history": user_events}


# Mission System
class Mission(BaseModel):
    id: str
    title: str
    description: str
    type: str  # "scan", "dispose", "daily", "weekly"
    target: int
    reward_points: int
    duration_days: int
    requirements: Dict[str, Any] = {}

@app.get("/missions", tags=["missions"])
async def get_available_missions():
    """Get all available missions"""
    missions = [
        {
            "id": "daily_scan_5",
            "title": "Scan 5 Items Today",
            "description": "Scan 5 different waste items to learn about recycling",
            "type": "scan",
            "target": 5,
            "reward_points": 100,
            "duration_days": 1,
            "requirements": {"scan_count": 5}
        },
        {
            "id": "weekly_plastic_10",
            "title": "Plastic Warrior",
            "description": "Scan 10 plastic items this week",
            "type": "scan",
            "target": 10,
            "reward_points": 500,
            "duration_days": 7,
            "requirements": {"category": "plastic", "scan_count": 10}
        },
        {
            "id": "dispose_session_3",
            "title": "Disposal Champion",
            "description": "Complete 3 disposal sessions",
            "type": "dispose",
            "target": 3,
            "reward_points": 300,
            "duration_days": 7,
            "requirements": {"session_count": 3}
        },
        {
            "id": "eco_explorer",
            "title": "Eco Explorer",
            "description": "Scan items from 3 different categories",
            "type": "scan",
            "target": 3,
            "reward_points": 200,
            "duration_days": 3,
            "requirements": {"unique_categories": 3}
        }
    ]
    return {"missions": missions}

@app.post("/users/{user_id}/missions/{mission_id}/start", tags=["missions"])
async def start_mission(user_id: str, mission_id: str):
    """Start a mission for a user"""
    user_doc = await app.state.db.users.find_one({"userId": user_id})
    if not user_doc:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Check if mission is already active
    active_missions = user_doc.get("activeMissions", [])
    if any(m["id"] == mission_id for m in active_missions):
        raise HTTPException(status_code=400, detail="Mission already active")
    
    # Get mission details
    missions_response = await get_available_missions()
    mission = next((m for m in missions_response["missions"] if m["id"] == mission_id), None)
    if not mission:
        raise HTTPException(status_code=404, detail="Mission not found")
    
    # Add mission to user's active missions
    new_mission = {
        **mission,
        "started_at": datetime.utcnow(),
        "expires_at": datetime.utcnow() + timedelta(days=mission["duration_days"]),
        "progress": 0
    }
    
    await app.state.db.users.update_one(
        {"userId": user_id},
        {"$push": {"activeMissions": new_mission}}
    )
    
    return {"status": "success", "message": "Mission started", "mission": new_mission}

@app.get("/users/{user_id}/missions", tags=["missions"])
async def get_user_missions(user_id: str):
    """Get user's active and completed missions"""
    user_doc = await app.state.db.users.find_one({"userId": user_id})
    if not user_doc:
        raise HTTPException(status_code=404, detail="User not found")
    
    active_missions = user_doc.get("activeMissions", [])
    completed_missions = user_doc.get("completedMissions", [])
    
    # Check for expired missions
    current_time = datetime.utcnow()
    expired_missions = []
    still_active = []
    
    for mission in active_missions:
        expires_at = mission.get("expires_at")
        if isinstance(expires_at, datetime) and expires_at < current_time:
            expired_missions.append(mission)
        else:
            still_active.append(mission)
    
    # Remove expired missions
    if expired_missions:
        await app.state.db.users.update_one(
            {"userId": user_id},
            {"$set": {"activeMissions": still_active}}
        )
    
    return {
        "active_missions": still_active,
        "completed_missions": completed_missions,
        "expired_missions": expired_missions
    }

@app.post("/users/{user_id}/missions/check_progress", tags=["missions"])
async def check_mission_progress(user_id: str):
    """Check and update mission progress based on user activities"""
    user_doc = await app.state.db.users.find_one({"userId": user_id})
    if not user_doc:
        raise HTTPException(status_code=404, detail="User not found")
    
    active_missions = user_doc.get("activeMissions", [])
    if not active_missions:
        return {"message": "No active missions"}
    
    completed_missions = []
    updated_missions = []
    
    for mission in active_missions:
        mission_type = mission.get("type")
        requirements = mission.get("requirements", {})
        
        if mission_type == "scan":
            # Count scans based on requirements
            query = {"userId": user_id, "eventType": "scan"}
            
            if "category" in requirements:
                query["category"] = requirements["category"]
            
            # Count scans since mission started
            started_at = mission.get("started_at", datetime.utcnow())
            query["ts"] = {"$gte": started_at}
            
            if "unique_categories" in requirements:
                # Count unique categories
                pipeline = [
                    {"$match": query},
                    {"$group": {"_id": "$category"}},
                    {"$count": "unique_count"}
                ]
                result = await app.state.db.events.aggregate(pipeline).to_list(length=1)
                progress = result[0]["unique_count"] if result else 0
            else:
                progress = await app.state.db.events.count_documents(query)
            
        elif mission_type == "dispose":
            # Count disposal sessions
            query = {
                "userId": user_id,
                "status": "ended"
            }
            started_at = mission.get("started_at", datetime.utcnow())
            query["createdAt"] = {"$gte": started_at}
            
            progress = await app.state.db.sessions.count_documents(query)
        
        else:
            progress = 0
        
        mission["progress"] = progress
        
        # Check if mission is completed
        if progress >= mission.get("target", 1):
            mission["completed_at"] = datetime.utcnow()
            completed_missions.append(mission)
            
            # Award points
            points_to_add = mission.get("reward_points", 0)
            await app.state.db.users.update_one(
                {"userId": user_id},
                {"$inc": {"points": points_to_add}}
            )
        else:
            updated_missions.append(mission)
    
    # Update user missions
    all_completed = user_doc.get("completedMissions", []) + completed_missions
    await app.state.db.users.update_one(
        {"userId": user_id},
        {
            "$set": {
                "activeMissions": updated_missions,
                "completedMissions": all_completed
            }
        }
    )
    
    return {
        "completed_missions": completed_missions,
        "updated_missions": updated_missions,
        "points_earned": sum(m.get("reward_points", 0) for m in completed_missions)
    }


@app.get("/config", tags=["meta"])  # Public config for mobile/ESP32 discovery
async def get_public_config(request: Request):
    # Derive API base from forwarded host if available, else from env or request
    host = request.headers.get("x-forwarded-host") or request.headers.get("host")
    scheme = request.headers.get("x-forwarded-proto") or request.url.scheme
    api_base = PUBLIC_API_BASE or (f"{scheme}://{host}/" if host else str(request.base_url))
    
    # Include model status
    classifier = get_classifier()
    model_status = {
        "enabled": MODEL_ENABLED,
        "loaded": classifier is not None,
        "classes": classifier.get_class_names() if classifier else []
    }
    
    return {
        "apiBase": api_base,
        "mqttHost": PUBLIC_MQTT_HOST or getattr(app.state, "mqtt_host", "mqtt"),
        "mqttPort": PUBLIC_MQTT_PORT,
        "model": model_status,
    }


@app.post("/classify", tags=["ml"])  # Test classification endpoint for Android app
async def classify_image(request: Request):
    """Test endpoint for image classification without IoT workflow"""
    try:
        data = await request.body()
        if not data:
            raise HTTPException(status_code=400, detail="Empty body")
        
        # Get classifier and predict
        classifier = get_classifier()
        if not classifier or not MODEL_ENABLED:
            raise HTTPException(status_code=503, detail="Model not available")
        
        label, confidence = classifier.predict(data)
        
        return {
            "status": "ok",
            "prediction": {
                "label": label,
                "confidence": confidence,
                "classes": classifier.get_class_names()
            },
            "model": "MobileNetV2"
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.exception(f"Classification failed: {e}")
        raise HTTPException(status_code=500, detail="Classification failed")


def _jsonify_id(doc: Dict[str, Any]) -> Dict[str, Any]:
    out = dict(doc)
    if "_id" in out:
        out["_id"] = str(out["_id"])  # type: ignore[assignment]
    if isinstance(out.get("ts"), datetime):
        out["ts"] = out["ts"].isoformat()
    return out


# Removed duplicate user endpoint - using the comprehensive one at line 500


@app.get("/session/{sessionId}", tags=["session"])  # Session details for app
async def get_session(sessionId: str):
    try:
        oid = ObjectId(sessionId)
    except Exception:  # noqa: BLE001
        raise HTTPException(status_code=400, detail="Invalid session id")
    sdoc = await app.state.db.sessions.find_one({"_id": oid})
    if not sdoc:
        raise HTTPException(status_code=404, detail="Session not found")
    # counts
    images_count = await app.state.db.images.count_documents({"sessionId": sessionId})
    events_count = await app.state.db.events.count_documents({"sessionId": sessionId})
    out = _jsonify_id(sdoc)
    out["imagesCount"] = images_count
    out["eventsCount"] = events_count
    return out


@app.get("/images/{file_id}", tags=["images"])  # Stream image from GridFS
async def get_image(file_id: str):
    if IMAGE_STORAGE != "gridfs":
        raise HTTPException(status_code=404, detail="GridFS storage not enabled")
    try:
        oid = ObjectId(file_id)
    except Exception:  # noqa: BLE001
        raise HTTPException(status_code=400, detail="Invalid image id")
    try:
        buf = io.BytesIO()
        await app.state.gridfs.download_to_stream(oid, buf)
        buf.seek(0)
        return StreamingResponse(buf, media_type="image/jpeg")
    except Exception as e:  # noqa: BLE001
        logger.exception("GridFS read failed: %s", e)
        raise HTTPException(status_code=404, detail="Image not found")
