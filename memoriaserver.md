# Memoria del Proyecto - Stream Dock Pro (Servidor Rust y Editor de Escritorio)

Este documento registra la arquitectura, los componentes implementados y todos los avances realizados en el ecosistema de PC (`pc-companion-rust` y `pc-companion-winui`) junto con su sincronización con la app Android.

---

## 1. Arquitectura General y Estructura del Proyecto

1. **`pc-companion-rust/` (Servidor Backend)**:
   - Motor asíncrono basado en **Tokio** y **Axum** (`ws://0.0.0.0:8081/ws`).
   - Gestión de contextos aislados por `ClientUUID` mediante `ContextManager`.
   - Sincronización en tiempo real mediante canal `broadcast`.

2. **`pc-companion-winui/` (Editor Gráfico de Escritorio)**:
   - Aplicación WPF .NET 8 con arquitectura MVVM (`CommunityToolkit.Mvvm`).
   - Gestión visual de perfiles, cuadrícula de botones, extracción de iconos en alta resolución e integración con OBS Studio.

---

## 2. Funcionalidades e Implementaciones Realizadas

### A. Backend Servidor Core (`pc-companion-rust`)
- **Protocolo de Sincronización (`db_sync`)**:
  - Soporte completo para sincronización de `Workspace` completo y actualización/eliminación de botones individuales (`update_button` / `delete_button`).
  - Compatibilidad con dimensiones de cuadrícula personalizadas por perfil (`grid_rows`, `grid_cols`).
- **Eliminación y Compactación Automática de Botones**:
  - Al eliminar un botón desde Android o PC, el servidor filtra los botones configurados del perfil, los reordena y reasigna sus posiciones (`0..N`) sin dejar huecos vacíos, retransmitiendo el `Workspace` compactado a todos los clientes conectados.
- **Sincronización de Iconos (`icon_manager.rs`)**:
  - Enriquecimiento automático de iconos (`IconManager::enrich_button_icon`) preservando imágenes PNG en Base64 (`data:image/png;base64,...`) y asignando iconos por defecto según el tipo de acción cuando no se especifica uno.
- **Integración Nativa con OBS WebSocket v5 (`obs_websocket.rs`)**:
  - Cliente WebSocket v5 asíncrono sobre `tokio::net::TcpStream` con autenticación SHA-256 (`salt` + `challenge`).
  - Soporte para los mensajes `obs_config` (actualización de Host, Puerto y Contraseña) y `obs_get_scenes` (consulta real de escenas `GetSceneList` y fuentes `GetSceneItemList`).
  - Ejecución directa de acciones en OBS Studio: cambio de escenas (`SetCurrentProgramScene`), mostrar/ocultar fuentes (`SetSceneItemEnabled` con `SOURCE_TOGGLE:Escena|Fuente`), silenciar entradas de audio (`ToggleInputMute` con `MUTE_INPUT:Fuente` o `TOGGLE_MUTE`), grabación (`ToggleRecord`) y stream (`ToggleStream`).
- **Descubrimiento de Red Dual (mDNS + UDP Broadcast)**:
  - Anuncio `_streamdock._tcp` con registros PTR, SRV, TXT y `A` (IPv4) y beacon UDP en el puerto `8082`.

---

### B. Editor Gráfico de Escritorio (`pc-companion-winui`)
- **Extracción de Iconos en Alta Resolución 256×256 (`IconExtractor.cs`)**:
  - Extracción nativa mediante `SHGetImageList` (`SHIL_JUMBO` 256×256 y `SHIL_EXTRALARGE` 48×48) para archivos `.exe`, accesos directos `.lnk` y accesos directos `.url` (juegos de Steam/Epic/etc.).
  - Recorte inteligente de bordes transparentes (`TrimTransparentPadding`) y serialización automática en PNG Base64 (`data:image/png;base64,...`) para que los iconos se vean grandes y nítidos tanto en la PC como en la app Android.
- **Eliminación, Compactación y Reordenamiento por Pulsación de 6s**:
  - Eliminación de botones desde el panel lateral o clic derecho, compactando automáticamente los botones restantes de la página para no dejar espacios vacíos.
  - **Modo Mover (6 segundos)**: Al mantener presionado un botón durante 6 segundos en la cuadrícula (o mediante clic derecho → Mover), el botón entra en modo de traslado (borde dorado) e intercambia su posición al hacer clic sobre el botón destino.
- **Escáner Real de Escenas y Fuentes de OBS (`ObsWebSocketClient.cs` y `MainWindow.xaml`)**:
  - Conexión directa a OBS WebSocket v5 con soporte de contraseña.
  - Escaneo de todas las **Escenas** (`GetSceneList`), **Fuentes por escena** (`GetSceneItemList`) y **Canales de audio** (`GetInputList`).
  - Tarjetas interactivas en la interfaz para asignar con un clic cualquier escena, fuente visual (mostrar/ocultar) o entrada de audio (mute/unmute) al botón seleccionado.
