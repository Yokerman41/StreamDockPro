# 01. Arquitectura General del Sistema y Topología de Red

## 1. Topología del Sistema y Aislamiento de Red

La topología del sistema se estructura como un grafo distribuido de estrella centrado en el **Servidor Rust**, el cual actúa como Broker de Mensajes, Bus de Eventos y Coordinador de Persistencia. 

```
                                  +-----------------------+
                                  |   WinUI 3 GUI Editor  |
                                  | (ClientContext UI Selector)
                                  +-----------+-----------+
                                              |
                                   WebSocket Inter-Process
                                     (ws://127.0.0.1:8081)
                                              |
                                              v
+-----------------------+         +-----------------------+         +-----------------------+
|  Android Client 1     |         |     Rust Core Server  |         |  Android Client 2     |
| (UUID-111, Wi-Fi LAN) |<------->| (ClientContext Router)|<------->| (UUID-222, USB ADB)   |
| ws://192.168.1.50:8081|         | Port: 8081 Async      |         | ws://127.0.0.1:8081   |
+-----------------------+         +-----------------------+         +-----------------------+
```

---

## 2. Flujo de Datos End-to-End

### 2.1 Flujo de Registro de Dispositivo y Carga de Contexto
```
[Android Client]                  [Rust Server Core]               [WinUI 3 Editor]
       |                                   |                               |
       |----- (1) WS Connect ------------->|                               |
       |----- (2) register_client -------->|                               |
       |      (UUID, Hardware Metadata)    |                               |
       |                                   |-- (3) Lookup/Create Context ->|
       |                                   |   `workspaces/{uuid}.json`    |
       |                                   |                               |
       |<---- (4) register_ack ------------|                               |
       |      (UUID, Full Workspace Snapshot)                             |
       |                                   |-- (5) clients_updated -------->|
       |                                   |   (Broadcasting active list)  |
```

### 2.2 Flujo de Ejecución de Acción desde Dispositivo Móvil
```
[Android Client]                  [Rust Server Core]              [OS / App Target]
       |                                   |                               |
       |-- (1) action_trigger ------------>|                               |
       |    (UUID, ProfileId, Position)    |                               |
       |                                   |-- (2) Resolve ClientContext ->|
       |                                   |-- (3) Extract Button Action ->|
       |                                   |-- (4) Execute via ActionEngine|-------> [Win32 API / OBS]
       |<-- (5) action_feedback -----------|                               |
       |    (Execution status: OK/ERR)     |                               |
```

---

## 3. Modelo de Capas y Aislamiento

```
+-----------------------------------------------------------------------------------+
|                            CAPA DE PRESENTACIÓN & UI                              |
|   Android Grid Renderer (Kotlin StateFlow)  |  WinUI 3 Dynamic Context Editor     |
+-----------------------------------------------------------------------------------+
                                         |  Interfaces Desacopladas
+-----------------------------------------------------------------------------------+
|                            CAPA DE COMUNICACIÓN & TRANSPORTE                      |
|   WebSocket Framing Protocol (TCP/JSON)    |  ADB Bridge Tunnel (USB Multiplex)   |
+-----------------------------------------------------------------------------------+
                                         |  JSON RPC / Custom Frames
+-----------------------------------------------------------------------------------+
|                            CAPA DE GESTIÓN DE CONTEXTOS                           |
|   ClientContext Router  |  Session Monitor  |  Version Control & Delta Merger     |
+-----------------------------------------------------------------------------------+
                                         |  Domain Events
+-----------------------------------------------------------------------------------+
|                            CAPA DE DOMINIO Y EJECUCIÓN                            |
|   Workspace Engine  |  Profile Engine  |  Button Engine  |  Action Engine (Plugins) |
+-----------------------------------------------------------------------------------+
                                         |  I/O Abstracción
+-----------------------------------------------------------------------------------+
|                            CAPA DE PERSISTENCIA Y ADAPTADORES                     |
|   JSON File Adapter (Current)  -->  [SQLite Adapter]  -->  [PostgreSQL Adapter]   |
+-----------------------------------------------------------------------------------+
```

---

## 4. Matriz de Requisitos No Funcionales (NFR)

1. **Latencia Extremadamente Baja**:
   * Tiempo de ida y vuelta (*RTT*) de ejecución de botón en red local $< 8\text{ ms}$.
   * Latencia sobre ADB USB $< 2\text{ ms}$.
2. **Concurrencia Escala Horizontal en Nodo Único**:
   * Capacidad para gestionar hasta 1,000 conexiones concurrentes sin fuga de memoria ni consumo de CPU $> 3\%$ en reposo.
3. **Resiliencia y Auto-Recuperación**:
   * Reconexión automática transparente en menos de $1.5\text{ segundos}$ tras pérdida de señal Wi-Fi.
   * Cero pérdida de datos de configuración mediante escrituras atómicas (*Write-Ahead Simulation* en archivos de disco).
4. **Portabilidad y Desacoplamiento**:
   * Cero acoplamiento de lógica de negocio en la capa de interfaz de usuario de WinUI 3 o Android. El servidor contiene la verdad única del modelo.
