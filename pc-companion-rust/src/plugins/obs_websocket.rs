use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use sha2::{Digest, Sha256};
use std::fs;
use std::path::PathBuf;
use std::sync::RwLock;
use std::time::Duration;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpStream;
use tokio::time::timeout;
use tracing::{info, warn};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ObsConfig {
    pub host: String,
    pub port: u16,
    pub password: String,
}

impl Default for ObsConfig {
    fn default() -> Self {
        Self {
            host: "127.0.0.1".to_string(),
            port: 4455,
            password: String::new(),
        }
    }
}

static OBS_CONFIG: RwLock<Option<ObsConfig>> = RwLock::new(None);

pub struct ObsWebsocketPlugin;

impl ObsWebsocketPlugin {
    fn settings_file_path() -> Option<PathBuf> {
        let local_app_data = std::env::var("LOCALAPPDATA").ok()?;
        let dir = PathBuf::from(local_app_data).join("StreamDockPro");
        let _ = fs::create_dir_all(&dir);
        Some(dir.join("obs_settings.json"))
    }

    fn load_from_disk() -> ObsConfig {
        if let Some(path) = Self::settings_file_path() {
            if let Ok(content) = fs::read_to_string(&path) {
                let clean = content.trim_start_matches('\u{feff}');
                if let Ok(cfg) = serde_json::from_str::<ObsConfig>(clean) {
                    if !cfg.password.is_empty() {
                        return cfg;
                    }
                }
            }
        }

        // Fallback: leer directamente la configuración de obs-websocket de OBS Studio si existe
        if let Ok(appdata) = std::env::var("APPDATA") {
            let obs_cfg_path = PathBuf::from(appdata)
                .join("obs-studio")
                .join("plugin_config")
                .join("obs-websocket")
                .join("config.json");
            if let Ok(content) = fs::read_to_string(obs_cfg_path) {
                if let Ok(val) = serde_json::from_str::<Value>(&content) {
                    let port = val
                        .get("server_port")
                        .and_then(|v| v.as_u64())
                        .map(|p| p as u16)
                        .unwrap_or(4455);
                    let password = val
                        .get("server_password")
                        .and_then(|v| v.as_str())
                        .unwrap_or("")
                        .to_string();
                    return ObsConfig {
                        host: "127.0.0.1".to_string(),
                        port,
                        password,
                    };
                }
            }
        }

        ObsConfig::default()
    }

    pub fn set_config(host: String, port: u16, password: String) {
        let cfg = ObsConfig {
            host,
            port,
            password,
        };
        if let Some(path) = Self::settings_file_path() {
            if let Ok(json_str) = serde_json::to_string_pretty(&cfg) {
                let _ = fs::write(path, json_str);
            }
        }
        if let Ok(mut guard) = OBS_CONFIG.write() {
            *guard = Some(cfg);
        }
    }

    pub fn get_config() -> ObsConfig {
        if let Ok(guard) = OBS_CONFIG.read() {
            if let Some(ref cfg) = *guard {
                return cfg.clone();
            }
        }
        let disk_cfg = Self::load_from_disk();
        if let Ok(mut guard) = OBS_CONFIG.write() {
            *guard = Some(disk_cfg.clone());
        }
        disk_cfg
    }

    pub async fn fetch_scenes_and_sources() -> Result<Vec<String>, String> {
        let cfg = Self::get_config();
        let mut stream = Self::connect_and_identify(&cfg).await?;

        let mut items = Vec::new();

        // 1. GetSceneList
        let scene_resp = Self::send_request(&mut stream, "GetSceneList", "req-scenes", None).await?;
        let mut scene_names = Vec::new();
        if let Some(scenes) = scene_resp
            .pointer("/responseData/scenes")
            .and_then(|v| v.as_array())
        {
            for s in scenes.iter().rev() {
                if let Some(name) = s.get("sceneName").and_then(|v| v.as_str()) {
                    scene_names.push(name.to_string());
                    items.push(name.to_string());
                }
            }
        }

        // 2. GetSceneItemList for each scene
        for scene_name in &scene_names {
            if let Ok(item_resp) = Self::send_request(
                &mut stream,
                "GetSceneItemList",
                "req-scene-items",
                Some(json!({ "sceneName": scene_name })),
            )
            .await
            {
                if let Some(scene_items) = item_resp
                    .pointer("/responseData/sceneItems")
                    .and_then(|v| v.as_array())
                {
                    for item in scene_items.iter().rev() {
                        if let Some(source_name) = item.get("sourceName").and_then(|v| v.as_str()) {
                            items.push(format!("SOURCE_TOGGLE:{scene_name}|{source_name}"));
                        }
                    }
                }
            }
        }

        // 3. Quick actions
        items.push("START_RECORD".to_string());
        items.push("START_STREAM".to_string());
        items.push("TOGGLE_MUTE".to_string());

        Ok(items)
    }

