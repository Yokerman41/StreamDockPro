# 04. Editor Windows WinUI 3 (`pc-companion-winui`) - Especificación Técnica

## 1. Visión General del Editor WinUI 3

El editor de escritorio `pc-companion-winui` es la interfaz gráfica de usuario administrativa y de diseño para Windows. Está construido en C# utilizando Windows App SDK / WinUI 3 (o WPF Fluent GUI), diseñado con principios de arquitectura limpia y MVVM.

---

## 2. Diagrama de la Interfaz y Flujo de Selección de Contexto

```
+-----------------------------------------------------------------------------------+
| WINUI 3 TITLEBAR & CLIENT SELECTOR BAR                                           |
| Dispositivos Conectados:                                                          |
| [ Card: Redmi Note (Online) ]  [ Card: Pixel Tablet (Online) ]  [ Card: Mobile 3 ] |
|  ==> SelectedClientUuid: "UUID-AAA-111"                                           |
+-----------------------------------------------------------------------------------+
| LEFT SIDEBAR        | CENTER CANVAS / GRID 4x5             | RIGHT ACTION DETAILS|
|                     |                                      |                     |
| PERFILES (Context)  | [ Btn 1 ]  [ Btn 2 ]  [ Btn 3 ] ...  | Configurar Botón:   |
| - Navegador (Active)|                                      | - Etiqueta: "Naveg" |
| - Edición           | Active Profile: "Navegador"          | - Acción: Launch App|
| - Streaming         |                                      | - Ruta: chrome.exe  |
|                     | [ Add Button ]  [ Clear Slot ]       | - Icono: browser    |
+---------------------+--------------------------------------+---------------------+
```

---

## 3. Principio de Aislamiento en la Interfaz
* **Prohibición de Edición Global**: Ningún control de la interfaz (cuadrícula, listbox de perfiles, picker de acciones) tiene acceso a un modelo global.
* **Binding por `SelectedClientUuid`**:
  ```csharp
  // Pseudocódigo conceptual de binding
  public string SelectedClientUuid {
      get => _selectedClientUuid;
      set {
          _selectedClientUuid = value;
          RequestWorkspaceForClient(_selectedClientUuid);
          OnPropertyChanged(nameof(SelectedClientUuid));
      }
  }
  ```
* **Acciones de Edición Targetizadas**: Toda modificación (crear perfil, mover botón, cambiar icono) envía una trampa WebSocket `db_sync` especificando de forma obligatoria `targetUuid: SelectedClientUuid`.

---

## 4. Monitor Integrado de Transporte ADB / USB
El editor WinUI 3 incluye un servicio en segundo plano (`StartUsbAdbMonitor`) que inspecciona periódicamente los dispositivos Android conectados físicamente por USB ejecutando `adb devices`.

Para cada serie de dispositivo detectado (`serial`), ejecuta:
```bash
adb -s <serial> reverse tcp:8081 tcp:8081
```
Esto permite comunicación cero-configuración sobre cable USB a latencia ultra baja ($<2\text{ ms}$) simultáneamente para múltiples tablets y teléfonos conectados.
