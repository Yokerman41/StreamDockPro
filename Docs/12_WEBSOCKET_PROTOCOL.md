# 12. WebSocket Protocol (`WebSocketProtocol`) - Especificación del Protocolo de Red

## 1. Visión General del Protocolo de Comunicación

La comunicación en red local entre los nodos se realiza sobre **WebSocket (WSS / WS)** estándar en el puerto `8081`. La capa de aplicación transmite mensajes codificados en cadenas JSON formateadas con la siguiente estructura base de envoltorio (*Envelope Format*):

```json
{
  "type": "<MESSAGE_TYPE_IDENTIFIER>",
  "uuid": "<CLIENT_UUID_V4>",
  "timestamp": 1782391200000,
  "payload": { ... }
}
```

---

## 2. Catálogo de Mensajes del Protocolo

### 2.1 Mensajes de Registro e Identificación
* **`register_client`** (Cliente Android -> Servidor Rust):
  Transmite UUID inmutable, nombre visual, fabricante, modelo, versión de Android, resolución de pantalla y tipo de conexión.
* **`register_ack`** (Servidor Rust -> Cliente Android):
  Confirma el registro y entrega el snapshot inicial completo del `Workspace` asignado a ese `ClientUUID`.
* **`gui_ready`** (WinUI 3 -> Servidor Rust):
  Notifica al servidor que el editor gráfico de Windows está listo para recibir actualizaciones de la lista de clientes.

### 2.2 Mensajes de Sincronización y Edición
* **`clients_updated`** (Servidor Rust -> Todos los Editores):
  Emite la lista actualizada de todos los clientes registrados, sus datos de hardware y su estado de conexión (`online` / `offline`).
* **`get_workspace`** / `workspace_data`:
  Solicita y responde el Workspace correspondiente a un `uuid` específico.
* **`db_sync`** (WinUI 3 -> Servidor Rust -> Cliente Android):
  Transmite mutaciones de perfiles, páginas o botones para un `targetUuid`.
* **`rename_client`**:
  Modifica el nombre visual de un cliente identificado por su `uuid`.

### 2.3 Mensajes de Acción y Feedback
* **`action`** (Cliente Android -> Servidor Rust):
  Ejecuta una acción al presionar o soltar un botón táctil.
* **`action_error`** (Servidor Rust -> Cliente Android):
  Notifica el éxito o fallo de la ejecución de una acción.
