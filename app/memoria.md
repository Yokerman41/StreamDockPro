# Memoria del Proyecto - Stream Dock Pro (Android, Servidor Rust, Editor PC y Web Oficial)

Este documento detalla la evolución, arquitectura, correcciones y todas las funcionalidades implementadas en la aplicación Android (`app`), el servidor backend (`pc-companion-rust`), el editor de escritorio (`pc-companion-winui`), el sistema de empaquetado e instalador único y la página web oficial (`index.html`).

---

## 1. Correcciones de Estabilidad Inicial
- **Compilación de Compose**: Se eliminaron bloques `try-catch` alrededor de funciones composables en `ButtonTile.kt`, sustituyéndolos por lógica de estado recordada (`remember`).
- **Resolución de Ámbitos**: Se corrigieron errores de receptores implícitos en animaciones de visibilidad en `GridScreen.kt`.
- **Ajustes de Memoria**: Se aumentó la memoria del demonio de Gradle (`-Xmx4g`) para soportar la compilación de librerías extensas como `material-icons-extended`.

---

## 2. Interfaz de Usuario (UI) Moderna, Experiencia (UX) y Adaptabilidad Total
### Sistema de Diseño (Atomic Design & Cyber-Deck UI)
- **`AtomicComponents.kt`**: Componentes reutilizables (`StyledInput`, `PrimaryActionButton`, `SectionHeader`) con estética "Cyber-Obsidian / Stream Deck".
- **Teclas con Relieve 3D Tactile Keycaps (`ButtonTile.kt`)**:
  - Superficie biselada con brillo superior especular, indicador LED inferior de estado activo, respuesta elástica al presionar (`animateFloatAsState` con resorte) y retroalimentación háptica (`HapticFeedbackType`).
  - En modo normal, las casillas vacías se muestran como paneles oscuros discretos; en **Modo Edición (`✏️ EDITAR`)**, se iluminan con un botón `+` para configurarlas en 1 toque.

### Navegación Simplificada y Adaptabilidad Multi-Pantalla (`MainScreen.kt`, `BottomNavBar.kt`, `CommandHeader.kt`)
- **4 Destinos Principales Claros**:
  1. **`Panel Deck`**: Cuadrícula interactiva de perfiles + Espacio de Barras Verticales de Volumen.
  2. **`Quick Deck`**: Centro rápido de ventanas, volumen maestro, Spotify exclusivo y energía del PC.
  3. **`OBS Studio`**: Centro de mando en vivo con matriz adaptable de escenas y fuentes.
  4. **`Conexión`**: Estado de red, dirección IP de la PC con chips de 1 toque y credenciales de OBS / Streamer.bot.
- **Adaptación Automática a Teléfonos, Tablets, Vertical y Horizontal**:
  - **Modo Vertical (Portrait)**: `Scaffold` con `CommandHeader` superior y `BottomNavBar` inferior.
  - **Modo Horizontal (Landscape)**: `Row` con riel lateral izquierdo (`SideNavRail`) y `CommandHeader` compacto, liberando el 100% del alto para las teclas y los faders de volumen.
  - **Transposición Inteligente de Cuadrícula**: Cuando el dispositivo está en horizontal (`isLandscape && rawRows > rawCols`), las cuadrículas verticales (como `5×4` o `4×3`) se rotan automáticamente a `4×5` o `3×4`.
  - **Modo Consola a Pantalla Completa (`🔲`)**: Botón en el encabezado que oculta las barras de navegación para dejar exclusivamente el panel de botones y las barras de volumen ocupando toda la pantalla.
  - **Pantalla Siempre Activa (`FLAG_KEEP_SCREEN_ON`)**: Evita que el teléfono o tablet se bloquee mientras se usa como consola sobre el escritorio.

---

