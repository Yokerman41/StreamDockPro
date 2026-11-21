#![cfg_attr(not(test), windows_subsystem = "windows")]
#![allow(dead_code)]
#![allow(unused_variables)]

use std::net::SocketAddr;
use std::sync::Arc;
use tokio::sync::broadcast;
use tracing::info;
use tracing_subscriber::FmtSubscriber;

mod core;
mod engines;
mod plugins;
mod transport;
mod utils;

use core::context_manager::ContextManager;
use transport::mdns_discovery::start_mdns_responder;
use transport::ws_server::{create_router, ServerState};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let subscriber = FmtSubscriber::builder()
        .with_max_level(tracing::Level::INFO)
        .finish();
    tracing::subscriber::set_global_default(subscriber)?;

    info!("Starting Stream Dock Pro - Rust Server Core v0.1.0");

    let _mdns = start_mdns_responder(8081);

    let context_manager = ContextManager::new();
    let (broadcast_tx, _) = broadcast::channel(100);
    let state = Arc::new(ServerState {
        context_manager,
        broadcast_tx,
    });

    let app = create_router(state);
    let addr = SocketAddr::from(([0, 0, 0, 0], 8081));
    info!("Server listening on ws://{}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await?;
    axum::serve(listener, app).await?;

    Ok(())
}
