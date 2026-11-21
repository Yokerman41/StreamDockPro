# 18. Security Architecture (`SecurityEngine`) - Seguridad y Autenticación

## 1. Visión General del Modelo de Seguridad

Debido a que el Servidor Rust posee permisos para simular pulsaciones de teclas, ejecutar programas y controlar componentes del sistema en la máquina host, la arquitectura implementa un modelo de seguridad por capas para evitar accesos no autorizados dentro de la red local.

---

## 2. Capas de Seguridad y Autenticación

```
+-----------------------------------------------------------------------------------+
|                            CAPAS DE SEGURIDAD DEL SISTEMA                         |
+-----------------------------------------------------------------------------------+
|  1. Aislamiento de Red: Escucha restringida a Subred Local / USB ADB Localhost    |
|  2. Autenticación por PIN de Emparejamiento (Pairing PIN): Handshake 6 dígitos    |
|  3. Cifrado TLS / WSS: Cifrado en tránsito opcional mediante Certificado Auto-firm  |
|  4. Sanitiación de Ejecutables: Validación estricta de rutas de archivos .exe     |
|  5. Permisos de Contexto: UUIDs firmados y tokens de sesión efímeros              |
+-----------------------------------------------------------------------------------+
```

---

## 3. Protocolo de Emparejamiento por PIN (Pairing Handshake)

```
[ Cliente Android ]                                         [ Servidor Rust / WinUI 3 ]
         |                                                              |
         |----- (1) Solicita Conexión --------------------------------->|
         |                                                              | (Muestra PIN "849201" en pantalla WinUI 3)
         |<---- (2) Challenge `auth_required` --------------------------|
         |                                                              |
    (Usuario ingresa PIN "849201" en Android)                           |
         |                                                              |
         |----- (3) `verify_pin` (PIN + ClientUUID) ------------------->|
         |                                                              | Validar PIN hash
         |<---- (4) `auth_success` (Guarda Token permanente) -----------|
```
