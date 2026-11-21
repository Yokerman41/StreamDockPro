# 17. Icon Manager (`IconManager`) - Gestión de Recursos Gráficos y Renderizado

## 1. Visión General del Gestor de Iconos

El `IconManager` se encarga de la extracción, almacenamiento en caché, procesamiento gráfico y distribución de recursos visuales (iconos vectores, imágenes PNG/WebP, iconos extraídos de archivos ejecutable `.exe`) para los botones de las consolas táctiles.

---

## 2. Pipeline de Extracción y Caché de Iconos

```
[ Archivo .exe o Imagen ]
            |
            v
   Win32 Icon Extraction (ExtractIconEx / Shell32)
            |
            v
   Conversión a Formato Normalizado (PNG 128x128 32-bit ARGB)
            |
            v
   Generación de SHA-256 Hash del Contenido
            |
            v
   Guardado en Caché Local (`%APPDATA%/StreamCorePro/extracted_icons/hash.png`)
            |
            v
   Transferencia al Cliente Android vía WebSocket Base64 / Binary Stream
            |
            v
   Caché LRU en Memoria Android (LruCache<String, Bitmap>)
```

---

## 3. Renderizado de Tintado Vectorial Dinámico (*Dynamic Vector Tinting*)
Los iconos en formato de glifos o vectores de línea permiten tintado en tiempo real en la consola Android utilizando máscaras de color (Color Filters). Esto ahorra consumo de red y almacenamiento al transmitir únicamente el nombre del icono estándar (ej. `"settings"`, `"mic_off"`) y el valor Hexadecimal del color de acento (ej. `#06B6D4`), realizando el tintado directamente en el cliente Android.
