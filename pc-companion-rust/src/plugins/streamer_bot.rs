use tracing::info;

pub struct StreamerBotPlugin;

impl StreamerBotPlugin {
    pub fn run_action(action_id: &str) -> Result<(), String> {
        info!("Streamer.bot: Running action '{action_id}'");
        Ok(())
    }
}
