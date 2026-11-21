# 06. Workspace Engine (`WorkspaceEngine`) - Especificación Técnica

## 1. Visión General del Motor de Workspaces

El `WorkspaceEngine` gestiona la jerarquía lógica de configuración de cada consola. Cada cliente posee exactamente un `Workspace` que contiene la colección de perfiles, la navegación de páginas, los botones, los temas visuales, el esquema de colores y los parámetros del dispositivo.

---

## 2. Jerarquía del Objeto Workspace

```
[ Workspace (Pertenece a 1 ClientUUID) ]
  |
  +--> Metadatos (Version, ActiveProfileId, GridDimensions: 4x5, Theme: "Dark")
  |
  +--> Perfiles [ List<Profile> ]
  |     |
  |     +--> Profile "Navegador" (Id: "prof-1")
  |     |     |
  |     +--> Profile "Edición Video" (Id: "prof-2")
  |
  +--> Botones [ List<Button> ]
        |
        +--> Button (ProfileId: "prof-1", Position: 0, ActionType: "KEYBOARD_HOTKEY")
        +--> Button (ProfileId: "prof-1", Position: 1, ActionType: "OBS_SCENE_SWITCH")
        +--> Button (ProfileId: "prof-2", Position: 0, ActionType: "LAUNCH_APP")
```

---

## 3. Matriz de Atributos del Workspace

| Atributo | Tipo | Descripción |
| :--- | :--- | :--- |
| `client_uuid` | String (UUID v4) | Identificador soberano del cliente poseedor. |
| `version` | u64 | Número de secuencia incrementable para delta sync. |
| `active_profile_id` | String | ID del perfil que la consola está renderizando actualmente. |
| `grid_rows` | u8 | Cantidad de filas de la cuadrícula táctil (ej. 4). |
| `grid_cols` | u8 | Cantidad de columnas de la cuadrícula táctil (ej. 5). |
| `profiles` | List<Profile> | Colección de perfiles definidos en el workspace. |
| `buttons` | List<Button> | Colección de botones configurados en todos los perfiles. |

---

## 4. Invariantes del Motor de Workspaces
1. **Integridad Referencial**: Ningún botón puede existir con un `profileId` que no corresponda a un perfil válido dentro del mismo `Workspace`.
2. **Validación de Posición de Cuadrícula**: La combinación `(profileId, position)` dentro de la colección de botones debe ser única.
3. **Persistencia Atómica**: La actualización del `Workspace` se guarda en disco mediante el patrón *Write-to-Temp-and-Rename*, garantizando que ningún corte eléctrico corrompa la configuración.
