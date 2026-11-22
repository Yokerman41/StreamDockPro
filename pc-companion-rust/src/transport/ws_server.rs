use axum::{
    extract::ws::{Message, WebSocket, WebSocketUpgrade},
    response::IntoResponse,
    routing::get,
    Router,
};
use futures_util::{SinkExt, StreamExt};
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tokio::sync::broadcast;
use tracing::{error, info, warn};

use crate::core::context_manager::ContextManager;
use crate::engines::action_engine::ActionEngine;
use crate::engines::icon_manager::IconManager;
use crate::engines::workspace_engine::Button;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WsEnvelope {
    pub r#type: String,
    pub uuid: Option<String>,
    pub timestamp: Option<u64>,
    pub payload: Option<serde_json::Value>,
}

pub struct ServerState {
    pub context_manager: ContextManager,
    pub broadcast_tx: broadcast::Sender<WsEnvelope>,
}

pub fn create_router(state: Arc<ServerState>) -> Router {
    Router::new()
        .route("/ws", get(ws_handler))
        .with_state(state)
}

async fn ws_handler(
    ws: WebSocketUpgrade,
    axum::extract::State(state): axum::extract::State<Arc<ServerState>>,
) -> impl IntoResponse {
    ws.on_upgrade(|socket| handle_socket(socket, state))
}

