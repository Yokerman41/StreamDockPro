# Memoria del Proyecto - Stream Dock Pro (Android, Servidor Rust y Editor PC)

Este documento detalla la evolución, arquitectura, correcciones y funcionalidades implementadas en la aplicación Android (`app`), el servidor backend (`pc-companion-rust`) y el editor de escritorio (`pc-companion-winui`).

---

## 1. Correcciones de Estabilidad Inicial
- **Compilación de Compose**: Se eliminaron bloques `try-catch` alrededor de funciones composables en `ButtonTile.kt`, sustituyéndolos por lógica de estado recordada (`remember`).
- **Resolución de Ámbitos**: Se corrigieron errores de receptores implícitos en animaciones de visibilidad en `GridScreen.kt`.
- **Ajustes de Memoria**: Se aumentó la memoria del demonio de Gradle (`-Xmx4g`) para soportar la compilación de librerías extensas como `material-icons-extended`.

---

## 2. Interfaz de Usuario (UI), Experiencia (UX) y Modo Horizontal
### Sistema de Diseño (Atomic Design)
Se creó `AtomicComponents.kt` con componentes reutilizables que mantienen la estética "Obsidian/Dark":
- **`StyledInput`**: Campos de texto personalizados con bordes finos y labels minimalistas.
- **`PrimaryActionButton`**: Botones con esquinas redondeadas y colores vibrantes de acción.
- **`SectionHeader`**: Etiquetas de grupo en mayúsculas e índigo.

### Navegación Principal y Adaptabilidad Horizontal (Landscape)
- **`MainScreen`**: Detecta la orientación del dispositivo (`Configuration.ORIENTATION_LANDSCAPE`) y adapta dinámicamente la estructura:
  - **Modo Vertical (Portrait)**: `Scaffold` con `CommandHeader` superior y `BottomNavBar` inferior.
  - **Modo Horizontal (Landscape)**: `Row` con barra lateral izquierda (`SideNavRail` / `NavigationRail`) y `CommandHeader` compacto, liberando el alto completo de la pantalla para la cuadrícula de botones.
- **`BottomNavBar` / `SideNavRail`**: Navegación con 5 secciones (Panel, Aplicaciones, OBS, Streamer.bot y Configuración) que se transforma en riel vertical en modo horizontal.
- **`CommandHeader`**: Encabezado inteligente con modo compacto (`compact = true`) en horizontal y estado de conexión en tiempo real.
- **`GridScreen` y `ButtonTile` Adaptables**:
  - Transposición automática de dimensiones cuando `isLandscape && rows > cols` (por ejemplo, una página `4×3` pasa automáticamente a `3 filas × 4 columnas` en horizontal).
  - Cálculo dinámico de altura por celda (`tileHeight`) mediante `BoxWithConstraints` para garantizar que todas las filas de botones entren en pantalla sin recortes.
  - Escalado proporcional de iconos, tipografía y márgenes en `ButtonTile.kt`.
- **Scroll en Pantallas Secundarias**: `ConfigurationScreen.kt`, `OBSScreen.kt` y `StreamerBotScreen.kt` cuentan con desplazamiento vertical (`verticalScroll`) para operar cómodamente en pantallas horizontales.

---

## 3. Funcionalidades del Panel (Stream Dock)
### Gestión de Páginas (Perfiles)
- Creación de múltiples páginas de botones con dimensiones configurables (`gridRows` x `gridCols`, hasta `8×8`).
- **`PageNamingDialog`**: Sub-ventana modal para asignar nombre, filas y columnas a cada página.
- **`PageSelector`**: Barra horizontal de pestañas (con modo compacto en horizontal) para cambiar rápidamente entre perfiles.
- **Eliminación de Páginas**: Botón de papelera en el encabezado para eliminar la página activa (manteniendo siempre al menos una página por defecto).

### Configuración, Eliminación, Compactación y Reordenamiento de Botones
- **Eliminación de Botones (`deleteButton`)**:
  - En la **App Android (`ButtonConfigDialog.kt`)**: Botón de papelera rápido en la barra superior del diálogo y botón rojo **"ELIMINAR BOTÓN"** en el pie fijo del modal cuando el botón está configurado o seleccionado.
  - En el **Editor PC (`MainWindow.xaml` y `MainViewModel.cs`)**: Botón **"🗑️ Eliminar Botón"** y opción en menú contextual (clic derecho).
- **Compactación Automática (Sin espacios vacíos)**:
  - Tanto en Android (`StreamDockViewModel.kt`), como en el Editor PC (`MainViewModel.cs`) y en el Servidor Rust (`ws_server.rs`), al eliminar un botón todos los botones posteriores se recorren y reindexan automáticamente (`position = 0, 1, 2...`) para que nunca queden huecos vacíos entre botones.
