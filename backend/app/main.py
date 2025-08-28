import os
import asyncio
import contextlib
import logging
from datetime import datetime
from typing import Optional
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
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
