# Memoria del Proyecto - Stream Dock Pro (Servidor Rust y Editor de Escritorio)

Este documento registra la arquitectura, los componentes implementados y todos los avances realizados en el ecosistema de PC (`pc-companion-rust` y `pc-companion-winui`) junto con su sincronización con la app Android y la publicación en GitHub.

---

## 1. Arquitectura General y Empaquetado Unificado

1. **`pc-companion-rust/` (Servidor Backend Silencioso)**:
   - Motor asíncrono basado en **Tokio** y **Axum** (`ws://0.0.0.0:8081/ws`).
   - Compilado con `#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]` para ejecutarse 100% en segundo plano sin ventana CMD.
   - Incrustado como recurso (`EmbeddedResource`) dentro de `StreamDockPro.exe`, extraído y lanzado automáticamente por `App.xaml.cs` en `%LOCALAPPDATA%\StreamDockPro\server\pc-companion-rust.exe`.
   - Gestión de contextos aislados por `ClientUUID` mediante `ContextManager` y sincronización en tiempo real mediante canal `broadcast`.

2. **`pc-companion-winui/` (Editor Gráfico de Escritorio .NET 8)**:
   - Aplicación WPF .NET 8 con arquitectura MVVM (`CommunityToolkit.Mvvm`).
   - Empaquetada como ejecutable único `dist/StreamDockPro.exe` e instalador gráfico `dist/StreamDockPro_Instalador.exe` con icono oficial multi-capa (`app_icon.ico`).
   - Incluye monitor automático de USB ADB (`UsbAdbMonitor.cs`) que ejecuta `adb reverse tcp:8081 tcp:8081` al detectar dispositivos Android conectados por cable.

---

## 2. Funcionalidades e Implementaciones Realizadas

### A. Backend Servidor Core (`pc-companion-rust`)
- **Control Exacto de Volumen por Barras Verticales (`win32_input.rs`)**:
  - **Volumen Maestro del PC (`VOLUME_SET:<0..100>`, `PC_SET_VOL:<0..100>`, `VOLUME_SLIDER_PC:<0..100>`)**: Invoca directamente `IAudioEndpointVolume::SetMasterVolumeLevelScalar` (vtable slot 7) y `SetMute(0)` (vtable slot 14) de Windows Core Audio para fijar el nivel exacto de `0%` a `100%`.
  - **Volumen Exclusivo de Spotify (`SPOTIFY_SET_VOL:<0..100>`, `VOLUME_SLIDER_SPOTIFY:<0..100>`)**: Recorre las sesiones de audio activas (`IAudioSessionEnumerator`) e invoca `ISimpleAudioVolume::SetMasterVolume` únicamente sobre el proceso `Spotify.exe` sin alterar el volumen de juegos ni del resto del PC.
- **Protocolo de Sincronización (`db_sync`) y Compactación Automática**:
  - Soporte completo para sincronización de `Workspace`, `update_button` y `delete_button`.
  - Al eliminar un botón, reordena y compacta automáticamente los botones restantes (`0..N`) sin dejar huecos vacíos.
- **Sincronización de Iconos (`icon_manager.rs`)**:
  - Preservación de imágenes PNG en Base64 (`data:image/png;base64,...`) de alta resolución `256×256` y asignación automática de iconos vectoriales por tipo de acción.
- **Integración Nativa con OBS WebSocket v5 (`obs_websocket.rs`)**:
  - Autenticación SHA-256 (`salt` + `challenge`), escaneo de escenas (`GetSceneList`) y fuentes (`GetSceneItemList`), cambio de escena, visibilidad de fuentes, mute de micrófono y control de transmisión/grabación.

---

### B. Editor Gráfico de Escritorio (`pc-companion-winui`)
- **Espacio Dedicado de Barras Verticales de Volumen en el Perfil (`MainWindow.xaml`, `MainViewModel.cs`, `SpotifyController.cs`)**:
  - Panel **`🎚️ VOLUMEN`** a la derecha de la cuadrícula `4×5` del perfil activo.
  - Botones directos **`🔊 + Barra PC`** y **`🟢 + Spotify`** para agregar las barras deslizantes verticales al perfil en 1 clic (también disponibles en las categorías **`🎵 Audio PC`** y **`🟢 Spotify`**).
  - Sliders verticales (`0% - 100%`) sincronizados en tiempo real con Windows Core Audio (`IAudioEndpointVolume` e `ISimpleAudioVolume`), porcentaje en vivo y botón de silencio (`🔇`).
- **Rediseño de "TIPO DE ACCIÓN" en 8 Categorías Visuales**:
  - Matriz visual en 2 columnas para **`🎵 Audio PC`**, **`🟢 Spotify`**, **`🚀 Abrir App`**, **`🪟 Ventanas`**, **`🎥 OBS Studio`**, **`⌨️ Atajo Teclado`**, **`📄 Ir a Página`** y **`⚙️ Sistema PC`**, acompañada de vista previa en vivo y paleta rápida de 8 colores.
- **Grabador de Atajos en Vivo, Auto-Perfil, IP Local y Bandeja del Sistema**:
  - Grabación física de combinaciones de teclado (`IsRecordingHotkey`).
  - Detección automática del proceso en primer plano (`WindowController.GetForegroundProcessName`) para cambiar de perfil automáticamente (`AutoSwitchProcess`).
  - Detección y copia en 1 clic de la dirección IP LAN (`192.168.x.x:8081`).
  - Minimizado opcional a la bandeja del sistema de Windows (System Tray).
- **Extracción de Iconos Jumbo 256×256 (`IconExtractor.cs`)**:
  - Extracción nativa mediante `SHGetImageList` (`SHIL_JUMBO`) con recorte de márgenes transparentes y sincronización Base64 con Android.
