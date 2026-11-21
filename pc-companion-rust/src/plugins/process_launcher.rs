use std::process::Command;
use tracing::info;
use crate::plugins::win32_input::Win32InputPlugin;

pub struct ProcessLauncherPlugin;

impl ProcessLauncherPlugin {
    pub fn launch(command_path: &str) -> Result<(), String> {
        let trimmed = command_path.trim();
        let upper = trimmed.to_uppercase();
        if upper.starts_with("WIN_MINIMIZE")
            || upper.starts_with("WIN_MAXIMIZE")
            || upper.starts_with("WIN_CLOSE")
        {
            return Win32InputPlugin::execute_window_action(trimmed);
        }

        info!("Launching async process or shortcut: {trimmed}");

        let mut cmd = Command::new("cmd");
        cmd.args(["/C", "start", "", trimmed]);
        #[cfg(windows)]
        {
            use std::os::windows::process::CommandExt;
            const CREATE_NO_WINDOW: u32 = 0x08000000;
            cmd.creation_flags(CREATE_NO_WINDOW);
        }
        cmd.spawn().map_err(|e| e.to_string())?;

        Ok(())
    }
}
