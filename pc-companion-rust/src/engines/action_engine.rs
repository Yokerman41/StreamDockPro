use tracing::info;
use crate::plugins::obs_websocket::ObsWebsocketPlugin;
use crate::plugins::process_launcher::ProcessLauncherPlugin;
use crate::plugins::streamer_bot::StreamerBotPlugin;
use crate::plugins::win32_input::Win32InputPlugin;

pub struct ActionEngine;

impl ActionEngine {
    pub async fn execute_action(action_type: &str, action_value: &str) -> Result<(), String> {
        info!("Executing action [{action_type}] with value [{action_value}]");
        match action_type {
            "LAUNCH_APP" | "LAUNCH_SHORTCUT" => {
                ProcessLauncherPlugin::launch(action_value)
            }
            "MEDIA_CONTROL" => {
                Win32InputPlugin::send_media_key(action_value)
            }
            "SYSTEM_COMMAND" => {
                Win32InputPlugin::execute_system_command(action_value)
            }
            "KEYBOARD_HOTKEY" => {
                Win32InputPlugin::send_hotkey(action_value)
            }
            "OBS_SCENE_SWITCH" => {
                ObsWebsocketPlugin::switch_scene(action_value).await
            }
            "STREAMER_BOT_ACTION" => {
                StreamerBotPlugin::run_action(action_value)
            }
            _ => {
                info!("Generic action executed: {action_type} -> {action_value}");
                Ok(())
            }
        }
    }
}
