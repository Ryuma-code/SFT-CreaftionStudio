import asyncio
import json
import logging
from datetime import datetime

from asyncio_mqtt import Client, MqttError
from motor.motor_asyncio import AsyncIOMotorDatabase

logger = logging.getLogger("ecotionbuddy.mqtt")


class MQTTWorker:
    def __init__(self, db: AsyncIOMotorDatabase, host: str = "localhost", port: int = 1883,
                 topic: str = "ecotionbuddy/events/disposal_complete") -> None:
        self.db = db
        self.host = host
        self.port = port
        self.topic = topic
        self._stopped = asyncio.Event()

    def stop(self) -> None:
        self._stopped.set()

    async def run(self) -> None:
        """Run the MQTT consumer loop with basic reconnect logic."""
        while not self._stopped.is_set():
            try:
                logger.info("Connecting to MQTT %s:%s", self.host, self.port)
                async with Client(self.host, self.port) as client:
                    async with client.unfiltered_messages() as messages:
                        await client.subscribe(self.topic)
                        logger.info("Subscribed to topic: %s", self.topic)
                        async for message in messages:
                            if self._stopped.is_set():
                                break
                            try:
                                payload = message.payload.decode("utf-8")
                                await self._handle_message(payload)
                            except Exception as e:  # noqa: BLE001
                                logger.exception("Error handling MQTT message: %s", e)
            except MqttError as e:
                if self._stopped.is_set():
                    break
                logger.warning("MQTT error: %s. Reconnecting in 5s...", e)
                await asyncio.sleep(5)

    async def _handle_message(self, payload: str) -> None:
        try:
            data = json.loads(payload)
        except json.JSONDecodeError:
            logger.warning("Invalid JSON payload: %s", payload)
            return

        data["receivedAt"] = datetime.utcnow().isoformat()
        await self.db.events.insert_one(data)
        logger.info("Stored MQTT event: %s", data)

        # If this is a disposal completion event with a session, award points
        try:
            # Expected fields from ESP32 event publisher
            session_id = data.get("sessionId") or data.get("sid")
            label = data.get("label") or "unknown"
            bin_id = data.get("binId")
            if session_id:
                from bson import ObjectId  # lazy import to avoid top-level dep here
                sdoc = await self.db.sessions.find_one({"_id": ObjectId(session_id)})
                if sdoc:
                    user_id = sdoc.get("userId")
                    # Simple rule: if label == compatible award 50, else 0
                    points = 50 if label in ("compatible", "accepted", True, "true") else 0
                    claim = {
                        "userId": user_id,
                        "binId": bin_id or sdoc.get("binId"),
                        "sessionId": session_id,
                        "points": points,
                        "label": label,
                        "ts": datetime.utcnow(),
                        "status": "awarded" if points > 0 else "skipped",
                        "source": "disposal_complete",
                    }
                    await self.db.claims.insert_one(claim)
                    if points > 0 and user_id:
                        await self.db.users.update_one({"userId": user_id}, {"$inc": {"points": points}}, upsert=True)
                    # Mark session lastAction and optionally keep active for multi-throw
                    await self.db.sessions.update_one({"_id": sdoc["_id"]}, {"$set": {"lastActionAt": datetime.utcnow()}, "$inc": {"disposals": 1}})
                    logger.info("Awarded %s points to %s for session %s", points, user_id, session_id)
        except Exception as e:  # noqa: BLE001
            logger.exception("Failed to process award: %s", e)
