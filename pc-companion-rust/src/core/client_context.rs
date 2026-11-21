use std::net::SocketAddr;
use crate::engines::workspace_engine::Workspace;

#[derive(Debug, Clone, PartialEq)]
pub enum ConnectionStatus {
    Online,
    Offline,
}

#[derive(Debug, Clone)]
pub struct ClientMetadata {
    pub client_uuid: String,
    pub display_name: String,
    pub manufacturer: Option<String>,
    pub model: Option<String>,
    pub android_version: Option<String>,
}

pub struct ClientContext {
    pub metadata: ClientMetadata,
    pub workspace: Workspace,
    pub socket_addr: Option<SocketAddr>,
    pub connection_status: ConnectionStatus,
    pub sequence_version: u64,
}

impl ClientContext {
    pub fn new(uuid: String, display_name: String) -> Self {
        let workspace = Workspace::new_default(uuid.clone());
        Self {
            metadata: ClientMetadata {
                client_uuid: uuid,
                display_name,
                manufacturer: None,
                model: None,
                android_version: None,
            },
            workspace,
            socket_addr: None,
            connection_status: ConnectionStatus::Offline,
            sequence_version: 1,
        }
    }
}
