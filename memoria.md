# Memoria General del Proyecto - Stream Dock Pro

Este archivo consolida toda la memoria técnica, mejoras de interfaz, controles de audio vertical, empaquetado y despliegue web de **Stream Dock Pro** (Servidor Rust + Editor de PC + App Android + Página Web Oficial).

---

## 1. Resumen de Componentes del Ecosistema

| Componente | Ruta Principal | Tecnología | Binario / Salida |
| :--- | :--- | :--- | :--- |
| **Servidor Core PC** | `pc-companion-rust/` | Rust (`Tokio`, `Axum`, `DashMap`, `Win32`, `CoreAudio`) | Incrustado dentro de `StreamDockPro.exe` |
| **Editor Gráfico PC** | `pc-companion-winui/` | C# / WPF (.NET 8, MVVM, COM Audio & Shell Interop) | `dist/StreamDockPro.exe` y `dist/StreamDockPro_Instalador.exe` |
| **Aplicación Android** | `app/` | Kotlin + Jetpack Compose + Room + OkHttp WebSocket | `dist/Android/StreamDockPro.apk` |
| **Página Web Oficial** | `index.html` | HTML5 + CSS3 Cyber-Deck + Simulador JS en Vivo | Local (`index.html`) y GitHub Pages (`.github/workflows/pages.yml`) |

---

## 2. Registro Cronológico de Implementaciones

### Fase 1: Iconos Oficiales, Ejecutable Único (Sin CMD) e Instalador
1. **Generación de Icono Oficial `.ICO` Multi-Capa (256×256 a 16×16)**:
   - Diseño tipo consola Stream Deck de 9 teclas luminosas sobre chasis oscuro (`pc-companion-winui/StreamDockWinUI/app_icon.ico` y `dist/StreamDockPro.ico`).
   - Aplicado al ejecutable principal (`ApplicationIcon` en `StreamDockWinUI.csproj`), a la ventana principal (`MainWindow.xaml`) y al instalador gráfico (`StreamDockPro_Instalador.exe`).
