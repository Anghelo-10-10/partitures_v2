# 🎵 Partituras Musicales - API Spring Boot

> Sistema completo para gestión de partituras musicales desarrollado con Spring Boot (Kotlin), Spring Data JPA y PostgreSQL.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![License](https://img.shields.io/badge/license-MIT-green)

## ✨ Características Principales

- 📤 **Subida de partituras** en formato PDF con validación estricta
- 🔍 **Búsqueda avanzada** por título, artista, género e instrumento  
- 👥 **Sistema de usuarios** con autenticación y perfiles
- ⭐ **Favoritos** para gestión personal de partituras
- 📄 **Visualización y descarga** de PDFs
- 🚀 **Optimización anti-N+1** queries para alta performance
- ⚠️ **Manejo robusto de errores** con códigos HTTP semánticos

## 📋 Requisitos Previos

- Java 21 (JDK 21)
- PostgreSQL 15 o superior
- Gradle 8 o superior
- Docker (opcional, para base de datos)

## 🚀 Instalación y Configuración

### 1. Clonar el Repositorio

```bash
git clone https://github.com/Anghelo-10-10/partitures_v2.git
cd partitures
```

### 2. Configurar Base de Datos

**Opción A: Con Docker (Recomendado)**
```bash
docker-compose up -d
```

**Opción B: PostgreSQL Local**
- Crear base de datos: `partitures_db`
- Usuario: `admin` / Password: `admin`
- Puerto: `5433`

### 3. Ejecutar la Aplicación

```bash
./gradlew bootRun
```

La API estará disponible en: `http://localhost:8080`

## 🔗 Conexión a Base de Datos (PgAdmin)

| Parámetro | Valor |
|-----------|-------|
| Host | localhost |
| Puerto | 5433 |
| Usuario | admin |
| Password | admin |
| Base de Datos | partitures_db |

## 📚 Documentación de la API

### Autenticación

#### Iniciar Sesión
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "usuario@email.com",
  "password": "mipassword"
}
```

### Usuarios

#### Crear Usuario
```http
POST /api/users
Content-Type: application/json

{
  "name": "Juan Pérez",
  "email": "juan@email.com", 
  "password": "Password123"
}
```

#### Obtener Usuario
```http
GET /api/users/{id}
```

#### Actualizar Perfil
```http
PUT /api/users/profile?userId=1
Content-Type: application/json

{
  "name": "Nuevo Nombre",
  "bio": "Mi biografía musical"
}
```

### Partituras

#### Subir Partitura
```http
POST /api/sheets
Content-Type: multipart/form-data

