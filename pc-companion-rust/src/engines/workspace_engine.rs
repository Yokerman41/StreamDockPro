use serde::{Deserialize, Serialize};

fn default_grid_rows() -> u8 {
    5
}

fn default_grid_cols() -> u8 {
    4
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Button {
    pub id: String,
    pub profile_id: String,
    pub position: u8,
    pub label: String,
    #[serde(default)]
    pub icon_name: String,
    #[serde(default)]
    pub icon_tint: Option<String>,
    #[serde(default)]
    pub background_color: Option<String>,
    #[serde(default)]
    pub font_size: Option<u8>,
    pub action_type: String,
    pub action_value: String,
    #[serde(default)]
    pub is_pressed: bool,
    #[serde(default)]
    pub is_active_state: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Profile {
    pub id: String,
    pub name: String,
    #[serde(default)]
    pub auto_switch_process: Option<String>,
    #[serde(default)]
    pub is_default: bool,
    #[serde(default)]
    pub icon_name: String,
    #[serde(default)]
    pub background_color: String,
    #[serde(default = "default_grid_rows")]
    pub grid_rows: u8,
    #[serde(default = "default_grid_cols")]
    pub grid_cols: u8,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Workspace {
    pub client_uuid: String,
    pub version: u64,
    pub active_profile_id: String,
    #[serde(default = "default_grid_rows")]
    pub grid_rows: u8,
    #[serde(default = "default_grid_cols")]
    pub grid_cols: u8,
    pub profiles: Vec<Profile>,
    pub buttons: Vec<Button>,
}

impl Workspace {
    pub fn new_default(client_uuid: String) -> Self {
        let default_profile_id = format!("prof-default-{}", uuid::Uuid::new_v4());
        let default_profile = Profile {
            id: default_profile_id.clone(),
            name: "Default Profile".to_string(),
            auto_switch_process: None,
            is_default: true,
            icon_name: "home".to_string(),
            background_color: "#1E1E2E".to_string(),
            grid_rows: default_grid_rows(),
            grid_cols: default_grid_cols(),
        };

        Self {
            client_uuid,
            version: 1,
            active_profile_id: default_profile_id,
            grid_rows: default_grid_rows(),
            grid_cols: default_grid_cols(),
            profiles: vec![default_profile],
            buttons: Vec::new(),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn android_and_winui_workspace_json_deserializes_with_defaults() {
        let json = r##"{
            "client_uuid": "uuid-test",
            "version": 3,
            "active_profile_id": "prof-1",
            "grid_rows": 5,
            "grid_cols": 4,
            "profiles": [
                {
                    "id": "prof-1",
                    "name": "Nueva Página",
                    "is_default": true,
                    "icon_name": "apps",
                    "background_color": "#1E1E2E"
                }
            ],
            "buttons": [
                {
                    "id": "btn-prof-1-0",
                    "profile_id": "prof-1",
                    "position": 0,
                    "label": "Play",
                    "icon_name": "play",
                    "action_type": "MEDIA_CONTROL",
                    "action_value": "PLAY_PAUSE",
                    "background_color": "#9B59B6"
                }
            ]
        }"##;

        let ws: Workspace = serde_json::from_str(json).expect("Valid workspace JSON");
        assert_eq!(ws.client_uuid, "uuid-test");
        assert_eq!(ws.version, 3);
        assert_eq!(ws.profiles.len(), 1);
        assert_eq!(ws.profiles[0].grid_rows, 5);
        assert_eq!(ws.profiles[0].grid_cols, 4);
        assert_eq!(ws.buttons.len(), 1);
        assert_eq!(ws.buttons[0].action_value, "PLAY_PAUSE");
    }
}

