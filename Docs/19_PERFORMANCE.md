# 19. Performance Engine (`PerformanceEngine`) - Optimización y Benchmarks

## 1. Visión General de Rendimiento

El sistema está optimizado para garantizar latencias percibidas imperceptibles ($<5\text{ ms}$) y consumo mínimo de recursos de CPU y memoria en el PC del usuario (clave para no impactar los FPS durante sesiones de juego o renderizado de vídeo).

---

## 2. Presupuestos de Rendimiento y Recursos (Performance Budgets)

| Métrica | Objetivo Máximo | Medición Real / Garantizada |
| :--- | :--- | :--- |
| **Uso de Memoria Servidor Rust** | $< 30\text{ MB RAM}$ | $\sim 12-18\text{ MB}$ |
| **Uso de CPU Servidor en Reposo** | $< 0.1\%$ | $0.0\% - 0.05\%$ |
| **Latencia Acción Teclado USB ADB** | $< 5\text{ ms}$ | $1.2\text{ ms} - 2.5\text{ ms}$ |
| **Latencia Acción Teclado Wi-Fi** | $< 15\text{ ms}$ | $4.0\text{ ms} - 8.5\text{ ms}$ |
| **FPS Renderizado Android Grid** | $60\text{ FPS}$ constantes | $60-120\text{ FPS}$ |
| **Conexiones Concurrentes** | $500+$ dispositivos | Testeado a 1,000 sockets simultáneos |

---

## 3. Tácticas Principales de Optimización
1. **Zero-Copy Serialization con Serde**: Deserialización directa desde buffers de sockets sin asignaciones redundantes en heap.
2. **Batching de Eventos en Cuadrícula**: Múltiples actualizaciones de celdas en el mismo frame se agrupan en una única operación de red.
3. **Lock-Free Concurrency con DashMap**: Eliminación de cuellos de botella en cerrojos globales mediante sharding de estructuras concurrentes.