file: [archivo.pdf]
title: "Moonlight Sonata"
description: "Sonata No. 14 de Beethoven"
artist: "Ludwig van Beethoven" 
genre: "Classical"
instrument: "Piano"
isPublic: true
ownerId: 1
```

#### Obtener Partituras Públicas
```http
GET /api/sheets/public
```

#### Búsqueda Avanzada
```http
GET /api/sheets/search/advanced?searchTerm=beethoven&genre=Classical&instrument=Piano&sortBy=recent
```

#### Ver PDF en Navegador
```http
GET /api/sheets/{id}/pdf
```

#### Descargar PDF
```http
GET /api/sheets/{id}/pdf/download
```

### Favoritos

#### Agregar a Favoritos
```http
POST /api/sheets/{sheetId}/favorites?userId=1
```

#### Obtener Favoritos del Usuario
```http
GET /api/sheets/users/{userId}/favorites
```

### Filtros y Búsqueda

#### Obtener Géneros Disponibles
```http
GET /api/sheets/filters/genres
```

#### Obtener Instrumentos Disponibles  
```http
GET /api/sheets/filters/instruments
```

#### Filtrar por Género
```http
GET /api/sheets/genre/Classical
```

#### Filtrar por Instrumento
```http
GET /api/sheets/instrument/Piano
```

#### Partituras del Usuario
```http
GET /api/sheets/users/{userId}/owned
```

## 📊 Ejemplo de Respuesta

```json
{
  "id": 1,
  "title": "Moonlight Sonata",
  "description": "Sonata No. 14 de Beethoven",
  "artist": "Ludwig van Beethoven",
  "genre": "Classical",
  "instrument": "Piano", 
  "pdfFilename": "1698765432_abc12345.pdf",
  "pdfSize": 2048576,
  "pdfSizeMB": "2.0 MB",
  "pdfContentType": "application/pdf",
  "pdfDownloadUrl": "/api/sheets/1/pdf",
  "isPublic": true,
  "ownerId": 1,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

## 🏗️ Arquitectura del Proyecto

```
src/main/kotlin/com/partituresforall/partitures/
├── controllers/          # Endpoints REST
│   ├── AuthController    # Autenticación
│   ├── UserController    # Gestión usuarios  
│   ├── SheetController   # Gestión partituras
│   └── FileController    # Gestión archivos
├── services/             # Lógica de negocio
│   ├── UserService       # Operaciones usuario
│   ├── SheetService      # Operaciones partituras  
│   └── FileService       # Gestión archivos
├── repositories/         # Acceso a datos JPA
│   ├── UserRepository    # Consultas usuario
│   ├── SheetRepository   # Consultas partituras
│   └── UserSheetRepository # Relaciones
├── models/
│   ├── entities/         # Entidades JPA
│   ├── requests/         # DTOs entrada
│   └── responses/        # DTOs salida
├── exceptions/           # Manejo errores
└── config/              # Configuraciones
```

## ⚡ Características Técnicas

### Sistema de Validación
- **Archivos PDF**: Validación de tamaño (máx 5MB), tipo MIME y magic bytes
- **Contraseñas**: Mínimo 8 caracteres, mayúsculas, minúsculas y números
- **Emails**: Validación de formato y unicidad

### Optimizaciones de Performance
- **Anti N+1 Queries**: Batch loading de relaciones
- **Lazy Loading**: Contenido PDF cargado bajo demanda  
- **Índices de BD**: Optimizados para búsquedas frecuentes
- **Queries Nativas**: PostgreSQL ILIKE para búsquedas eficientes

### Seguridad
- **Encriptación BCrypt** para contraseñas
- **Validación de archivos** con magic bytes
- **Control de acceso** por propietario
- **Manejo seguro** de multipart uploads

## 🛠️ Stack Tecnológico

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Spring Boot | 3.5.0 | Framework principal |
| Kotlin | 1.9+ | Lenguaje de programación |
| PostgreSQL | 15 | Base de datos |
| Hibernate | 6.6.15 | ORM |
| Docker | latest | Contenedores |
| BCrypt | - | Encriptación |

## ⚙️ Configuración

### application.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/partitures_db
    username: admin  
    password: admin
  
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

app:
  file:
    upload-dir: uploads
```

### docker-compose.yml
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: partitures_db
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin
    ports:
      - "5433:5432"
```

## 🧪 Testing con cURL

### Crear Usuario
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"Password123"}'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Password123"}'
```

### Obtener Partituras Públicas
```bash
curl http://localhost:8080/api/sheets/public
```

### Buscar Partituras
```bash
curl "http://localhost:8080/api/sheets/search/advanced?searchTerm=classical&sortBy=recent"
```

## 🚀 Roadmap

- [ ] **JWT + Spring Security** para autenticación stateless
- [ ] **Validaciones @Valid** en controllers  
- [ ] **Paginación** para endpoints de listas
- [ ] **Tests unitarios** e integración
- [ ] **Swagger/OpenAPI** para documentación automática
- [ ] **Cache Redis** para consultas frecuentes
- [ ] **Métricas Prometheus** para observabilidad
- [ ] **Frontend React/Vue** para interfaz gráfica

## 🐛 Solución de Problemas

### Error de Conexión a BD
1. Verificar que PostgreSQL esté ejecutándose en puerto 5433
2. Comprobar credenciales en `application.yml`
3. Usar Docker: `docker-compose up -d`

### Error de Subida de Archivos
1. Verificar que el archivo sea PDF válido
2. Comprobar tamaño máximo (5MB)
3. Revisar logs para detalles del error

### Puerto en Uso
```bash
# Cambiar puerto en application.yml
server:
  port: 8081
```

## 📞 Contacto

- **GitHub**: [Anghelo-10-10](https://github.com/Anghelo-10-10)
- **Repositorio**: [partitures_v2](https://github.com/Anghelo-10-10/partitures_v2)
- **Stack**: Kotlin + Spring Boot + PostgreSQL

---

**🎵 ¡API lista para ser consumida por cualquier frontend o aplicación móvil! ✨**
