# 20. Implementation Guide (`ImplementationGuide`) - Guía de Desarrollo y Despliegue

## 1. Requisitos Previos del Entorno de Desarrollo

* **Rust Toolchain**: `rustc` $\ge 1.75$ / `cargo` (Edición 2021).
* **Windows SDK**: Visual Studio 2022 / C++ Build Tools con SDK de Windows 10/11 (10.0.19041+).
* **.NET SDK**: .NET 8.0 SDK para WinUI 3 / WPF.
* **Android SDK**: Android Studio Jellyfish / SDK API 34+ con Kotlin 1.9+.

---

## 2. Pasos de Compilación e Integración

### 2.1 Compilación del Servidor Rust
```bash
cd pc-companion-rust
cargo build --release
```
El ejecutable resultante `pc-companion-rust.exe` se ubicará en `target/release/`.

### 2.2 Compilación del Editor WinUI 3
```bash
cd pc-companion-winui
dotnet restore --source "https://api.nuget.org/v3/index.json"
dotnet build -c Release
```

### 2.3 Compilación de la App Android
```bash
cd app
./gradlew assembleRelease
```

---

## 3. Matriz de Verificación y Pruebas
1. **Prueba Unitaria de Identidad**: Verificación de inmutabilidad de UUID en Android frente a reinicios y cambios de red.
2. **Prueba de Aislamiento de Contextos**: Conectar 2 emuladores Android simultáneos y verificar que modificar la cuadrícula del Cliente $A$ no altera la memoria o persistencia del Cliente $B$.
3. **Prueba de Redirección USB ADB**: Ejecutar dos instancias de emulador y comprobar que `adb -s <serial> reverse` habilita comunicación independiente en ambas consolas.