- **Cambio de Posición al Mantener Presionado por 6 Segundos (Modo Mover)**:
  - **En Android (`ButtonTile.kt` y `GridScreen.kt`)**:
    - Mantener presionado entre **0.5 s y < 6 s** y soltar abre el diálogo de configuración/eliminación.
    - Mantener presionado por **6 segundos o más** activa el **Modo Mover** con vibración háptica, banner ámbar superior (`"MODO MOVER: Toca otro botón para cambiar de posición"`) y borde dorado en el botón origen. Al tocar cualquier otro botón, ambos intercambian su posición (`swapButtons`) y se sincronizan con el servidor y la PC.
  - **En el Editor PC (`MainWindow.xaml.cs` y `MainViewModel.cs`)**:
    - Mantener presionado un botón con el clic izquierdo por **6 segundos** (o desde el menú contextual) activa el **Modo Mover** (borde dorado `#F9E2AF`) y al hacer clic en otro botón intercambia sus posiciones.

---

## 4. Sincronización de Iconos en Alta Resolución (256×256 Jumbo)
- **Extracción de Iconos Jumbo en PC (`IconExtractor.cs`)**:
  - Integración con la API nativa de Windows Shell (`SHGetFileInfo` + `SHGetImageList` con `SHIL_JUMBO = 0x4` y `SHIL_EXTRALARGE = 0x2`) para obtener los iconos originales de Windows en **256×256 píxeles** de archivos `.exe`, accesos directos `.lnk` y accesos directos de juegos/Steam/Epic `.url`.
  - Recorte inteligente de márgenes transparentes (`TrimTransparentPadding`) cuando el icono real está centrado en un lienzo de 256×256.
  - Codificación en `data:image/png;base64,...` sincronizada automáticamente en el campo `icon_name`.
- **Sincronización desde el Servidor Rust (`icon_manager.rs` y `ws_server.rs`)**:
  - Enriquecimiento automático de iconos (`IconManager::enrich_button_icon`) al recibir actualizaciones de botones o workspaces completos, convirtiendo iconos de ejecutables o nombres de acción a Base64 PNG o iconos vectoriales compatibles.
- **Renderizado Nítido en Android (`IconMapper.kt` y `ButtonTile.kt`)**:
  - Decodificación de `data:image/png;base64,...` a `ImageBitmap` cacheado en memoria con `FilterQuality.High` y `ContentScale.Fit`, ocupando hasta el 62% del área del botón para máxima legibilidad.

---

## 5. Integración Completa con OBS Studio (OBS WebSocket v5)
- **Cliente OBS WebSocket v5 en Editor PC (`ObsWebSocketClient.cs`)**:
  - Implementación nativa del protocolo OBS WebSocket v5 (`OpCode 0 Hello` → `OpCode 1 Identify` con autenticación SHA-256 `salt + challenge` → `OpCode 2 Identified`).
  - Escaneo automático y manual (`ScanObsWebSocketCommand`) de:
    - **`GetSceneList`**: Todas las escenas de OBS Studio (`ObsScenes`).
    - **`GetSceneItemList`**: Todas las fuentes visuales dentro de cada escena para mostrar/ocultar fuentes (`SOURCE_TOGGLE:Escena|Fuente`).
    - **`GetInputList`**: Todas las entradas y canales de audio para silenciar/activar (`MUTE_INPUT:Fuente`).
  - Interfaz en `MainWindow.xaml` con tarjetas interactivas para **Escenas Detectadas** y **Fuentes y Entradas Detectadas**.
- **Cliente OBS WebSocket v5 en Servidor Rust (`obs_websocket.rs` y `ws_server.rs`)**:
  - Sincronización automática de credenciales (`obs_config`: Host, Puerto y Contraseña) desde el Editor PC al Servidor Rust.
  - Consulta en vivo de escenas y fuentes desde la app Android (`obs_get_scenes` → `obs_scenes_list`).
  - Ejecución directa sobre OBS Studio de cambio de escena (`SetCurrentProgramScene`), visibilidad de fuentes (`SetSceneItemEnabled`), mute de audio (`ToggleInputMute`), grabación (`ToggleRecord`) y transmisión (`ToggleStream`).

---

## 6. Gestión de Datos, Red y Herramientas de Despliegue
- **Base de Datos Local (Room)**: Persistencia completa de `workspaces`, `profiles` y `buttons` con sincronización bidireccional (`db_sync`) y resolución de conflictos con el servidor Rust.
- **Pruebas Unitarias (`ProtocolCompatibilityTest.kt` y `workspace_engine.rs`)**:
  - Suite de pruebas verificando serialización JSON `snake_case`, sincronización de iconos Base64 y compatibilidad de protocolo entre Android y Rust.
- **Script de Instalación Automática (`instalar_app.bat`)**:
  - Script por lotes que detecta ADB, compila el APK debug (`gradlew assembleDebug`), configura `adb reverse tcp:8081 tcp:8081` para conexión USB, instala el APK (`adb install -r -t`) e inicia `com.streamdock.pro/.MainActivity`.