2. **Icono Adaptativo Oficial para Android (API 26+ y API 33+ Monocromo)**:
   - Implementados `ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, `ic_launcher.xml` e `ic_launcher_round.xml`.
3. **Unificación de Servidor Rust + Editor PC en 1 Solo `.EXE`**:
   - `pc-companion-rust` configurado con `#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]` para ejecutarse 100% en segundo plano sin abrir ventana CMD.
   - `App.xaml.cs` extrae automáticamente el recurso embebido `pc-companion-rust.exe` en `%LOCALAPPDATA%\StreamDockPro\server\` y lo inicia silenciosamente al abrir el Editor.

---

### Fase 2: Las 5 Mejoras de Interfaz y Facilidad de Uso (PC y Android)
1. **Tarjetas de Red e IP Local con Copia en 1 Clic**:
   - El Editor de PC muestra la IP local real (`192.168.x.x:8081`) en la barra superior con botón para copiarla en 1 clic.
   - La pestaña **Conexión** de la App Android guarda historial de IPs recientes como chips interactivos de conexión en 1 toque.
2. **Grabador Interactivo de Atajos de Teclado (`🎹 Grabar Combinación`)**:
   - Permite presionar físicamente cualquier combinación en el teclado (ej. `CTRL+SHIFT+M`, `ALT+F1`, `WIN+SHIFT+S`) y la asigna automáticamente al botón sin escribir códigos manualmente.
3. **Cambio Automático de Perfil según Juego/App Activa (`Auto-Perfil`)**:
   - Cada perfil permite asociar un ejecutable (ej. `obs64.exe`, `Spotify.exe`). Cuando esa ventana pasa a primer plano en Windows, el Editor cambia automáticamente el perfil activo en PC y Android.
4. **Reordenamiento por Drag & Drop y Modo Edición en 1 Toque (`✏️ EDITAR`)**:
   - En PC: Arrastrar y soltar botones entre casillas o arrastrar accesos directos desde el Escritorio.
   - En Android: Botón **`✏️ EDITAR`** en la cabecera para configurar o mover botones con un toque sin esperar pulsaciones largas.
5. **Bandeja del Sistema en Windows y Modo Consola Siempre Activo en Android**:
   - Opción de minimizar el Editor de PC al System Tray y `FLAG_KEEP_SCREEN_ON` en Android para que la pantalla nunca se apague durante el stream.

---

### Fase 3: Rediseño de "TIPO DE ACCIÓN" en 8 Categorías Visuales
Se reemplazó la lista desplegable técnica por una matriz visual de **8 Categorías con Presets de 1 Clic** y **Paleta Rápida de 8 Colores** tanto en PC (`MainWindow.xaml`) como en Android (`ButtonConfigDialog.kt`):
1. **`🎵 Audio PC`**: Controles multimedia globales de Windows + Barra Deslizante de Volumen PC.
2. **`🟢 Spotify Exclusivo`**: Control en 2do plano exclusivo para Spotify + Barra Deslizante de Volumen Spotify.
3. **`🚀 Abrir App`**: Selector de ejecutables `.exe` / `.lnk` con extracción de icono Jumbo `256×256`.
4. **`🪟 Ventanas`**: Minimizar, Maximizar/Restaurar, Cerrar ventana activa y Mostrar Escritorio (`Win+D`).
5. **`🎥 OBS Studio`**: Auto-escaneo en 1 clic de escenas y fuentes vía WebSocket v5, Stream, Grabar y Mute Mic.
6. **`⌨️ Atajo Teclado`**: Grabador de teclas en vivo + catálogo de atajos rápidos (Mute Discord, Captura, Copiar, Pegar, Alt+Tab, Alt+F4).
7. **`📄 Ir a Página`**: Selector visual de perfiles para navegar entre páginas.
8. **`⚙️ Sistema PC`**: Bloquear PC, Suspender, Reiniciar y Apagar.

---

### Fase 4: Modernización de la App Android y Adaptabilidad de Pantalla
- **Teclas 3D estilo Hardware (`ButtonTile.kt`)**: Biselado superior, indicador LED inferior, animación elástica al toque y feedback háptico.
- **Navegación Adaptativa en 4 Pestañas (`BottomNavBar.kt` / `SideNavRail`)**: *Panel Deck*, *Quick Deck*, *OBS Studio* y *Conexión*.
- **Modo Consola a Pantalla Completa (`🔲`)**: Oculta barras de navegación para maximizar el área táctil.
- **Adaptación a Vertical, Horizontal y Tablets**: Rotación automática de filas/columnas en modo horizontal (`isLandscape`) y matrices adaptativas en `ApplicationsScreen.kt` y `OBSScreen.kt`.

---

### Fase 5: Barras Deslizantes Verticales de Volumen y Espacio de Volumen en el Perfil
1. **Nuevos Tipos de Interfaz de Volumen Vertical (`VOLUME_SLIDER_PC` y `VOLUME_SLIDER_SPOTIFY`)**:
   - Permiten subir y bajar el volumen deslizando de arriba a abajo (`100%` a `0%`) tanto para el **Volumen Maestro del PC** como para el **Volumen Exclusivo de Spotify**.
2. **Espacio Dedicado de Volumen dentro del Perfil (`🎚️ VOLUMEN`)**:
   - **En el Editor de PC (`MainWindow.xaml` y `MainViewModel.cs`)**: Panel lateral **`🎚️ VOLUMEN`** junto a la cuadrícula `4×5` con botones de activación rápida (`🔊 + Barra PC` y `🟢 + Spotify`), sliders verticales en tiempo real, visor de porcentaje (`0% - 100%`) y botón de silencio (`🔇`).
   - **En la App Android (`GridScreen.kt` y `ButtonTile.kt`)**:
     - Botones rápidos **`🎚️ +Vol PC`** y **`🟢 +Spotify`** en la barra superior del perfil (`PageSelector`).
     - **`ProfileVolumeFaderDock` y `VerticalVolumeFaderBar`**: Espacio lateral dedicado dentro del perfil con barras táctiles verticales que envían `VOLUME_SET:<0..100>` y `SPOTIFY_SET_VOL:<0..100>` mientras deslizas el dedo de arriba a abajo.
     - Soporte adicional de deslizamiento vertical directamente sobre el botón en la cuadrícula.
3. **Motor de Audio CoreAudio por Porcentaje Exacto (`win32_input.rs` y `SpotifyController.cs`)**:
   - `IAudioEndpointVolume::SetMasterVolumeLevelScalar` para fijar el porcentaje exacto del volumen maestro de Windows (`0%` a `100%`).
   - `ISimpleAudioVolume::SetMasterVolume` para fijar el porcentaje exacto de la sesión de audio de `Spotify.exe` en segundo plano.

---

### Fase 6: Publicación en GitHub y Página Web Oficial
- **Repositorio GitHub**: `https://github.com/Yokerman41/StreamDockPro` (ramas `main` y `gh-pages`).
- **Página Web Local y GitHub Pages (`index.html`)**:
  - Incluye demostración interactiva de los botones y de las barras verticales deslizantes de volumen (`🔊 PC` y `🟢 SPO`), documentación visual de las 8 categorías, arquitectura y botones de descarga directa para `StreamDockPro_Instalador.exe`, `StreamDockPro.apk` y `StreamDockPro.exe`.

