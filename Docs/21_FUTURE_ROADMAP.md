# 21. Future Roadmap & Extensibility (`FutureRoadmap`) - Hoja de Ruta Eos

## 1. Visión a Largo Plazo

La plataforma está diseñada para evolucionar desde un controlador de accesos directos hacia un **Ecosistema Inteligente de Automatización de Escritorio** apoyado en capacidades de Inteligencia Artificial local, plugins distribuidos y persistencia escalable.

---

## 2. Fases del Roadmap de Extensibilidad

```
+-----------------------------------------------------------------------------------+
| FASE 1: NÚCLEO MULTI-CLIENTE & CONTEXTOS (Completada / Actual)                    |
| - Aislamiento absoluto por ClientContext, UUID inmutable, transporte WS/ADB USB.  |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
| FASE 2: MOTOR DE MIGRACIÓN SQLITE / POSTGRESQL & WEB DASHBOARD                     |
| - Migración de archivos JSON a SQLite local / PostgreSQL para despliegue empresarial.|
| - Panel de administración web remoto (React/Next.js) servido por Axum Rust.       |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
| FASE 3: STORE DE PLUGINS COMUNITARIOS & RUNTIME WASM                              |
| - Tienda pública de plugins y paquetes de acciones instalables con 1-clic.        |
| - Ejecución segura de plugins de terceros dentro de Sandbox WebAssembly (Wasmtime)|
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
| FASE 4: AGENTES IA LOCALES & RECONOCIMIENTO CONTEXTUAL AVANZADO                   |
| - Integración de modelos SLM locales (ej. Phi-3 / Llama 3 en ONNX Runtime C++)     |
| - Generación automática de botones, macros y perfiles según el hábito del usuario. |
+-----------------------------------------------------------------------------------+
```

---

## 3. Principios de Compatibilidad Hacia Adelante (*Backward Compatibility*)
1. **Estabilidad del Esquema de Contexto**: El identificador `ClientUUID` seguirá siendo el ancla soberana de datos en todas las iteraciones futuras.
2. **Protocolo Versionado**: Los payloads JSON mantendrán firmas retrocompatibles garantizando que clientes Android en versiones anteriores puedan conectarse sin romper la sesión.
