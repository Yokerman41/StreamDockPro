package com.streamdock.pro

import com.google.gson.JsonParser
import com.streamdock.pro.core.GsonProvider
import com.streamdock.pro.data.models.Button
import com.streamdock.pro.data.models.Profile
import com.streamdock.pro.data.models.Workspace
import com.streamdock.pro.data.repository.StreamDockRepository
import com.streamdock.pro.network.dto.ActionTriggerPayload
import com.streamdock.pro.network.dto.MessageTypes
import com.streamdock.pro.network.dto.WsEnvelope
import com.streamdock.pro.ui.components.IconMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolCompatibilityTest {

    private val gson = GsonProvider.gson

    @Test
    fun `action payload serializes to snake_case expected by Rust ws_server`() {
        val payload = ActionTriggerPayload(
            clientUuid = "uuid-123",
            profileId = "prof-1",
            position = 3,
            actionType = "MEDIA_CONTROL",
            actionValue = "PLAY_PAUSE",
        )
        val json = gson.toJsonTree(payload).asJsonObject

        // ws_server.rs reads payload["action_type"] and payload["action_value"]
        assertEquals("MEDIA_CONTROL", json.get("action_type").asString)
        assertEquals("PLAY_PAUSE", json.get("action_value").asString)
        assertEquals("uuid-123", json.get("client_uuid").asString)
        assertEquals("prof-1", json.get("profile_id").asString)
        assertEquals(3, json.get("position").asInt)
    }

    @Test
    fun `rust register_ack default workspace deserializes cleanly`() {
        // Exact JSON structure produced by Workspace::new_default in pc-companion-rust
        val rustJson = """
            {
              "type": "register_ack",
              "uuid": "client-uuid-1",
              "timestamp": 1700000000000,
              "payload": {
                "client_uuid": "client-uuid-1",
                "version": 1,
                "active_profile_id": "prof-default-1",
                "grid_rows": 5,
                "grid_cols": 4,
                "profiles": [
                  {
                    "id": "prof-default-1",
                    "name": "Default Profile",
                    "auto_switch_process": null,
                    "is_default": true,
                    "icon_name": "home",
                    "background_color": "#1E1E2E",
                    "grid_rows": 5,
                    "grid_cols": 4
                  }
                ],
                "buttons": []
              }
            }
        """.trimIndent()

        val envelope = gson.fromJson(rustJson, WsEnvelope::class.java)
        assertEquals(MessageTypes.REGISTER_ACK, envelope.type)
        assertEquals("client-uuid-1", envelope.uuid)
        assertNotNull(envelope.payload)

        val ws = gson.fromJson(envelope.payload, Workspace::class.java)
        assertEquals("client-uuid-1", ws.clientUuid)
        assertEquals(1L, ws.version)
        assertEquals("prof-default-1", ws.activeProfileId)
        assertEquals(5, ws.gridRows)
        assertEquals(4, ws.gridCols)
        assertEquals(1, ws.profiles.size)
        assertEquals("Default Profile", ws.profiles[0].name)
        assertEquals("home", ws.profiles[0].iconName)
        assertTrue(ws.buttons.isEmpty())
    }

    @Test
    fun `android workspace serializes to snake_case compatible with Rust serde`() {
        val profile = Profile(
            id = "prof-1",
            name = "Streaming",
            isDefault = true,
            iconName = "stream",
            backgroundColor = "#1A1B26",
            gridRows = StreamDockRepository.DEFAULT_ROWS,
            gridCols = StreamDockRepository.DEFAULT_COLS,
        )
        val button = Button(
            id = "btn-prof-1-0",
            profileId = "prof-1",
            position = 0,
            label = "OBS",
            iconName = "tv",
            backgroundColor = "#3498DB",
            actionType = "OBS_SCENE_SWITCH",
            actionValue = "Escena Principal",
        )
        val ws = Workspace(
            clientUuid = "uuid-abc",
            version = 2L,
            activeProfileId = "prof-1",
            gridRows = 5,
            gridCols = 4,
            profiles = listOf(profile),
            buttons = listOf(button),
        )

        val jsonObj = JsonParser.parseString(gson.toJson(ws)).asJsonObject
        assertEquals("uuid-abc", jsonObj.get("client_uuid").asString)
        assertEquals("prof-1", jsonObj.get("active_profile_id").asString)
        assertEquals(5, jsonObj.get("grid_rows").asInt)
        assertEquals(4, jsonObj.get("grid_cols").asInt)

        val btnObj = jsonObj.getAsJsonArray("buttons").get(0).asJsonObject
        assertEquals("prof-1", btnObj.get("profile_id").asString)
        assertEquals("OBS_SCENE_SWITCH", btnObj.get("action_type").asString)
        assertEquals("Escena Principal", btnObj.get("action_value").asString)
    }

    @Test
    fun `icon mapper resolves server default icons and returns null for none`() {
        assertNull(IconMapper.getIcon("none"))
        assertNull(IconMapper.getIcon(null))
        assertNotNull(IconMapper.getIcon("home"))
        assertNotNull(IconMapper.getIcon("default"))
        assertNotNull(IconMapper.getIcon("play"))
        assertNotNull(IconMapper.getIcon("vol_up"))
        assertNotNull(IconMapper.getIcon("lock"))
        assertNotNull(IconMapper.getIcon("record"))
        assertNotNull(IconMapper.getIcon("keyboard"))
        assertTrue("none" !in IconMapper.getAllIconNames())

        // Inferencia cuando el servidor o WinUI envía "default"/"none" con una acción configurada
        assertEquals("play", IconMapper.inferIconNameForAction("MEDIA_CONTROL", "PLAY_PAUSE"))
        assertEquals("vol_up", IconMapper.inferIconNameForAction("MEDIA_CONTROL", "VOLUME_UP"))
        assertEquals("lock", IconMapper.inferIconNameForAction("SYSTEM_COMMAND", "LOCK_WORKSTATION"))
        assertEquals("record", IconMapper.inferIconNameForAction("OBS_SCENE_SWITCH", "START_RECORD"))
        assertEquals("launch", IconMapper.inferIconNameForAction("LAUNCH_APP", "C:\\Windows\\notepad.exe"))
        assertTrue(IconMapper.isBase64Icon("data:image/png;base64,iVBORw0KGgo="))
    }
}