---

### Fase 7: Administrador de Tareas Remoto de Windows en la App Android (`Tareas`)
1. **Nueva Pestaña en la Barra Inferior y Riel Lateral**:
   - Integrada entre **`OBS Studio`** y **`Conexión`**: `Panel Deck` | `Quick Deck` | `OBS Studio` | `Tareas` | `Conexión`.
   - Pantalla [`TaskManagerScreen.kt`](file:///C:/Users/yorke/Downloads/stream%20dock%20pro%20nuevo/app/app/src/main/java/com/streamdock/pro/ui/components/TaskManagerScreen.kt) con diseño obsidian, contador de apps con ventana y procesos totales, barra de búsqueda en vivo y selector de filtros (`Apps con Ventana` vs `Todos los Procesos`).
2. **Monitoreo en Tiempo Real de Procesos de Windows**:
   - Títulos reales de ventanas visibles (ej. `"YouTube - Google Chrome"`, `"OBS Studio 30.2"`), nombre del ejecutable (ej. `chrome.exe`, `obs64.exe`), identificador PID y consumo exacto de memoria RAM en Megabytes (`MB`).
3. **Finalización Remota de Procesos (`kill_process`)**:
   - Botón directo `Finalizar` con diálogo modal de confirmación de seguridad y retroalimentación háptica.
   - Motor en Rust (`task_manager.rs`) que ejecuta terminación limpia con `OpenProcess` + `TerminateProcess` y fallback `taskkill /F /PID`, retornando de inmediato la lista actualizada sin retraso.

### Fase 8: Integración Nativa con Voicemeeter Banana (Audio Profesional)
1. **Conexión Directa con la API Oficial de Voicemeeter (`VoicemeeterRemote64.dll`)**:
   - El servidor Rust (`pc-companion-rust/src/plugins/voicemeeter.rs`) localiza y enlaza en tiempo de ejecución la biblioteca oficial de `C:\Program Files (x86)\VB\Voicemeeter\VoicemeeterRemote64.dll`.
   - Inicializa sesión `VBVMR_Login` y detecta automáticamente el tipo de consola (`Voicemeeter Banana` = Tipo 2).
   - Control instantáneo sin latencia de entradas físicas (`Strip[0]` Micrófono Realtek), virtuales (`Strip[3]` VAIO y `Strip[4]` AUX), y buses de salida (`Bus[0..4]`).
2. **Presets de Acción en el Editor de PC y Modal de Configuración Android**:
   - Nueva categoría **`🎛️ Voicemeeter`** integrada tanto en el Editor WinUI de Windows como en el modal de botones en Android:
     - *Micrófono*: Mute/Unmute (`VOICEMEETER_TOGGLE:Strip[0].Mute`), envío a monitoreo A1 (`VOICEMEETER_TOGGLE:Strip[0].A1`), envío a Discord/OBS B1 (`VOICEMEETER_TOGGLE:Strip[0].B1`).
     - *Voicemeeter VAIO (PC/Juegos)*: Mute (`VOICEMEETER_TOGGLE:Strip[3].Mute`), envío a Auriculares A2 (`VOICEMEETER_TOGGLE:Strip[3].A2`).
     - *Voicemeeter AUX*: Mute (`VOICEMEETER_TOGGLE:Strip[4].Mute`), envío a Altavoces A1 (`VOICEMEETER_TOGGLE:Strip[4].A1`).
     - *Buses Master*: Mute Master A1 (`VOICEMEETER_TOGGLE:Bus[0].Mute`) y Mute Master A2 (`VOICEMEETER_TOGGLE:Bus[1].Mute`).
     - *Mantenimiento*: Reinicio del motor de audio en 1 toque (`VOICEMEETER_RESTART`) y abrir la ventana de Voicemeeter (`VOICEMEETER_SHOW`).
3. **Faders Verticales Táctiles para Pistas de Voicemeeter**:
   - Soporte para botones deslizantes táctiles en el perfil de la app (`VOLUME_SLIDER_VM_STRIP0` para Micrófono, `VOLUME_SLIDER_VM_STRIP3` para PC VAIO, `VOLUME_SLIDER_VM_STRIP4` para AUX).
   - Permiten modular el volumen de cada pista deslizando el dedo de arriba a abajo directamente sobre el botón.

