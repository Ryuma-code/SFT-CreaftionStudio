import os
import asyncio
import contextlib
import logging
from datetime import datetime
from typing import Optional, Any, Dict, List
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field
from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorDatabase

from .mqtt_worker import MQTTWorker

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


@asynccontextmanager
async def lifespan(app: FastAPI):
    mongo_uri = os.getenv("MONGO_URI", "mongodb://mongo:27017/ecotionbuddy")
    mqtt_host = os.getenv("MQTT_HOST", "mqtt")
    mqtt_port = int(os.getenv("MQTT_PORT", "1883"))

    logger.info("Connecting to Mongo: %s", mongo_uri)
    mongo_client = AsyncIOMotorClient(mongo_uri)
    db = _get_database(mongo_client, mongo_uri)
    app.state.db = db

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


@app.post("/iot/camera/upload", tags=["iot"])  # Content-Type: image/jpeg, raw body
async def iot_camera_upload(request: Request, deviceId: Optional[str] = None, binId: Optional[str] = None):
    try:
        data = await request.body()
        if not data:
            raise HTTPException(status_code=400, detail="Empty body")
        ts = datetime.utcnow()
        fname = ts.strftime("%Y%m%dT%H%M%S%f") + ".jpg"
        fpath = os.path.join(UPLOADS_DIR, fname)
        with open(fpath, "wb") as f:
            f.write(data)

        doc = {
            "deviceId": deviceId,
            "binId": binId,
            "filename": fname,
            "path": fpath,
            "url": f"/uploads/{fname}",
            "size": len(data),
            "contentType": "image/jpeg",
            "ts": ts,
            "origin": "iot",
        }
        await app.state.db.images.insert_one(doc)
        return {"status": "ok", "url": doc["url"], "size": doc["size"], "deviceId": deviceId, "binId": binId}
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
