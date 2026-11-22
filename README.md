# Stream Dock Pro

Plataforma profesional de control remoto, automatización de escritorio y superficie táctil interactiva multi-dispositivo para Windows y Android, basada en una arquitectura **Cliente-Servidor asíncrona** en tiempo real con **Aislamiento por `ClientContext`**.

---

## 🌐 Página Web Oficial (Local & GitHub Pages)

* **Página Web Local**: Abre directamente [`index.html`](./index.html) en tu navegador (incluye una demo interactiva del Stream Dock y de las barras deslizantes verticales de volumen).
* **GitHub Pages**: [https://yokerman41.github.io/StreamDockPro/](https://yokerman41.github.io/StreamDockPro/)

---

## 📦 Descargas Directas (Releases)

Puedes descargar el instalador de Windows y el APK de Android directamente desde la sección [**Releases**](https://github.com/Yokerman41/StreamDockPro/releases/latest) o desde la carpeta [`dist/`](./dist):

| Plataforma | Archivo | Descripción |
| :--- | :--- | :--- |
| **Windows (Instalador)** | [`StreamDockPro_Instalador.exe`](./dist/StreamDockPro_Instalador.exe) | Instalador gráfico todo-en-uno (instala el Editor de PC + Servidor Rust integrado en 1 solo `.exe` sin ventanas CMD y crea accesos directos). |
| **Windows (Portable)** | [`StreamDockPro.exe`](./dist/StreamDockPro.exe) | Ejecutable único portable con el Servidor Rust incrustado automáticamente. |
| **Android (APK Firmado)** | [`StreamDockPro.apk`](./dist/Android/StreamDockPro.apk) | Aplicación Android lista para instalar en teléfonos o tablets (orientación vertical y horizontal adaptativa). |

---

## ✨ Características Principales

* **Barras Deslizantes Verticales de Volumen en el Perfil (`0% - 100%`)**: Espacio lateral dedicado dentro del perfil para subir o bajar el **Volumen Maestro del PC** (`VOLUME_SLIDER_PC`) y el **Volumen Exclusivo de Spotify** (`VOLUME_SLIDER_SPOTIFY`) deslizando de arriba a abajo tanto en la App Android como en el Editor de PC.
* **Ejecutable Único en Windows (Sin ventanas CMD)**: El servidor de alto rendimiento en Rust (`pc-companion-rust`) está incrustado dentro del Editor de PC (`StreamDockPro.exe`) y se inicia y cierra automáticamente en segundo plano, con soporte para minimizar a la bandeja del sistema.
* **Control Exclusivo de Spotify**: Botones y barras dedicadas para `Play/Pause`, `Siguiente`, `Anterior`, `Subir/Bajar Volumen`, `Barra Deslizante` y `Silenciar` exclusivos de Spotify mediante `WM_APPCOMMAND` directo a la ventana de Spotify y `ISimpleAudioVolume` (CoreAudio por proceso) sin afectar el volumen de juegos ni del resto de Windows.
* **Integración Completa con OBS Studio (WebSocket v5)**: Auto-escaneo en 1 clic de escenas y fuentes, autenticación SHA-256 persistente, cambio de escenas, control de visibilidad de fuentes, muteo de entradas de audio y control de transmisión/grabación.
* **8 Categorías Visuales de Acciones + Grabador de Atajos**: Selector visual organizado en *Audio PC*, *Spotify Exclusivo*, *Abrir Aplicación*, *Gestión de Ventanas*, *OBS Studio*, *Atajo de Teclado* (con grabador en vivo), *Ir a Página* y *Sistema PC*.
* **Cambio Automático de Perfil (Auto-Switch)**: Detecta la ventana o juego activo en Windows y cambia automáticamente al perfil asignado.
* **Sincronización de Iconos de Alta Resolución (256×256)**: Extracción nativa en Windows (`SHGetFileInfo` / `SHGetImageList` Jumbo) sincronizada en tiempo real con el teléfono Android.
* **Interfaz Android Moderna y Adaptativa**: `NavigationRail` en horizontal, barra inferior en vertical, Modo Consola a pantalla completa, Modo Edición rápida en 1 toque y rotación inteligente de cuadrículas.

---

## 🏗️ Estructura del Proyecto

```
StreamDockPro/
├── index.html               # Página web interactiva oficial (Local & GitHub Pages)
├── Docs/                    # Especificaciones técnicas de arquitectura
├── pc-companion-rust/       # Servidor Núcleo en Rust (Axum, Tokio, DashMap, Win32/CoreAudio)
├── pc-companion-winui/      # Editor GUI para Windows en WPF / C# (.NET 8)
├── app/                     # Aplicación Android en Kotlin + Jetpack Compose
├── crear_ejecutable_pc.bat  # Empaquetador del ejecutable único e instalador de Windows
├── crear_apk_android.bat    # Compilador y exportador del APK firmado de Android
└── dist/                    # Binarios compilados listos para instalar
```

---

## 🛠️ Compilación desde el Código Fuente

* **Generar Ejecutable Único e Instalador de PC**:
  Ejecuta `crear_ejecutable_pc.bat` (genera `dist\StreamDockPro.exe` y `dist\StreamDockPro_Instalador.exe`).
* **Generar APK Firmado de Android**:
  Ejecuta `crear_apk_android.bat` (genera `dist\Android\StreamDockPro.apk` y lo instala por ADB si hay un teléfono conectado).
