# 13. USB Protocol & ADB Bridge (`UsbProtocol`) - Transporte Físico de Alta Velocidad

## 1. Visión General del Transporte USB ADB

Para escenarios de latencia crítica (ej. transmisión en vivo, eSports) o entornos sin conectividad Wi-Fi, la plataforma soporta comunicación USB mediante el protocolo de reenvío de puertos **ADB Reverse TCP Tunneling**.

---

## 2. Arquitectura de Túneles Multiplexados por Dispositivo

```
[ PC Windows Desktop ]                                    [ Dispositivos Android ]
                                ADB Server Daemon
                                       |
  WinUI 3 ADB Monitor                  |
  Inspecciona `adb devices`            |
  Ejecuta:                             |
  adb -s dev1 reverse tcp:8081 ------> | -------------> Android 1 (Tablet)
  adb -s dev2 reverse tcp:8081 ------> | -------------> Android 2 (Teléfono)
                                       |
                                       v
                             [ Rust Async Socket ]
                             (ws://127.0.0.1:8081)
```

---

## 3. Especificación del Proceso de Auto-Conexión USB
1. **Detección Automática**: El motor `StartUsbAdbMonitor` en WinUI 3 consulta la lista de dispositivos adjuntos vía `adb devices`.
2. **Asignación de Puertos por Serial**: Para cada dispositivo detectado, se establece la regla de puerto inverso individual:
   ```bash
   adb -s <SERIAL_ID> reverse tcp:8081 tcp:8081
   ```
3. **Redirección Loopback en Android**: La aplicación Android intenta conectarse en primer lugar a `ws://127.0.0.1:8081`. Al estar activo el túnel ADB, la conexión se desvía de forma transparente hacia el Servidor Rust en la máquina host a latencia física ultra baja ($<2\text{ ms}$).