    pub async fn switch_scene(action_value: &str) -> Result<(), String> {
        let cfg = Self::get_config();
        info!(
            "OBS WebSocket v5 ({}:{}): Executing '{}'",
            cfg.host, cfg.port, action_value
        );

        let mut stream = Self::connect_and_identify(&cfg).await?;
        let val = action_value.trim();

        if val.eq_ignore_ascii_case("START_RECORD") || val.eq_ignore_ascii_case("TOGGLE_RECORD") {
            Self::send_request(&mut stream, "ToggleRecord", "req-rec", None).await?;
            return Ok(());
        }
        if val.eq_ignore_ascii_case("STOP_RECORD") {
            Self::send_request(&mut stream, "StopRecord", "req-stop-rec", None).await?;
            return Ok(());
        }
        if val.eq_ignore_ascii_case("START_STREAM") || val.eq_ignore_ascii_case("TOGGLE_STREAM") {
            Self::send_request(&mut stream, "ToggleStream", "req-stream", None).await?;
            return Ok(());
        }
        if val.eq_ignore_ascii_case("STOP_STREAM") {
            Self::send_request(&mut stream, "StopStream", "req-stop-stream", None).await?;
            return Ok(());
        }
        if let Some(input_name) = val.strip_prefix("MUTE_INPUT:") {
            Self::send_request(
                &mut stream,
                "ToggleInputMute",
                "req-mute",
                Some(json!({ "inputName": input_name.trim() })),
            )
            .await?;
            return Ok(());
        }
        if val.eq_ignore_ascii_case("TOGGLE_MUTE") {
            let inputs_resp =
                Self::send_request(&mut stream, "GetInputList", "req-inputs", None).await?;
            if let Some(inputs) = inputs_resp
                .pointer("/responseData/inputs")
                .and_then(|v| v.as_array())
            {
                let audio_input = inputs
                    .iter()
                    .find(|i| {
                        let kind = i
                            .get("unversionedInputKind")
                            .or_else(|| i.get("inputKind"))
                            .and_then(|v| v.as_str())
                            .unwrap_or("");
                        kind.contains("wasapi") || kind.contains("audio")
                    })
                    .or_else(|| inputs.first());

                if let Some(name) = audio_input
                    .and_then(|i| i.get("inputName"))
                    .and_then(|v| v.as_str())
                {
                    Self::send_request(
                        &mut stream,
                        "ToggleInputMute",
                        "req-mute-def",
                        Some(json!({ "inputName": name })),
                    )
                    .await?;
                }
            }
            return Ok(());
        }
        if let Some(rest) = val.strip_prefix("SOURCE_TOGGLE:") {
            if let Some((scene_name, source_name)) = rest.split_once('|') {
                let scene_name = scene_name.trim();
                let source_name = source_name.trim();
                let id_resp = Self::send_request(
                    &mut stream,
                    "GetSceneItemId",
                    "req-item-id",
                    Some(json!({ "sceneName": scene_name, "sourceName": source_name })),
                )
                .await?;
                if let Some(item_id) = id_resp
                    .pointer("/responseData/sceneItemId")
                    .and_then(|v| v.as_i64())
                {
                    let en_resp = Self::send_request(
                        &mut stream,
                        "GetSceneItemEnabled",
                        "req-item-en",
                        Some(json!({ "sceneName": scene_name, "sceneItemId": item_id })),
                    )
                    .await?;
                    let current_enabled = en_resp
                        .pointer("/responseData/sceneItemEnabled")
                        .and_then(|v| v.as_bool())
                        .unwrap_or(true);
                    Self::send_request(
                        &mut stream,
                        "SetSceneItemEnabled",
                        "req-set-en",
                        Some(json!({
                            "sceneName": scene_name,
                            "sceneItemId": item_id,
                            "sceneItemEnabled": !current_enabled
                        })),
                    )
                    .await?;
                }
            }
            return Ok(());
        }

        let target_scene = val.strip_prefix("SCENE:").unwrap_or(val).trim();
        Self::send_request(
            &mut stream,
            "SetCurrentProgramScene",
            "req-scene",
            Some(json!({ "sceneName": target_scene })),
        )
        .await?;

        Ok(())
    }

