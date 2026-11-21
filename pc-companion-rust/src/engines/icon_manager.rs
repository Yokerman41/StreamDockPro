use crate::engines::workspace_engine::Button;

pub struct IconManager;

impl IconManager {
    pub fn get_icon_path(icon_name: &str) -> String {
        format!("extracted_icons/{icon_name}.png")
    }

    /// Si un botón no tiene un icono personalizado (o tiene "default"/"none") pero tiene una
    /// acción configurada, le asigna el nombre de icono vectorial correspondiente para que el
    /// cliente Android muestre el icono correcto. Si ya trae un PNG en Base64
    /// (`data:image/png;base64,...`) o un nombre de icono explícito, lo preserva.
    pub fn enrich_button_icon(btn: &mut Button) {
        let trimmed = btn.icon_name.trim();
        if trimmed.starts_with("data:image/") {
            return;
        }

        let needs_inference = trimmed.is_empty() || trimmed == "default" || trimmed == "none";
        if !needs_inference {
            return;
        }

        if let Some(inferred) = Self::infer_icon_for_action(&btn.action_type, &btn.action_value) {
            btn.icon_name = inferred.to_string();
        }
    }

    pub fn infer_icon_for_action(action_type: &str, action_value: &str) -> Option<&'static str> {
        match action_type {
            "MEDIA_CONTROL" => match action_value {
                "PLAY_PAUSE" | "SPOTIFY_PLAY_PAUSE" | "SPOTIFY_OPEN" => Some("play"),
                "NEXT_TRACK" | "SPOTIFY_NEXT" => Some("next"),
                "PREV_TRACK" | "SPOTIFY_PREV" => Some("prev"),
                "VOLUME_UP" | "SPOTIFY_VOL_UP" => Some("vol_up"),
                "VOLUME_DOWN" | "SPOTIFY_VOL_DOWN" => Some("vol_down"),
                "VOLUME_MUTE" | "SPOTIFY_MUTE" => Some("mute"),
                "SPOTIFY_STOP" => Some("stop"),
                _ => Some("play"),
            },
            "SYSTEM_COMMAND" => match action_value {
                "LOCK_WORKSTATION" => Some("lock"),
                "SHOW_DESKTOP" => Some("computer"),
                "SLEEP" | "SHUTDOWN" | "REBOOT" => Some("power"),
                _ => Some("settings"),
            },
            "KEYBOARD_HOTKEY" => match action_value {
                "CTRL+SHIFT+M" => Some("mic_off"),
                "WIN+SHIFT+S" => Some("computer"),
                _ => Some("keyboard"),
            },
            "OBS_SCENE_SWITCH" => match action_value {
                "START_RECORD" => Some("record"),
                "START_STREAM" => Some("stream"),
                "TOGGLE_MUTE" => Some("mic_off"),
                _ => Some("tv"),
            },
            "LAUNCH_APP" | "LAUNCH_SHORTCUT" => match action_value {
                v if v.starts_with("WIN_MINIMIZE") => Some("computer"),
                v if v.starts_with("WIN_MAXIMIZE") => Some("apps"),
                v if v.starts_with("WIN_CLOSE") => Some("error"),
                _ => Some("launch"),
            },
            "STREAMER_BOT_ACTION" => Some("stream"),
            "NAVIGATE" => Some("folder"),
            _ => None,
        }
    }
}