async fn handle_socket(socket: WebSocket, state: Arc<ServerState>) {
    let (mut sender, mut receiver) = socket.split();
    let mut rx = state.broadcast_tx.subscribe();
    info!("New WebSocket connection established");

    let send_task = tokio::spawn(async move {
        while let Ok(msg) = rx.recv().await {
            if let Ok(json_str) = serde_json::to_string(&msg) {
                if sender.send(Message::Text(json_str)).await.is_err() {
                    break;
                }
            }
        }
    });

    while let Some(Ok(msg)) = receiver.next().await {
        if let Message::Text(text) = msg {
            info!("Received WebSocket message: {text}");
            match serde_json::from_str::<WsEnvelope>(&text) {
                Ok(envelope) => {
                match envelope.r#type.as_str() {
                    "get_workspace" => {
                        let target_uuid = envelope.uuid.clone().unwrap_or_default();
                        if let Some(ctx_lock) = state.context_manager.get(&target_uuid) {
                            let ctx = ctx_lock.read().await;
                            let reply = WsEnvelope {
                                r#type: "db_sync".to_string(),
                                uuid: Some(target_uuid),
                                timestamp: Some(chrono::Utc::now().timestamp_millis() as u64),
                                payload: Some(serde_json::json!({
                                    "workspace": ctx.workspace
                                })),
                            };
                            let _ = state.broadcast_tx.send(reply);
                        }
                    }
                    "register_client" => {
                        let uuid = envelope.uuid.clone().unwrap_or_else(|| uuid::Uuid::new_v4().to_string());
                        let display_name = envelope.payload.as_ref()
                            .and_then(|p| p.get("display_name"))
                            .and_then(|v| v.as_str())
                            .unwrap_or("Android Client")
                            .to_string();
                        info!("Registering client with UUID: {uuid} ({display_name})");
                        let ctx_lock = state.context_manager.get_or_create(uuid.clone(), display_name.clone());
                        {
                            let mut ctx = ctx_lock.write().await;
                            ctx.connection_status = crate::core::client_context::ConnectionStatus::Online;
                            if !display_name.is_empty() {
                                ctx.metadata.display_name = display_name;
                            }
                        }
                        
                        let ack = WsEnvelope {
                            r#type: "register_ack".to_string(),
                            uuid: Some(uuid),
                            timestamp: Some(chrono::Utc::now().timestamp_millis() as u64),
                            payload: Some(serde_json::to_value(&ctx_lock.read().await.workspace).unwrap_or_default()),
                        };
                        let _ = state.broadcast_tx.send(ack);

                        let clients = state.context_manager.list_clients().await;
                        let list_msg = WsEnvelope {
                            r#type: "client_list".to_string(),
                            uuid: None,
                            timestamp: Some(chrono::Utc::now().timestamp_millis() as u64),
                            payload: Some(serde_json::to_value(&clients).unwrap_or_default()),
                        };
                        let _ = state.broadcast_tx.send(list_msg);
                    }
                    "get_client_list" => {
                        let clients = state.context_manager.list_clients().await;
                        let list_msg = WsEnvelope {
                            r#type: "client_list".to_string(),
                            uuid: None,
                            timestamp: Some(chrono::Utc::now().timestamp_millis() as u64),
                            payload: Some(serde_json::to_value(&clients).unwrap_or_default()),
                        };
                        let _ = state.broadcast_tx.send(list_msg);
                    }
                    "db_sync" => {
                        let target_uuid = envelope.uuid.clone().unwrap_or_default();
                        info!("db_sync received for target ClientUUID: '{target_uuid}'");

                        if let Some(ref payload) = envelope.payload {
                            // Caso 1: Sincronización de Workspace completo
                            if let Some(ws_json) = payload.get("workspace") {
                                match serde_json::from_value::<crate::engines::workspace_engine::Workspace>(ws_json.clone()) {
                                    Ok(mut new_ws) => {
                                        for btn in &mut new_ws.buttons {
                                            IconManager::enrich_button_icon(btn);
                                        }
                                        let ctx_lock = state.context_manager.get_or_create(target_uuid.clone(), "Android Client".to_string());
                                        let mut ctx = ctx_lock.write().await;
                                        let next_version = ctx.workspace.version.max(new_ws.version) + 1;
                                        ctx.workspace = new_ws;
                                        ctx.workspace.version = next_version;
                                        info!("✓ Full Workspace synced for UUID '{target_uuid}'. Version: {}", ctx.workspace.version);

                                        // Broadcast a todos (incluyendo editores y cliente Android con iconos enriquecidos)
                                        let sync_broadcast = WsEnvelope {
                                            r#type: "db_sync".to_string(),
                                            uuid: Some(target_uuid),
                                            timestamp: Some(chrono::Utc::now().timestamp_millis() as u64),
                                            payload: Some(serde_json::json!({
                                                "workspace": ctx.workspace
                                            })),
                                        };
                                        let _ = state.broadcast_tx.send(sync_broadcast);
                                    }
                                    Err(err) => error!("Failed to deserialize Workspace in db_sync: {err}"),
                                }
                            }
                            // Caso 2: Sincronización de botón individual (actualización o eliminación)
                            else if let Some(button_json) = payload.get("button") {
                                match serde_json::from_value::<Button>(button_json.clone()) {
                                    Ok(mut btn) => {
                                        let action = payload
                                            .get("action")
                                            .and_then(|v| v.as_str())
                                            .unwrap_or("update_button");
                                        let is_delete = action == "delete_button"
                                            || (btn.action_type == "NONE"
                                                && btn.label.trim().is_empty()
                                                && (btn.icon_name == "none" || btn.icon_name.is_empty()));

                                        let ctx_lock = state.context_manager.get_or_create(target_uuid.clone(), "Client".to_string());
                                        let mut ctx = ctx_lock.write().await;

                                        if is_delete {
                                            let profile_id = btn.profile_id.clone();
                                            let removed_pos = btn.position;

                                            // Compactar los botones configurados del perfil para no dejar huecos
                                            let mut other_buttons: Vec<Button> = ctx
                                                .workspace
                                                .buttons
                                                .iter()
                                                .filter(|b| b.profile_id != profile_id)
                                                .cloned()
                                                .collect();

                                            let mut profile_buttons: Vec<Button> = ctx
                                                .workspace
                                                .buttons
                                                .iter()
                                                .filter(|b| {
                                                    b.profile_id == profile_id
                                                        && b.position != removed_pos
                                                        && !(b.action_type == "NONE"
                                                            && b.label.trim().is_empty()
                                                            && (b.icon_name == "none"
                                                                || b.icon_name == "default"
                                                                || b.icon_name.is_empty()))
                                                })
                                                .cloned()
                                                .collect();

                                            profile_buttons.sort_by_key(|b| b.position);
                                            for (new_idx, item) in profile_buttons.iter_mut().enumerate() {
                                                item.position = new_idx as u8;
                                                item.id = format!("btn-{profile_id}-{new_idx}");
                                            }

                                            other_buttons.extend(profile_buttons);
                                            ctx.workspace.buttons = other_buttons;
                                            ctx.workspace.version += 1;
                                            info!("✓ Deleted & compacted button #{} (profile '{}') for UUID '{target_uuid}'", removed_pos + 1, profile_id);

                                            let sync_broadcast = WsEnvelope {
                                                r#type: "db_sync".to_string(),
                                                uuid: Some(target_uuid),
                                                timestamp: Some(chrono::Utc::now().timestamp_millis() as u64),
                                                payload: Some(serde_json::json!({
                                                    "workspace": ctx.workspace
                                                })),
                                            };
                                            let _ = state.broadcast_tx.send(sync_broadcast);
                                        } else {
                                            IconManager::enrich_button_icon(&mut btn);
                                            if let Some(pos) = ctx.workspace.buttons.iter().position(|b| b.profile_id == btn.profile_id && b.position == btn.position) {
                                                ctx.workspace.buttons[pos] = btn.clone();
                                            } else {
                                                ctx.workspace.buttons.push(btn.clone());
                                            }

                                            ctx.workspace.version += 1;
                                            info!("✓ Saved button #{} (profile '{}') for UUID '{target_uuid}'", btn.position + 1, btn.profile_id);

                                            let sync_broadcast = WsEnvelope {
                                                r#type: "db_sync".to_string(),
                                                uuid: Some(target_uuid),
                                                timestamp: Some(chrono::Utc::now().timestamp_millis() as u64),
                                                payload: Some(serde_json::json!({
                                                    "action": "update_button",
                                                    "button": btn
                                                })),
                                            };
                                            let _ = state.broadcast_tx.send(sync_broadcast);
                                        }
                                    }
                                    Err(err) => error!("Failed to deserialize Button in db_sync: {err}"),
                                }
                            }
                        }
                    }
                    "obs_config" => {
                        if let Some(ref payload) = envelope.payload {
                            let host = payload
                                .get("host")
                                .and_then(|v| v.as_str())
                                .unwrap_or("127.0.0.1")
                                .to_string();
                            let port = payload
                                .get("port")
                                .and_then(|v| v.as_u64())
                                .unwrap_or(4455) as u16;
                            let password = payload
                                .get("password")
                                .and_then(|v| v.as_str())
                                .unwrap_or("")
                                .to_string();
                            crate::plugins::obs_websocket::ObsWebsocketPlugin::set_config(
                                host, port, password,
                            );
                        }
                    }
                    "obs_get_scenes" => {
                        if let Some(ref payload) = envelope.payload {
                            if payload.get("host").is_some()
                                || payload.get("port").is_some()
                                || payload.get("password").is_some()
                            {
                                let current =
                                    crate::plugins::obs_websocket::ObsWebsocketPlugin::get_config();
                                let host = payload
                                    .get("host")
                                    .and_then(|v| v.as_str())
                                    .unwrap_or(&current.host)
                                    .to_string();
                                let port = payload
                                    .get("port")
                                    .and_then(|v| v.as_u64())
                                    .map(|p| p as u16)
                                    .unwrap_or(current.port);
                                let password = payload
                                    .get("password")
                                    .and_then(|v| v.as_str())
                                    .unwrap_or(&current.password)
                                    .to_string();
                                crate::plugins::obs_websocket::ObsWebsocketPlugin::set_config(
                                    host, port, password,
                                );
                            }
                        }

                        let target_uuid = envelope.uuid.clone();
                        let scenes = match crate::plugins::obs_websocket::ObsWebsocketPlugin::fetch_scenes_and_sources().await {
                            Ok(list) if !list.is_empty() => list,
                            Ok(_) | Err(_) => vec![
                                "Escena Principal".to_string(),
                                "START_RECORD".to_string(),
                                "START_STREAM".to_string(),
                                "TOGGLE_MUTE".to_string(),
                            ],
                        };

                        let reply = WsEnvelope {
                            r#type: "obs_scenes_list".to_string(),
                            uuid: target_uuid,
                            timestamp: Some(chrono::Utc::now().timestamp_millis() as u64),
                            payload: Some(serde_json::json!({
                                "scenes": scenes
                            })),
                        };
                        let _ = state.broadcast_tx.send(reply);
                    }
                    "get_processes" => {
                        let target_uuid = envelope.uuid.clone();
                        let processes = crate::plugins::task_manager::TaskManagerPlugin::get_process_list();
                        let reply = WsEnvelope {
                            r#type: "process_list".to_string(),
                            uuid: target_uuid,
                            timestamp: Some(chrono::Utc::now().timestamp_millis() as u64),
                            payload: Some(serde_json::json!({
                                "processes": processes
                            })),
                        };
                        let _ = state.broadcast_tx.send(reply);
                    }
                    "kill_process" => {
                        let target_uuid = envelope.uuid.clone();
                        if let Some(ref payload) = envelope.payload {
                            let pid = payload.get("pid").and_then(|v| v.as_u64()).map(|p| p as u32);
                            if let Some(pid) = pid {
                                let _ = crate::plugins::task_manager::TaskManagerPlugin::kill_process(pid);
                            }
                        }
                        tokio::time::sleep(tokio::time::Duration::from_millis(150)).await;
                        let processes = crate::plugins::task_manager::TaskManagerPlugin::get_process_list();
                        let reply = WsEnvelope {
                            r#type: "process_list".to_string(),
                            uuid: target_uuid,
                            timestamp: Some(chrono::Utc::now().timestamp_millis() as u64),
                            payload: Some(serde_json::json!({
                                "processes": processes
                            })),
                        };
                        let _ = state.broadcast_tx.send(reply);
                    }
                    "voicemeeter_get_state" => {
                        let target_uuid = envelope.uuid.clone();
                        let vm_state = crate::plugins::voicemeeter::VoicemeeterPlugin::get_state();
                        let reply = WsEnvelope {
                            r#type: "voicemeeter_state".to_string(),
                            uuid: target_uuid,
                            timestamp: Some(chrono::Utc::now().timestamp_millis() as u64),
                            payload: Some(vm_state),
                        };
                        let _ = state.broadcast_tx.send(reply);
                    }
                    "voicemeeter_set_param" => {
                        let target_uuid = envelope.uuid.clone();
                        if let Some(ref payload) = envelope.payload {
                            if let Some(param) = payload.get("param").and_then(|v| v.as_str()) {
                                if let Some(val) = payload.get("value").and_then(|v| v.as_f64()) {
                                    let _ = crate::plugins::voicemeeter::VoicemeeterPlugin::set_parameter_float(param, val as f32);
                                } else if let Some(action) = payload.get("action").and_then(|v| v.as_str()) {
                                    if action == "toggle" {
                                        let _ = crate::plugins::voicemeeter::VoicemeeterPlugin::toggle_parameter(param);
                                    }
                                }
                            }
                        }
                        tokio::time::sleep(tokio::time::Duration::from_millis(50)).await;
                        let vm_state = crate::plugins::voicemeeter::VoicemeeterPlugin::get_state();
                        let reply = WsEnvelope {
                            r#type: "voicemeeter_state".to_string(),
                            uuid: target_uuid,
                            timestamp: Some(chrono::Utc::now().timestamp_millis() as u64),
                            payload: Some(vm_state),
                        };
                        let _ = state.broadcast_tx.send(reply);
                    }
                    "action" => {
                        info!("Action trigger received from client: {:?}", envelope.uuid);
                        if let Some(payload) = envelope.payload {
                            let action_type = payload["action_type"].as_str().unwrap_or_default();
                            let action_value = payload["action_value"].as_str().unwrap_or_default();

                            match ActionEngine::execute_action(action_type, action_value).await {
                                Ok(_) => info!("Action executed successfully: {action_type} -> {action_value}"),
                                Err(err) => error!("Action execution failed: {err}"),
                            }
                        }
                    }
                    _ => {
                        warn!("Unhandled WebSocket message type: {}", envelope.r#type);
                    }
                }
            },
            Err(err) => {
                error!("Failed to parse WsEnvelope: {err} | Message: {text}");
            }
        }
        }
    }

    send_task.abort();
}
