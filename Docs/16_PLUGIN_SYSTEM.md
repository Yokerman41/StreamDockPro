# 16. Plugin System (`PluginSystem`) - Arquitectura de Extensibilidad Dinámica

## 1. Visión General del Sistema de Plugins

El `PluginSystem` extiende las capacidades de la plataforma permitiendo a desarrolladores de terceros crear ejecutores de acciones, integraciones con software externo y widgets visuales personalizados sin modificar el código fuente del núcleo.

---

## 2. Arquitectura de Plugins y Sandbox de Ejecución

```
+-----------------------------------------------------------------------------------+
|                                SERVIDORES RUST CORE                               |
|  +-----------------------------------------------------------------------------+  |
|  |                            PLUGIN REGISTRY HOST                             |  |
|  |  - Plugin Manager Lifecycle (Load, Unload, Register, Execute)                |  |
|  +-----------------------+-----------------------------+------------------------+  |
+--------------------------|-----------------------------|--------------------------+
                           | Dynamic Bindings            | WASM Sandbox / FFI
                           v                             v
            +------------------------------+  +------------------------------+
            |  Native DLL Plugin (C/C++)   |  |  WASM Plugin (Rust/Assembly) |
            |  - Low Latency Win32 Access  |  |  - Isolated Execution Safe   |
            +------------------------------+  +------------------------------+
```

---

## 3. Especificación de la Interfaz de Plugins

Todo plugin debe cumplir con la especificación de ciclo de vida:

```rust
// Concepto de Trait de Plugin de Arquitectura
pub trait StreamDockPlugin: Send + Sync {
    fn name(&self) -> &'static str;
    fn version(&self) -> &'static str;
    fn on_load(&mut self) -> Result<(), PluginError>;
    fn execute_action(&self, action_type: &str, payload: &str) -> Result<String, PluginError>;
    fn on_unload(&mut self);
}
```

---

## 4. Tipos de Plugins Soportados
1. **Action Plugins**: Integración con aplicaciones externas (ej. Spotify, Discord, Philips Hue, Voicemeeter).
2. **Visual Plugins**: Renderizado dinámico de widgets (ej. gráficos de uso de CPU/GPU en tiempo real, reloj digital, cotizaciones de criptomonedas).
3. **Scripting Plugins**: Motores de script ligero (ej. Lua, JavaScript/V8) para ejecutar macros complejas condicionales.
