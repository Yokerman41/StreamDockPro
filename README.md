# Stream Dock Pro

Plataforma profesional de control remoto, automatización de escritorio y superficie táctil interactiva multi-dispositivo para Windows y Android, basada en una arquitectura **Cliente-Servidor asíncrona** en tiempo real con **Aislamiento por `ClientContext`**.

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

* **Ejecutable Único en Windows (Sin ventanas CMD)**: El servidor de alto rendimiento en Rust (`pc-companion-rust`) está incrustado dentro del Editor de PC (`StreamDockPro.exe`) y se inicia y cierra automáticamente en segundo plano.
* **Control Exclusivo de Spotify**: Botones dedicados para `Play/Pause`, `Siguiente`, `Anterior`, `Subir/Bajar Volumen` y `Silenciar` exclusivos de Spotify mediante `WM_APPCOMMAND` directo a la ventana de Spotify y `ISimpleAudioVolume` (CoreAudio por proceso) sin afectar el volumen de juegos ni del resto de Windows.
* **Integración Completa con OBS Studio (WebSocket v5)**: Autenticación SHA-256, persistencia automática de contraseña, cambio de escenas, control de visibilidad de fuentes, muteo de entradas de audio y control de transmisión/grabación.
* **Gestión de Ventanas y Aplicaciones**: Lanzamiento de programas (`.exe`), accesos directos (`.lnk`, `.url`), carpetas y acciones para **Minimizar**, **Maximizar/Restaurar** o **Cerrar** ventanas.
* **Perfiles por Contexto Editables**: Creación, renombrado y eliminación aislada de perfiles con sus propios botones sin alterar los demás perfiles.
* **Sincronización de Iconos de Alta Resolución (256×256)**: Extracción nativa en Windows (`SHGetFileInfo` / `SHGetImageList` Jumbo) sincronizada en tiempo real con el teléfono Android, además de biblioteca de iconos vectoriales.
* **Interfaz Android Adaptativa (Vertical y Horizontal)**: Reordenamiento de botones con pulsación larga (6s), compactación automática al eliminar botones y diseño optimizado para modo paisaje (Landscape) tipo consola Stream Deck.

---

## 🏗️ Estructura del Proyecto

```
StreamDockPro/
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
