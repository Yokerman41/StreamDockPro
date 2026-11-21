# 00. Visión General del Proyecto y Especificaciones de Arquitectura

## 1. Resumen Ejecutivo
Plataforma profesional de control remoto, automatización de escritorio y superficie táctil interactiva multi-dispositivo de alto rendimiento (equivalente a soluciones como Stream Deck, Touch Portal y Unified Remote). El sistema está diseñado sobre una arquitectura distribuida **Cliente-Servidor asíncrona**, desacoplada por eventos y estrictamente guiada por **Aislamiento de Contexto de Cliente (`ClientContext`)**.

La arquitectura prohíbe categóricamente el uso de estados globales o compartidos entre consolas (`CurrentWorkspace`, `Singleton Workspace`, etc.). Cada dispositivo físico o instancia de la aplicación Android opera como una entidad soberana e independiente administrada por un único Servidor Rust concurrente de alta disponibilidad y visualizada mediante un Editor GUI profesional en Windows (WinUI 3).

---

## 2. Principios Fundamentales de Diseño

### 2.1 Aislamiento Estricto por `ClientContext`
* **Cero Estado Global**: No existe el concepto de "Workspace global" o "Workspace activo en el servidor".
* **Soberanía por Hardware / Instancia**: Toda solicitud, comando, actualización de perfil, botón, variable o recurso está obligatoriamente encapsulado dentro del ámbito (`scope`) de un `ClientContext` asociado a un `ClientUUID` permanente.
* **Invariante de Contexto**: La modificación de un elemento dentro del `ClientContext` del Dispositivo $A$ no genera efecto colateral ni mutación en el `ClientContext` del Dispositivo $B$.

### 2.2 Identidad Inmutable de Cliente
* **Generación de UUID Permanente**: Toda instalación de la aplicación Android genera un Identificador Único Universal (UUID v4) durante su primera ejecución, guardado de forma inmutable en almacenamiento persistente (`SharedPreferences`).
* **Metadatos Visuales Separados**: El nombre del dispositivo (`Display Name`) es un mero atributo decorativo y modificable por el usuario. La ruteación interna, direccionamiento y persistencia utilizan única y exclusivamente el `ClientUUID`.

### 2.3 Orientación a Eventos y Delta-Sync
* **Bajo Ancho de Banda**: La transmisión entre Servidor y Clientes no envía copias completas de la base de datos tras cada interacción. Se emplea un motor de diferencias finas (*Delta Differences Sync Engine*) basado en hashes SHA-256 de estado y operaciones atómicas (*JSON Patch / Event Sourcing*).
* **Consistencia Eventual Fuerte**: Mecanismo de resolución de conflictos guiado por vectores de versión (*Vector Clocks*) y estampas de tiempo lógicas, garantizando recuperación automática frente a desconexiones.

---

## 3. Matriz de Componentes del Sistema

```
+-----------------------------------------------------------------------------------+
|                                 SISTEMA PLATAFORMA                                |
+------------------------------------+----------------------------------------------+
| COMPONENTE                         | TECNOLOGÍA / ROL                             |
+------------------------------------+----------------------------------------------+
| 1. Servidor Núcleo (Core Server)   | Rust (Axum, Tokio, Serde, DashMap)           |
|                                    | Servidor Concurrente, Bus de Eventos, Sync   |
+------------------------------------+----------------------------------------------+
| 2. Editor Desktop GUI              | C# / WinUI 3 (WPF / Windows App SDK)         |
|                                    | UI Múltiple ClientContext, Drag & Drop       |
+------------------------------------+----------------------------------------------+
| 3. Cliente Móvil / Tablet          | Kotlin / Android SDK (Coroutines, StateFlow) |
|                                    | Cuadrículas dinámicas, hálito y render       |
+------------------------------------+----------------------------------------------+
| 4. Motor de Transporte             | WebSocket (TCP 8081) / ADB Reverse (USB)     |
|                                    | Canales duplex cifrados/frameados            |
+------------------------------------+----------------------------------------------+
| 5. Almacenamiento Persistente      | NoSQL JSON por UUID -> Abstracción SQL ready |
|                                    | Isolating `workspaces/{uuid}.json`           |
+------------------------------------+----------------------------------------------+
```

---

## 4. Diagrama de Bloques Arquitectónicos Generales

```
+-----------------------------------------------------------------------------------+
|                                   WINUI 3 EDITOR                                  |
|  +-----------------------------------------------------------------------------+  |
|  | Dispositivos Conectados: [Client A (Redmi)]  [Client B (Tablet)]           |  |
|  | Context Selector -> Active ClientContext: UUID-AAA-111                      |  |
|  +-----------------------------------------------------------------------------+  |
+------------------------------------------+----------------------------------------+
                                           |
                              WebSocket IPC / IPC Local
                                           |
+------------------------------------------v----------------------------------------+
|                                SERVIDORES RUST CORE                               |
|  +-----------------------------------------------------------------------------+  |
|  |                       CLIENT CONTEXT MANAGER ENGINE                         |  |
|  |  +--------------------------------+   +----------------------------------+  |  |
|  |  | ClientContext (UUID-AAA-111)   |   | ClientContext (UUID-BBB-222)     |  |  |
|  |  | - Workspace AAA                |   | - Workspace BBB                  |  |  |
|  |  | - Session State / Channel      |   | - Session State / Channel        |  |  |
|  |  +--------------------------------+   +----------------------------------+  |  |
|  +-----------------------------------------------------------------------------+  |
|  | Event Bus | Storage Router | Sync & Delta Engine | OS Integrations (Win32)   |  |
+----------------------+-----------------------------------+------------------------+
                       |                                   |
              WebSocket (Wi-Fi)                     ADB Reverse (USB Cable)
                       |                                   |
+----------------------v-------------------+ +-------------v------------------------+
|    CLIENTE ANDROID A (UUID-AAA-111)      | |    CLIENTE ANDROID B (UUID-BBB-222)  |
| - Renderizador 4x5 Grid / Paginas        | | - Renderizador 6x8 Grid / Paginas    |
| - StateFlow Reactivo Local              | | - StateFlow Reactivo Local          |
+------------------------------------------+ +--------------------------------------+
```

---

## 5. Garantías de Escalabilidad y Tolerancia a Fallos
1. **Soporte Concurrente de Escala**: Capaz de manejar más de 500 conexiones simultáneas sin degradación de latencia ($<5\text{ ms}$ en red local / USB).
2. **Cero Puntos Únicos de Fallo en Contexto**: La corrupción de datos de un cliente no invalida la ejecución de los contextos adyacentes.
3. **Pluggability Total**: Todos los módulos de acciones (OBS, Streamer.bot, Teclado/Ratón, Ejecución de Procesos, Audio) son plugins desacoplados registrados dinámicamente en el Bus de Eventos.
