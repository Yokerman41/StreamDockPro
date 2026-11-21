# 03. Cliente Android (`app`) - Especificación Técnica del Cliente Móvil

## 1. Visión General de la Aplicación Android

La aplicación Android actúa como la superficie interactiva táctil. Está construida bajo la arquitectura **MVVM (Model-View-ViewModel)** reactiva con Jetpack Compose / Custom Canvas Rendering, utilizando Kotlin Coroutines y `StateFlow` para garantizar renderizado fluido a 60-120 FPS sin bloqueos en el hilo UI (*Main Thread*).

---

## 2. Diagrama de Capas de Android

```
+-----------------------------------------------------------------------------------+
|                              JETPACK COMPOSE / UI LAYER                           |
|  - GridScreen (Custom Grid Dynamic 4x5, 3x3, 5x6)                                |
|  - ButtonTile Component (Canvas, Tinting, Text, Micro-animations)                |
|  - Configuration / Connection Status Bar                                         |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                               VIEWMODEL & REACTIVE STATE                          |
|  - MainViewModel: StateFlow<WorkspaceUiState>                                     |
|  - NetworkViewModel: StateFlow<ConnectionState>                                   |
+---------------------+-----------------------------------+-------------------------+
                      |                                   |
                      v                                   v
+-----------------------------------+   +-------------------------------------------+
|          NETWORK MANAGER          |   |               UUID MANAGER                |
| - OkHttp WebSocket Client         |   | - SharedPreferences / KeyStore           |
| - ADB Local Loopback Listener     |   | - Immutable ClientUUID Provider           |
| - Reconnection & Heartbeat Engine |   | - Hardware Specs Harvester                |
+-----------------------------------+   +-------------------------------------------+
```

---

## 3. Máquina de Estados de Conexión del Cliente

```
  +------------------+
  |  DISCONNECTED    |<-----------------------------------+
  +--------+---------+                                    |
           |                                              |
     Intent de Conexión (Wi-Fi / ADB)                    |
           v                                              |
  +------------------+                                    |
  |   CONNECTING     |                                    |
  +--------+---------+                                    |
           |                                              |
   Socket Abierto (WS Open)                               |
           v                                              |
  +------------------+                                    | Error / Timeout
  |   REGISTERING    |--(Envía `register_client`)         |
  +--------+---------+                                    |
           |                                              |
    `register_ack` recibido                               |
           v                                              |
  +------------------+                                    |
  |    CONNECTED     |------------------------------------+
  +------------------+
```

---

## 4. Estrategia de Persistencia y Caché Local
1. **UUID Inalterable**: Generado al inicio y guardado en `SharedPreferences` privado de la aplicación (`stream_deck_prefs.xml`).
2. **Caché Offline del Workspace**: El último `Workspace` recibido se almacena localmente en disco como JSON comprimido. Si la aplicación inicia sin red, muestra la interfaz previa en modo `OFFLINE` hasta recuperar conectividad con el servidor Rust.
3. **Gestión de Memoria en Cuadrícula**: Los bitmaps de los botones e iconos se almacenan en memoria caché LRU (*Least Recently Used*) para evitar sobrecargas de Garbage Collection (*GC Pauses*).
