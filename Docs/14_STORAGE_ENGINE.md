# 14. Storage Engine (`StorageEngine`) - Persistencia y Abstracción NoSQL / Relacional

## 1. Visión General del Motor de Almacenamiento

El `StorageEngine` administra la persistencia de datos en el servidor Rust. Está diseñado bajo el principio de **Abstracción de Repositorio**, permitiendo operar actualmente sobre almacenamiento NoSQL basado en documentos JSON independientes en disco, con capacidad de migración transparente hacia **SQLite** o **PostgreSQL**.

---

## 2. Estructura de Almacenamiento NoSQL en Disco

```
%APPDATA%/StreamCorePro/
├── clients.json                    <-- Catálogo de metadatos de clientes registrados
├── settings.json                   <-- Configuración global del servidor PC
└── workspaces/                     <-- Directorio de Workspaces Aislados
    ├── 550e8400-e29b-41d4.json     <-- Workspace del Cliente A (Redmi)
    ├── a1b2c3d4-e5f6-7890.json     <-- Workspace del Cliente B (Tablet)
    └── f47ac10b-58cc-4372.json     <-- Workspace del Cliente C (Mobile 3)
```

---

## 3. Estrategia de Migración Futura a Motores SQL

El motor utiliza el patrón de diseño **Repository Pattern** desacoplado mediante Traits de Rust:

```rust
// Concepto de Abstracción de Repositorio (Trait de Arquitectura)
pub trait StorageRepository: Send + Sync {
    fn load_client_info(&self, uuid: &str) -> Result<ClientInfo, StorageError>;
    fn save_client_info(&self, info: &ClientInfo) -> Result<(), StorageError>;
    fn load_workspace(&self, uuid: &str) -> Result<Workspace, StorageError>;
    fn save_workspace(&self, uuid: &str, workspace: &Workspace) -> Result<(), StorageError>;
}
```

### Adaptadores Soportados:
1. **`JsonFileStorageAdapter`** (Actual): Lee y escribe archivos JSON en disco de forma atómica.
2. **`SqliteStorageAdapter`** (Futuro): Persistencia relacional local en archivo `.sqlite` único para entornos portables.
3. **`PostgresStorageAdapter`** (Futuro): Persistencia relacional empresarial multi-nodo para despliegues masivos.
