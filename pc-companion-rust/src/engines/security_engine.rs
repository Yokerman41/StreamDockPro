pub struct SecurityEngine;

impl SecurityEngine {
    pub fn verify_pairing_pin(input_pin: &str, expected_pin: &str) -> bool {
        input_pin == expected_pin
    }
}
