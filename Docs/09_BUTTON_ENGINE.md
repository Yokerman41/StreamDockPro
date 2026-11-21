# 09. Button Engine (`ButtonEngine`) - Modelo y Estado de Botones

## 1. Visión General del Motor de Botones

El `ButtonEngine` modela la unidad fundamental de la interfaz interactiva. Representa un slot en la cuadrícula táctil que combina propiedades visuales (texto, icono, colores, estados activo/inactivo) con capacidades de ejecución de acciones.

---

## 2. Anatomía del Objeto Botón

```
+-----------------------------------------------------------------------------------+
|                                 MODELO DE BOTÓN                                   |
+-----------------------------------------------------------------------------------+
|  1. Identificadores: Id (UUID v4), ProfileId, Position (0..N-1)                   |
|  2. Visuales: Label, IconName, IconTint (Hex/ARGB), BackgroundColor, FontSize     |
|  3. Acción Principal: ActionType, ActionValue                                     |
|  4. Estado Dinámico: IsPressed, IsActiveState, DynamicText, BadgeValue             |
|  5. Feedback: HapticFeedbackType, SoundFeedbackName                              |
+-----------------------------------------------------------------------------------+
```

---

## 3. Tipos de Acciones Soportadas en Botones

```
+-----------------------------------------------------------------------------------+
| TIPO DE ACCIÓN         | VALOR DE EJEMPLO             | DESCRIPCIÓN               |
+------------------------+------------------------------+---------------------------+
| KEYBOARD_HOTKEY        | "CTRL+SHIFT+M"               | Atajo de teclado Win32    |
| LAUNCH_APP             | "C:\\Program Files\\app.exe" | Ejecutar aplicación local |
| MEDIA_CONTROL          | "PLAY_PAUSE"                 | Control multimedia Win32  |
| OBS_SCENE_SWITCH       | "Escena Principal"           | Cambio de escena OBS WS   |
| OBS_AUDIO_TOGGLE       | "Micrófono"                  | Mute/Unmute audio OBS     |
| STREAMER_BOT_ACTION    | "uuid-action-123"            | Ejecutar acción SB        |
| SYSTEM_COMMAND         | "SHUTDOWN"                   | Comando del sistema Win32 |
| PROFILE_SWITCH         | "profile-uuid-456"           | Conmutar perfil activo    |
| NAVIGATION_FOLDER      | "folder-uuid-789"            | Abrir carpeta subordinada |
+------------------------+------------------------------+---------------------------+
```

---

## 4. Estado Dinámico y Visual Feedback Engine
Un botón no es un elemento estático. Cuando una fuente de audio en OBS se ensordece, o cuando el estado de una transmisión cambia en Streamer.bot, el servidor Rust emite un evento de actualización de estado dinámico a través del `EventBus` hacia el `ClientContext`. El cliente Android recibe la actualización y redibuja únicamente la celda afectada (*partial cell re-render*).