## 3. Barras Deslizantes Verticales de Volumen y Espacio de Volumen en el Perfil (`0% - 100%`)
Tanto en el **Editor de PC** como en la **App de Android**, el control de volumen para **Volumen Maestro del PC** y **Spotify Exclusivo** se puede agregar como **barras deslizantes verticales (faders)** para subir o bajar el nivel deslizando de arriba a abajo (`100%` arriba → `0%` abajo):

### A. Espacio Dedicado de Volumen dentro del Perfil (`🎚️ VOLUMEN`)
- **En el Editor de PC (`MainWindow.xaml` y `MainViewModel.cs`)**:
  - A la derecha del lienzo `4×5` del perfil se encuentra el espacio **`🎚️ VOLUMEN`**.
  - Permite activar con 1 clic **`🔊 + Barra PC`** (`VOLUME_SLIDER_PC`) y/o **`🟢 + Spotify`** (`VOLUME_SLIDER_SPOTIFY`), o elegirlos desde la sección **`TIPO DE ACCIÓN`** → **`🎵 Audio PC`** y **`🟢 Spotify`**.
  - Muestra sliders verticales (`Orientation="Vertical" Minimum="0" Maximum="100"`) con porcentaje en vivo, botón de silencio rápido (`🔇`) y botón (`✕`) para quitar la barra del perfil.
- **En la App Android (`GridScreen.kt`, `ButtonTile.kt` y `ButtonConfigDialog.kt`)**:
  - **Activación en 1 Toque desde el Perfil**: En la barra superior de páginas (`PageSelector`), junto a los perfiles y `+ Página`, se incluyen los botones **`🎚️ +Vol PC`** y **`🟢 +Spotify`** para agregar u ocultar las barras verticales en el perfil activo al instante.
  - **Dock Lateral de Faders Verticales (`ProfileVolumeFaderDock` y `VerticalVolumeFaderBar`)**: Cuando el perfil tiene activada la barra de PC, la de Spotify o ambas, se despliega a la derecha de la botonera un espacio dedicado con pistas táctiles verticales, escala graduada, relleno con degradado luminoso en tiempo real, indicador `thumb` y botón `🔇` de silencio.
  - **Deslizamiento Directo sobre el Botón (`ButtonTile.kt`)**: Los botones configurados con `VOLUME_SLIDER_PC` o `VOLUME_SLIDER_SPOTIFY` también muestran una barra vertical interna de nivel y permiten deslizar el dedo de arriba a abajo directamente sobre la tecla.

### B. Control Exacto de Audio en Windows Core Audio (`0%` a `100%`)
- **Servidor Rust (`pc-companion-rust/src/plugins/win32_input.rs`)**:
  - Soporta `VOLUME_SET:<0..100>`, `PC_SET_VOL:<0..100>` y `VOLUME_SLIDER_PC:<0..100>` ajustando el volumen maestro de Windows mediante COM `IAudioEndpointVolume::SetMasterVolumeLevelScalar` (y desmuteando automáticamente).
  - Soporta `SPOTIFY_SET_VOL:<0..100>` y `VOLUME_SLIDER_SPOTIFY:<0..100>` ajustando exclusivamente la sesión de audio de `Spotify.exe` mediante `ISimpleAudioVolume::SetMasterVolume` sin alterar el volumen de juegos ni del resto del sistema.
- **Servicio de Audio en Editor PC (`SpotifyController.cs`)**:
  - Interop COM con `IAudioEndpointVolume` (`GetMasterVolumePercent` / `SetMasterVolumePercent`) e `ISimpleAudioVolume` (`GetSpotifyVolumePercent` / `SetSpotifyVolumeScalar`) para sincronizar y aplicar en tiempo real el porcentaje de las barras verticales en Windows.

---