    async fn connect_and_identify(cfg: &ObsConfig) -> Result<TcpStream, String> {
        let addr = format!("{}:{}", cfg.host.trim(), cfg.port);
        let mut stream = timeout(Duration::from_secs(3), TcpStream::connect(&addr))
            .await
            .map_err(|_| format!("Timeout conectando a OBS en {addr}"))?
            .map_err(|e| format!("No se pudo conectar a OBS en {addr}: {e}"))?;

        let req = format!(
            "GET / HTTP/1.1\r\n\
             Host: {addr}\r\n\
             Upgrade: websocket\r\n\
             Connection: Upgrade\r\n\
             Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n\
             Sec-WebSocket-Version: 13\r\n\r\n"
        );
        stream
            .write_all(req.as_bytes())
            .await
            .map_err(|e| e.to_string())?;

        // Read HTTP response header until \r\n\r\n
        let mut header_buf = Vec::new();
        let mut byte = [0u8; 1];
        while !header_buf.ends_with(b"\r\n\r\n") {
            let n = timeout(Duration::from_secs(3), stream.read(&mut byte))
                .await
                .map_err(|_| "Timeout en handshake HTTP con OBS".to_string())?
                .map_err(|e| e.to_string())?;
            if n == 0 {
                return Err("OBS cerró la conexión durante el handshake".to_string());
            }
            header_buf.push(byte[0]);
        }

        // Read OpCode 0 (Hello)
        let hello_text = Self::read_ws_text_frame(&mut stream).await?;
        let hello_json: Value = serde_json::from_str(&hello_text).map_err(|e| e.to_string())?;

        let mut identify_d = json!({
            "rpcVersion": 1,
            "eventSubscriptions": 0
        });

        if let Some(auth_obj) = hello_json.pointer("/d/authentication") {
            let challenge = auth_obj
                .get("challenge")
                .and_then(|v| v.as_str())
                .unwrap_or("");
            let salt = auth_obj.get("salt").and_then(|v| v.as_str()).unwrap_or("");
            let auth_str = Self::compute_auth(&cfg.password, salt, challenge);
            identify_d["authentication"] = Value::String(auth_str);
        }

        let identify_msg = json!({
            "op": 1,
            "d": identify_d
        });
        Self::write_ws_text_frame(&mut stream, &identify_msg.to_string()).await?;

        let identified_text = Self::read_ws_text_frame(&mut stream).await?;
        let identified_json: Value =
            serde_json::from_str(&identified_text).map_err(|e| e.to_string())?;
        if identified_json.get("op").and_then(|v| v.as_i64()) != Some(2) {
            warn!("OBS Identify failed: {identified_text}");
            return Err("Autenticación rechazada por OBS WebSocket".to_string());
        }

        Ok(stream)
    }

    async fn send_request(
        stream: &mut TcpStream,
        request_type: &str,
        request_id: &str,
        request_data: Option<Value>,
    ) -> Result<Value, String> {
        let mut d = json!({
            "requestType": request_type,
            "requestId": request_id
        });
        if let Some(data) = request_data {
            d["requestData"] = data;
        }
        let payload = json!({
            "op": 6,
            "d": d
        });
        Self::write_ws_text_frame(stream, &payload.to_string()).await?;

        loop {
            let text = Self::read_ws_text_frame(stream).await?;
            let val: Value = serde_json::from_str(&text).map_err(|e| e.to_string())?;
            if val.get("op").and_then(|v| v.as_i64()) == Some(7)
                && val.pointer("/d/requestId").and_then(|v| v.as_str()) == Some(request_id)
            {
                return Ok(val.get("d").cloned().unwrap_or(Value::Null));
            }
        }
    }

