# 11. Sync Engine (`SyncEngine`) - Sincronización Delta y Tolerancia a Fallos

## 1. Visión General del Motor de Sincronización

El `SyncEngine` implementa el protocolo de sincronización reactivo en tiempo real entre el Editor WinUI 3, el Servidor Rust y los Clientes Android. Su diseño elimina la necesidad de transferir bases de datos completas tras cada cambio parcial.

---

## 2. Diagrama de Sincronización Basada en Deltas (*Delta Sync Flow*)

```
[WinUI 3 Editor]                       [Rust Server Core]                  [Android Client]
       |                                       |                                  |
       |-- (1) db_sync ----------------------->|                                  |
       |    (targetUuid, patch: ReplaceLabel)  |                                  |
       |                                       |-- (2) Apply Patch to Context --->|
       |                                       |-- (3) Increment Version Clock ->|
       |                                       |-- (4) Save to `workspaces/{uuid}`|
       |                                       |                                  |
       |                                       |-- (5) Forward Delta Payload ---->|
       |                                       |    (targetUuid, Delta Patch)     |
       |                                       |                                  |
       |<-- (6) clients_updated ---------------|                                  |<-- (7) Apply Patch to StateFlow
```

---

## 3. Algoritmo de Detección de Diferencias y Hashes de Estado

1. **Vector Clock & Sequence Version**: Cada `Workspace` posee un campo `version: u64` que se incrementa monótonamente con cada mutación aprobada.
2. **State Checksum (SHA-256)**: Cada payload de sincronización adjunta un hash SHA-256 parcial de la cuadrícula. Si el cliente detecta una inconsistencia en el hash de verificación (*Checksum Mismatch*), el cliente solicita automáticamente una sincronización completa (*Full Snapshot Resync*).
3. **Resolución de Conflictos (*Last-Write-Wins with Vector Verification*)**:
   * Si dos ediciones ocurren simultáneamente, el servidor Rust valida los timestamps lógicos y la firma del `ClientContext` para serializar la aplicación de parches en orden secuencial estricto.
