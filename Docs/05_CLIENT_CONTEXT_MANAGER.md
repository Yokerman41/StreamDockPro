# 05. Client Context Manager (`ClientContextManager`) - Arquitectura del Núcleo

## 1. Visión General del Gestor de Contextos de Cliente

El **`ClientContextManager`** es la abstracción arquitectónica más importante del sistema. Es el módulo encargado de garantizar la regla de oro: **Cero Estado Global y Aislamiento Absoluto por Cliente**.

---

## 2. Anatomía Completa del `ClientContext`

Cada instancia de `ClientContext` encapsula todos los subsistemas requeridos por un cliente individual:

```
+-----------------------------------------------------------------------------------+
|                            CLIENT CONTEXT (UUID-AAA-111)                          |
+-----------------------------------------------------------------------------------+
|  1. Identidad: ClientUUID (Inmutable), DisplayName (Mutable), HardwareSpecs       |
|  2. Workspace: Instancia aislada de `Workspace` (Profiles, Pages, Buttons, etc.) |
|  3. Conexión: SocketAddr, ConnectionState (ONLINE/OFFLINE), TransportType         |
|  4. Reactividad: Async Channel Tx (MPSC), Event Stream Listener                   |
|  5. Versión & Sync: Version Clock, LastSyncTimestamp, Checksum Hash               |
|  6. Caché Local: LRU Image Bitmap Cache, Resolved Actions Table                   |
|  7. Historial & Audit: Event Log, Undo/Redo Queue                                 |
+-----------------------------------------------------------------------------------+
```

---

## 3. Diagrama de Flujo del Router de Contextos

```
   [Trama WebSocket Recibida en Servidor Rust]
                      |
                      v
          Extraer `UUID` o `targetUuid`
                      |
                      v
      ¿Existe `UUID` en `DashMap` del Router?
             /                  \
          (Sí)                  (No)
           /                      \
Obtener `Arc<ClientContext>`     Cargar de Disco `workspaces/{uuid}.json`
Ejecutar Operación en Contexto   Si no existe, instanciar Contexto Inicial
           \                      /
            v                    v
     Retornar Resultado / Transmitir Delta Sync
```

---

## 4. Reglas de Invariancia y Restricciones Estrictas
1. **Acceso Concurrente Seguro**: Las escrituras sobre un `ClientContext` utilizan cerrojos granulares por cliente (`RwLock` dentro del contexto específico). Dos operaciones sobre clientes diferentes se ejecutan en paralelo en hilos de CPU independientes sin contención (*Lock Contention = 0*).
2. **Ciclo de Vida de Contextos Desconectados**: Cuando una consola Android se desconecta, su `ClientContext` conmuta su estado a `OFFLINE`, pero permanece en memoria y guardado en disco. El editor WinUI 3 puede continuar editando el `ClientContext` de un dispositivo offline; al reconectarse la consola Android, el motor de sincronización resolverá y aplicará los cambios acumulados.