    async fn write_ws_text_frame(stream: &mut TcpStream, text: &str) -> Result<(), String> {
        let bytes = text.as_bytes();
        let len = bytes.len();
        let mask_key: [u8; 4] = [0x37, 0xFA, 0x21, 0x3D];
        let mut frame = Vec::with_capacity(len + 14);
        frame.push(0x81); // FIN + Text frame

        if len <= 125 {
            frame.push(0x80 | (len as u8));
        } else if len <= 65535 {
            frame.push(0x80 | 126);
            frame.extend_from_slice(&(len as u16).to_be_bytes());
        } else {
            frame.push(0x80 | 127);
            frame.extend_from_slice(&(len as u64).to_be_bytes());
        }

        frame.extend_from_slice(&mask_key);
        for (i, &b) in bytes.iter().enumerate() {
            frame.push(b ^ mask_key[i % 4]);
        }

        stream
            .write_all(&frame)
            .await
            .map_err(|e| format!("Error enviando trama WS a OBS: {e}"))
    }

    async fn read_ws_text_frame(stream: &mut TcpStream) -> Result<String, String> {
        loop {
            let mut header = [0u8; 2];
            timeout(Duration::from_secs(3), stream.read_exact(&mut header))
                .await
                .map_err(|_| "Timeout leyendo respuesta de OBS".to_string())?
                .map_err(|e| format!("Conexión cerrada por OBS: {e}"))?;

            let opcode = header[0] & 0x0F;
            let masked = (header[1] & 0x80) != 0;
            let mut payload_len = (header[1] & 0x7F) as usize;

            if payload_len == 126 {
                let mut ext = [0u8; 2];
                stream
                    .read_exact(&mut ext)
                    .await
                    .map_err(|e| e.to_string())?;
                payload_len = u16::from_be_bytes(ext) as usize;
            } else if payload_len == 127 {
                let mut ext = [0u8; 8];
                stream
                    .read_exact(&mut ext)
                    .await
                    .map_err(|e| e.to_string())?;
                payload_len = u64::from_be_bytes(ext) as usize;
            }

            let mut mask_key = [0u8; 4];
            if masked {
                stream
                    .read_exact(&mut mask_key)
                    .await
                    .map_err(|e| e.to_string())?;
            }

            let mut payload = vec![0u8; payload_len];
            if payload_len > 0 {
                stream
                    .read_exact(&mut payload)
                    .await
                    .map_err(|e| e.to_string())?;
            }

            if masked {
                for (i, b) in payload.iter_mut().enumerate() {
                    *b ^= mask_key[i % 4];
                }
            }

            if opcode == 0x08 {
                return Err("OBS cerró la sesión WebSocket (Verifica la contraseña)".to_string());
            }
            if opcode == 0x01 {
                return String::from_utf8(payload).map_err(|e| e.to_string());
            }
        }
    }

    fn compute_auth(password: &str, salt: &str, challenge: &str) -> String {
        let mut h1 = Sha256::new();
        h1.update(password.as_bytes());
        h1.update(salt.as_bytes());
        let secret_b64 = Self::base64_encode(&h1.finalize());

        let mut h2 = Sha256::new();
        h2.update(secret_b64.as_bytes());
        h2.update(challenge.as_bytes());
        Self::base64_encode(&h2.finalize())
    }

    fn base64_encode(data: &[u8]) -> String {
        const TABLE: &[u8; 64] =
            b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        let mut out = String::with_capacity((data.len() + 2) / 3 * 4);
        for chunk in data.chunks(3) {
            let b0 = chunk[0] as u32;
            let b1 = if chunk.len() > 1 { chunk[1] as u32 } else { 0 };
            let b2 = if chunk.len() > 2 { chunk[2] as u32 } else { 0 };
            let triple = (b0 << 16) | (b1 << 8) | b2;

            out.push(TABLE[((triple >> 18) & 0x3F) as usize] as char);
            out.push(TABLE[((triple >> 12) & 0x3F) as usize] as char);
            if chunk.len() > 1 {
                out.push(TABLE[((triple >> 6) & 0x3F) as usize] as char);
            } else {
                out.push('=');
            }
            if chunk.len() > 2 {
                out.push(TABLE[(triple & 0x3F) as usize] as char);
            } else {
                out.push('=');
            }
        }
        out
    }
}