## 4. Rediseño de "TIPO DE ACCIÓN" en 8 Categorías Visuales (PC y Android)
Tanto en el panel derecho del **Editor de PC (`MainWindow.xaml`)** como en el modal de configuración de la **App Android (`ButtonConfigDialog.kt`)**, la configuración de botones se organiza en **3 zonas sin scroll innecesario** y **8 categorías claras**:
1. **Zona 1 (Cabecera Compacta)**: Mini vista previa en vivo del botón + campo de etiqueta + **Paleta de 8 Colores en 1 Clic**.
2. **Zona 2 (8 Categorías Visuales + Catálogo de Presets en 2 Columnas)**:
   - **`🎵 1. Audio PC`**: Barra Deslizante de Volumen PC (`VOLUME_SLIDER_PC`), Play/Pausa global, Silenciar PC, Subir/Bajar Volumen, Pista Anterior/Siguiente.
   - **`🟢 2. Spotify Exclusivo`**: Barra Deslizante de Volumen Spotify (`VOLUME_SLIDER_SPOTIFY`), Play/Pausa en 2do plano, Abrir Spotify, Anterior, Siguiente, Spotify Vol+, Spotify Vol-, Silenciar solo Spotify.
   - **`🚀 3. Abrir Aplicación`**: Selector de programas (`.exe`), accesos directos (`.lnk`) o arrastrar y soltar desde el Escritorio con extracción automática de icono 256×256.
   - **`🪟 4. Gestión de Ventanas`**: Minimizar ventana activa (`WIN_MINIMIZE`), Maximizar/Restaurar (`WIN_MAXIMIZE`), Cerrar ventana (`WIN_CLOSE`) y Mostrar Escritorio (`SHOW_DESKTOP`).
   - **`🎥 5. OBS Studio`**: Auto-escaneo en 1 clic de escenas y fuentes por WebSocket v5, Stream, Grabar y Mute Mic OBS.
   - **`⌨️ 6. Atajo de Teclado`**: **Grabador Interactivo de Teclas (`🎹 Grabar Combinación`)** + presets listos (`Ctrl+Shift+M` Mute Discord, `Win+Shift+S` Captura, Copiar, Pegar, `Alt+Tab`, `Alt+F4`).
   - **`📄 7. Ir a Página`**: Selector directo de perfiles/páginas para crear botones de navegación entre pantallas.
   - **`⚙️ 8. Sistema PC`**: Bloquear PC (`Win+L`), Suspender, Reiniciar y Apagar PC.
3. **Zona 3 (Barra Fija de Acciones)**: Botones siempre visibles para **Probar**, **Guardar** y **Eliminar**.

---

## 5. Mejoras Avanzadas de Productividad en Editor PC y App Android
1. **Detección y Copia de IP Local en 1 Clic (`MainViewModel.cs` / `MainWindow.xaml`)**:
   - El encabezado del Editor de PC detecta automáticamente la IP LAN real (`192.168.x.x:8081`) y permite copiarla al portapapeles con un clic para vincular dispositivos por Wi-Fi sin abrir `cmd` ni `ipconfig`.
2. **Cambio Automático de Perfil según Juego o Programa Activo (`AutoSwitchProcess`)**:
   - Monitor en segundo plano (`DispatcherTimer` cada 1.5s + `WindowController.GetForegroundProcessName()`) que detecta qué aplicación está en primer plano en Windows (ej. `obs64.exe`, `Spotify.exe`) y cambia automáticamente el perfil activo tanto en PC como en el teléfono Android.
3. **Drag & Drop y Modo Edición Rápida (`✏️ EDITAR`)**:
   - En PC: Arrastrar y soltar botones para reordenarlos instantáneamente o soltar archivos `.exe` / `.lnk` desde Windows sobre cualquier celda.
   - En Android: Interruptor **`✏️ EDITAR`** en la barra superior para abrir o mover cualquier botón con un solo toque, además del gesto clásico de pulsación larga.
4. **Minimizado a la Bandeja del Sistema (System Tray)**:
   - Icono nativo en la bandeja del sistema de Windows con menú contextual (*Abrir Stream Dock Pro*, *Reiniciar Servidor Rust*, *Salir Completamente*).

---

