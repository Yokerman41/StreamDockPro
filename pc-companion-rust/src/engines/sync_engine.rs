use super::workspace_engine::Workspace;
use crate::utils::checksum::compute_sha256;

pub struct SyncEngine;

impl SyncEngine {
    pub fn compute_workspace_hash(workspace: &Workspace) -> String {
        let serialized = serde_json::to_string(workspace).unwrap_or_default();
        compute_sha256(serialized.as_bytes())
    }
}
