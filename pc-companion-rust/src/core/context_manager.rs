use dashmap::DashMap;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tokio::sync::RwLock;
use crate::core::client_context::{ClientContext, ConnectionStatus};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ClientSummary {
    pub uuid: String,
    pub display_name: String,
    pub status: String,
}

pub struct ContextManager {
    contexts: DashMap<String, Arc<RwLock<ClientContext>>>,
}

impl ContextManager {
    pub fn new() -> Self {
        Self {
            contexts: DashMap::new(),
        }
    }

    pub fn get_or_create(&self, uuid: String, display_name: String) -> Arc<RwLock<ClientContext>> {
        self.contexts
            .entry(uuid.clone())
            .or_insert_with(|| Arc::new(RwLock::new(ClientContext::new(uuid, display_name))))
            .value()
            .clone()
    }

    pub fn get(&self, uuid: &str) -> Option<Arc<RwLock<ClientContext>>> {
        self.contexts.get(uuid).map(|r| r.value().clone())
    }

    pub fn list_uuids(&self) -> Vec<String> {
        self.contexts.iter().map(|kv| kv.key().clone()).collect()
    }

    pub async fn list_clients(&self) -> Vec<ClientSummary> {
        let mut list = Vec::new();
        for entry in self.contexts.iter() {
            let ctx = entry.value().read().await;
            let status_str = match ctx.connection_status {
                ConnectionStatus::Online => "Online",
                ConnectionStatus::Offline => "Offline",
            };
            list.push(ClientSummary {
                uuid: ctx.metadata.client_uuid.clone(),
                display_name: ctx.metadata.display_name.clone(),
                status: status_str.to_string(),
            });
        }
        list
    }
}

