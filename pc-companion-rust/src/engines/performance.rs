pub struct PerformanceMetrics {
    pub active_connections: usize,
    pub latency_ms: f64,
}

impl PerformanceMetrics {
    pub fn default() -> Self {
        Self {
            active_connections: 0,
            latency_ms: 0.0,
        }
    }
}
