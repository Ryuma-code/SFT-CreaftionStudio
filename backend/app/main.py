import os
import asyncio
import contextlib
import logging
import shutil
import io
from datetime import datetime
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

        # Placeholder classification: accept all (label: compatible)
        label = "compatible"
        confidence = 0.99

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
            d["_id"] = str(d["_id"])  # make JSON serializable
        if isinstance(d.get("ts"), datetime):
            d["ts"] = d["ts"].isoformat()
    return {"items": docs, "count": len(docs)}


def _jsonify_id(doc: Dict[str, Any]) -> Dict[str, Any]:
    out = dict(doc)
    if "_id" in out:
        out["_id"] = str(out["_id"])  # type: ignore[assignment]
    if isinstance(out.get("ts"), datetime):
        out["ts"] = out["ts"].isoformat()
    return out


@app.get("/users/{userId}", tags=["users"])  # Simple points summary for app
async def get_user(userId: str):
    user = await app.state.db.users.find_one({"userId": userId})
    total_points = int(user.get("points", 0)) if user else 0
    claims_count = await app.state.db.claims.count_documents({"userId": userId})
    recent_claims: List[Dict[str, Any]] = []
    cursor = app.state.db.claims.find({"userId": userId}).sort("ts", -1).limit(10)
    for c in await cursor.to_list(length=10):
        recent_claims.append(_jsonify_id(c))
    return {"userId": userId, "points": total_points, "claimsCount": claims_count, "recentClaims": recent_claims}


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
