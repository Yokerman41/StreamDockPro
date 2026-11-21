# 15. Event Bus Engine (`EventBusEngine`) - Bus de Eventos Asíncrono

## 1. Visión General del Bus de Eventos

El `EventBusEngine` es el centro de mensajería pub/sub (*Publish-Subscribe*) dentro del Servidor Rust. Permite que los subsistemas (OBS, Streamer.bot, Sistema Operativo, WinUI 3 y Clientes Android) emitan y reaccionen a eventos sin acoplamiento directo entre módulos.

---

## 2. Diagrama de Publicación y Suscripción de Eventos

```
  [ FUENTES DE EVENTOS ]                     [ BUS DE EVENTOS ]                     [ SUSCRIPTORES ]
                                                  (Tokio MPSC)
+-----------------------+                         +--------+                      +-----------------------+
| Evento OBS WebSocket  |------------------------>|        |--------------------->| ClientContext (Redmi) |
| (ej. SceneChanged)    |                         |        |                      +-----------------------+
+-----------------------+                         |        |
                                                  | EVENT  |                      +-----------------------+
+-----------------------+                         |  BUS   |--------------------->| ClientContext (Tablet)|
| Evento Sistema Win32  |------------------------>|        |                      +-----------------------+
| (ej. ActiveAppChange) |                         |        |
+-----------------------+                         |        |                      +-----------------------+
                                                  |        |--------------------->| WinUI 3 GUI Editor    |
+-----------------------+                         +--------+                      +-----------------------+
| Evento Plugin Futuro  |------------------------>
+-----------------------+
```

---

## 3. Tipos de Eventos del Bus
1. **`SystemEvent::ForegroundAppChanged(String)`**: Emitido cuando la ventana enfocada en Windows cambia.
2. **`ObsEvent::SceneSwitched(String)`**: Emitido cuando OBS cambia de escena activa.
3. **`ClientEvent::ClientConnected(String)`**: Emitido cuando un nuevo dispositivo establece conexión.
4. **`ClientEvent::WorkspaceMutated(String)`**: Emitido cuando se aplica un cambio al workspace de un cliente.
