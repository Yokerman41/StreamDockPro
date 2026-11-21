# 10. Action Engine (`ActionEngine`) - Motor de Ejecución de Acciones

## 1. Visión General del Motor de Acciones

El `ActionEngine` es el subsistema en el servidor Rust responsable de interpretar y ejecutar la lógica de negocio asociada a la presión de un botón en cualquier consola Android.

---

## 2. Diagrama de Ejecución y Pipeline de Acciones

```
[ Trama `action_trigger` de Cliente Android ]
                    |
                    v
    Extraer `ClientUUID`, `Position`, `ProfileId`
                    |
                    v
    Obtener `ClientContext` desde Router
                    |
                    v
    Validar si el botón existe y está habilitado
                    |
                    v
  +---------------------------------------------------+
  |               DISPATCHER DE ACCIONES              |
  +---------------------------------------------------+
    |                   |                  |
    v                   v                  v
[Plugin Win32]   [Plugin OBS WS]  [Plugin Streamer.Bot]
 - Keyboard Input  - Switch Scene   - Run Action
 - Execute Process - Mute Audio     - Run Macro
    |                   |                  |
    +-------------------+------------------+
                        |
                        v
    Enviar `action_feedback` (Resultado OK / Error)
                        |
                        v
     [Cliente Android recibe vibración hálita]
```

---

## 3. Clases de Ejecutores

1. **Win32 Input Executor**: Utiliza APIs de bajo nivel de Windows (`SendInput`, `VkKeyScan`) para simular pulsaciones de teclas complejas, atajos con modificadores (`Ctrl`, `Shift`, `Alt`, `Win`), eventos de ratón y atajos multimedia.
2. **System Process Executor**: Lanza procesos ejecutables de forma asíncrona no bloqueante sin bloquear el bucle de eventos principal de Tokio.
3. **OBS WebSocket v5 Executor**: Mantiene un hilo de cliente WebSocket asíncrono hacia OBS Studio. Soporta cambio de escenas, control de fuentes de vídeo, filtros, audio y estado de grabación/stream.
4. **Streamer.Bot WebSocket Executor**: Integra eventos y macros de automatización para streamers en plataformas como Twitch y YouTube.
