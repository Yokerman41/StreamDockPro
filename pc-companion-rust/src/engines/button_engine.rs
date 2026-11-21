use super::workspace_engine::Button;

pub struct ButtonEngine;

impl ButtonEngine {
    pub fn trigger_feedback(button: &mut Button) {
        button.is_pressed = true;
    }
}
