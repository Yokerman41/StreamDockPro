# 07. Profile Engine (`ProfileEngine`) - Gestión de Perfiles por Cliente

## 1. Visión General del Motor de Perfiles

El `ProfileEngine` administra los perfiles dentro de un `ClientContext`. Un perfil es una agrupación lógica de páginas y botones orientada a un contexto de uso específico del usuario (ej. "Navegación Web", "Streaming OBS", "Edición Premiere Pro").

---

## 2. Flujo de Auto-Conmutación de Perfiles por Aplicación Enfocada (*Auto-Profile Switching*)

```
[Windows OS Foreground Event] (ej. Usuario cambia foco a `obs64.exe`)
               |
               v
     [Rust Core Event Listener]
               |
               v
   Recorrer todos los `ClientContext` activos
               |
               v
  ¿`ClientContext` tiene perfil mapeado a `obs64.exe`?
             /                  \
          (Sí)                  (No)
           /                      \
Conmutar `active_profile_id`    Mantener perfil actual
Notificar a la consola Android
vía WebSocket payload `set_active_profile`
```

---

## 3. Especificación del Objeto Perfil

```
Profile Struct:
- id: String (UUID v4)
- name: String (ej. "Navegador Chrome")
- auto_switch_process: Option<String> (ej. "chrome.exe")
- is_default: bool
- icon_name: String
- background_color: String (Hex #RRGGBB)
```

---

## 4. Reglas de Validación
1. **Perfil por Defecto Ineliminable**: Todo `Workspace` debe contener al menos un perfil marcado como `is_default = true`. Si el usuario intenta eliminar el último perfil, la operación es rechazada con un código de error de validación.
2. **Identificadores Únicos**: El ID del perfil se asigna como un UUID v4 al momento de su creación.
