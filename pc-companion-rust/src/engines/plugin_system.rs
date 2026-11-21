pub trait StreamDockPlugin: Send + Sync {
    fn name(&self) -> &'static str;
    fn version(&self) -> &'static str;
    fn execute_action(&self, action_type: &str, payload: &str) -> Result<String, String>;
}

pub struct PluginSystem {
    plugins: Vec<Box<dyn StreamDockPlugin>>,
}

impl PluginSystem {
    pub fn new() -> Self {
        Self { plugins: Vec::new() }
    }

    pub fn register(&mut self, plugin: Box<dyn StreamDockPlugin>) {
        self.plugins.push(plugin);
    }
}
