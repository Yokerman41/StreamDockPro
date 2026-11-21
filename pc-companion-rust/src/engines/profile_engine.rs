use super::workspace_engine::{Profile, Workspace};

pub struct ProfileEngine;

impl ProfileEngine {
    pub fn find_profile_for_process<'a>(workspace: &'a Workspace, process_name: &str) -> Option<&'a Profile> {
        workspace.profiles.iter().find(|p| {
            if let Some(ref target) = p.auto_switch_process {
                target.eq_ignore_ascii_case(process_name)
            } else {
                false
            }
        })
    }
}