## 6. Iconos Oficiales, Ejecutable Único, Instalador y Página Web en GitHub
- **Icono Oficial Vectorial y Multi-Capa (`app_icon.ico` y Adaptive Icon Android)**:
  - Icono `.ICO` de alta resolución (`256×256`, `64×64`, `48×48`, `32×32`, `16×16`) incrustado en el ejecutable de PC, ventana WPF, bandeja del sistema e instalador.
  - Adaptive Icon completo en Android (`ic_launcher.xml`, `ic_launcher_round.xml`, `ic_launcher_foreground.xml`, `ic_launcher_background.xml`, `ic_launcher_monochrome.xml`).
- **Ejecutable Único e Instalador Gráfico de Windows (`crear_ejecutable_pc.bat`)**:
  - Compila `pc-companion-rust.exe` en modo Release sin ventana de consola (`#![windows_subsystem = "windows"]`).
  - Incrusta el servidor Rust dentro de `StreamDockPro.exe` (`EmbeddedResource`) y lo extrae/ejecuta automáticamente en `%LOCALAPPDATA%\StreamDockPro\server\pc-companion-rust.exe`.
  - Genera el instalador gráfico en un solo archivo `dist/StreamDockPro_Instalador.exe` (y copia automática al Escritorio).
- **APK Firmado de Android (`crear_apk_android.bat`)**:
  - Compila y exporta `dist/Android/StreamDockPro.apk` (y copia en el Escritorio) e instala directamente por ADB si hay un dispositivo conectado.
- **Página Web Oficial Interactiva (`index.html` y GitHub Pages)**:
  - Landing page moderna en `index.html` con simulador interactivo en vivo de la consola Stream Dock y de las barras deslizantes verticales de volumen (`🔊 PC` y `🟢 SPO`), enlaces de descarga directa y flujo de publicación automática en `.github/workflows/pages.yml`.

---

## 7. Administrador de Tareas de Windows Integrado en la App Android (`TaskManagerScreen.kt`)
- **Ubicación en Navegación**: Integrado como pestaña intermedia entre **`OBS Studio`** y **`Conexión`** (`Panel Deck` | `Quick Deck` | `OBS Studio` | `Tareas` | `Conexión`).
- **Visualización en Tiempo Real**: Lista dinámica de todos los procesos y programas en ejecución en Windows, con diferenciación automática entre aplicaciones con ventana abierta y procesos de fondo.
- **Información Detallada por Proceso**: Título real de la ventana activa, nombre del ejecutable (`.exe`), PID de Windows y memoria RAM en Megabytes (`MB`) con badges cromáticos.
- **Búsqueda y Filtros**: Buscador instantáneo por nombre/PID/título y filtros de 1 toque (`Apps con Ventana` vs `Todos los Procesos`).
- **Finalización Remota de Procesos**: Botón `Finalizar` que invoca `kill_process` en el servidor Rust mediante `OpenProcess` + `TerminateProcess` / `taskkill /F /PID`, con diálogo de confirmación de seguridad.
- **Optimización Integral para Modo Horizontal (Landscape)**:
  - **Scroll Unificado Natural**: La cabecera, la barra de búsqueda y los filtros de selección se integraron directamente dentro del contenedor desplazable (`LazyColumn`). Al deslizar hacia arriba o hacia abajo, estos elementos se mueven suavemente con la lista en lugar de quedar fijos, liberando el 100% de la altura de la pantalla para visualizar los procesos.
  - **Buscador y Filtros en Fila Compacta**: En orientación horizontal, la barra de búsqueda y los botones de filtro se organizan lado a lado en una sola fila (`Row(height = 44.dp)`), ahorrando más del 50% del espacio vertical.
  - **Cuadrícula de 2 Columnas Paralelas**: Los procesos se agrupan en pares (`chunked(2)`) ocupando dos columnas balanceadas en pantallas panorámicas, lo que permite visualizar entre 6 y 10 procesos simultáneamente sin recortes ni saturación.
