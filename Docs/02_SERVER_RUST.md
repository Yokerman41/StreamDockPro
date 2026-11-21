# 02. Servidor Rust (`pc-companion-rust`) - Especificación Técnica del Núcleo

## 1. Visión General del Núcleo Rust

El servidor `pc-companion-rust` actúa como el motor central y broker del sistema. Está diseñado utilizando programación asíncrona de alto rendimiento con el runtime de `Tokio`, el marco HTTP/WebSocket `Axum`, y estructuras de datos concurrentes lock-free (`DashMap` / `Arc<RwLock>`).

---

## 2. Diagrama de Módulos Internos de Rust

```
+-----------------------------------------------------------------------------------+
|                                 AXUM WEB FRAMEWORK                                |
|  Routes: GET /ws (WebSocket Endpoint)  |  Static Assets / Diagnostic Endpoints    |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                             CLIENT CONTEXT ROUTER & MANAGER                       |
|  - DashMap<ClientUUID, Arc<ClientContext>>                                        |
|  - ClientSockets: DashMap<ClientUUID, SocketAddr>                                 |
+---------------------+-----------------------------------+-------------------------+
                      |                                   |
                      v                                   v
+-----------------------------------+   +-------------------------------------------+
|          EVENT BUS ENGINE         |   |             WORKSPACE ENGINE              |
| - Broadcast Channels (Tokio MPSC) |   | - Isolation per UUID                      |
| - Async Event Dispatcher          |   | - Atomic Storage Persistence              |
+---------------------+-------------+   +---------------------+---------------------+
                      |                                       |
                      +-------------------+-------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                                 PLUGINS & EXECUTORS                               |
| [OS Keyboard/Mouse] | [Process Launcher] | [OBS WebSocket] | [Streamer.Bot WS]   |
+-----------------------------------------------------------------------------------+
```

---

## 3. Especificación de Componentes y Claves de Diseño

### 3.1 Estructuras de Datos Principales (Conceptualmente)
* **`ClientContext`**: Envoltorio principal que contiene el `ClientUUID`, la estructura `Workspace`, metadatos del cliente, canal de respuesta WebSocket `UnboundedSender<Message>`, versión de secuencia y temporizadores de latencia.
* **`ClientManager`**: Administra la tabla hash concurrente `DashMap<String, Arc<ClientContext>>`. Proporciona métodos hilo-seguros (*thread-safe*) para crear, recuperar, renombrar y desconectar clientes.
* **`Workspace`**: Estructura de modelo que serializa hacia y desde `workspaces/{uuid}.json`.

---

## 4. Algoritmo de Registro y Desconexión

```
   [Conexión WebSocket Entrante]
                |
                v
  ¿Mensaje es `register_client`?
        /              \
     (Sí)              (No)
      /                  \
Extract UUID         Ignorar hasta autenticar/registrar
      |
¿Existe en `ClientManager`?
     /         \
  (Sí)         (No)
   /             \
Actualizar     Crear nuevo `ClientContext`
Estado/IP      Migrar o generar Workspace por defecto
   \             /
    v           v
Guardar en DashMap
Registrar Socket Rx/Tx
Enviar `register_ack` con Snapshot del Workspace al Cliente
Notificar a todos los Editores WinUI 3 mediante `clients_updated`
```

---

## 5. Gestión de Concurrencia y Sincronización Interna
1. **Ausencia de Bloqueos Globales (`No Mutex Deadlocks`)**:
   * Prohibido utilizar `std::sync::Mutex` síncronos sobre operaciones de E/S o await.
   * Uso exclusivo de `tokio::sync::RwLock` o lecturas concurrentes atomic en `DashMap`.
2. **Ciclo de Vida de Tareas de Tokio**:
   * Cada cliente conectado genera dos tareas independientes (*tokio tasks*):
     1. **`read_loop`**: Recibe tramas WebSocket, deserializa eventos y los despacha al `EventBus`.
     2. **`write_loop`**: Lee del canal MPSC privado del cliente (`rx`) y escribe tramas en la red.
