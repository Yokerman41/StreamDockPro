use tokio::sync::broadcast;

#[derive(Debug, Clone)]
pub enum SystemEvent {
    ForegroundAppChanged(String),
    ObsSceneSwitched(String),
    ClientConnected(String),
    WorkspaceMutated(String),
}

pub struct EventBusEngine {
    sender: broadcast::Sender<SystemEvent>,
}

impl EventBusEngine {
    pub fn new(capacity: usize) -> Self {
        let (sender, _) = broadcast::channel(capacity);
        Self { sender }
    }

    pub fn publish(&self, event: SystemEvent) {
        let _ = self.sender.send(event);
    }

    pub fn subscribe(&self) -> broadcast::Receiver<SystemEvent> {
        self.sender.subscribe()
    }
}
