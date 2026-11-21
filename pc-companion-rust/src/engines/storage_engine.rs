use std::fs;
use std::path::PathBuf;
use super::workspace_engine::Workspace;

pub trait StorageRepository: Send + Sync {
    fn load_workspace(&self, uuid: &str) -> Result<Workspace, String>;
    fn save_workspace(&self, workspace: &Workspace) -> Result<(), String>;
}

pub struct JsonFileStorageAdapter {
    pub base_dir: PathBuf,
}

impl JsonFileStorageAdapter {
    pub fn new(base_dir: PathBuf) -> Self {
        let workspaces_dir = base_dir.join("workspaces");
        let _ = fs::create_dir_all(&workspaces_dir);
        Self { base_dir: workspaces_dir }
    }
}

impl StorageRepository for JsonFileStorageAdapter {
    fn load_workspace(&self, uuid: &str) -> Result<Workspace, String> {
        let file_path = self.base_dir.join(format!("{uuid}.json"));
        if !file_path.exists() {
            return Err("Workspace file not found".to_string());
        }
        let content = fs::read_to_string(file_path).map_err(|e| e.to_string())?;
        serde_json::from_str(&content).map_err(|e| e.to_string())
    }

    fn save_workspace(&self, workspace: &Workspace) -> Result<(), String> {
        let file_path = self.base_dir.join(format!("{}.json", workspace.client_uuid));
        let content = serde_json::to_string_pretty(workspace).map_err(|e| e.to_string())?;
        fs::write(file_path, content).map_err(|e| e.to_string())
    }
}
