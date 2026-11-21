# 08. Page Engine (`PageEngine`) - Navegación y Páginas Subordinadas

## 1. Visión General del Motor de Páginas

El `PageEngine` amplía la capacidad física de la cuadrícula táctil del cliente Android al permitir estructurar sub-páginas y carpetas dentro de un mismo perfil.

---

## 2. Diagrama de Navegación de Páginas y Carpetas

```
[ PERFIL: STREAMING ]
  |
  +--> PÁGINA 1 (Principal 4x5)
        |
        +-- [ Botón Slot 3: "Carpeta Audio" ] -------> [ PÁGINA 2: Carpeta Audio ]
        |                                                    |
        +-- [ Botón Slot 19: "Siguiente Pag >" ]             +-- [ Botón Slot 0: "< Volver" ]
                  |
                  v
        [ PÁGINA 3 (Secundaria 4x5) ]
```

---

## 3. Comportamiento y Pilas de Navegación (*Navigation Stack*)

1. **Navegación Táctil Inmediata**: Al presionar un botón de tipo `FOLDER_OPEN` o `PAGE_NEXT`, el cliente Android actualiza su estado de pila de navegación local para ofrecer respuesta instantánea a 0 ms de latencia percibida.
2. **Sincronización de Estado de Navegación**: El cliente transmite al `ClientContext` del servidor el cambio de página activa para que el editor WinUI 3 refleje la cuadrícula exacta que el usuario está viendo en su consola móvil.
