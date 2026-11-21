#[derive(Debug, Clone)]
pub struct PageEngine {
    pub current_page_index: usize,
}

impl PageEngine {
    pub fn new() -> Self {
        Self { current_page_index: 0 }
    }
}
