# Módulos del Proyecto

## Estructura de paquetes

```
edu.dosw.parcial/
├── main/           → Clase de arranque de la aplicación
├── config/         → Configuración de Spring (seguridad, Swagger, JPA, Mongo)
├── controller/     → Endpoints REST, DTOs y manejo de errores
│   ├── handlers/   → Controladores y manejador global de excepciones
│   ├── dtos/
│   │   ├── request/   → Objetos de entrada (lo que recibe la API)
│   │   └── response/  → Objetos de salida (lo que devuelve la API)
│   └── mappers/    → Conversión entre DTOs y modelos de dominio
├── core/           → Lógica de negocio pura (sin dependencia de frameworks)
│   ├── services/   → Casos de uso del sistema
│   ├── models/     → Enums y modelos de dominio
│   ├── validators/ → Validaciones de negocio reutilizables
│   └── utils/      → Excepciones y utilidades compartidas
└── persistence/    → Acceso a datos
    ├── entities/        → Entidades JPA (tablas de PostgreSQL)
    ├── repositories/    → Repositorios JPA
    ├── mappers/         → Conversión entre entidades y modelos de dominio
    └── mongo/
        ├── documents/       → Documentos MongoDB
        └── repositories/    → Repositorios MongoDB
```

---

## config/

| Clase | Qué hace |
|---|---|
| `SecurityConfig` | Define qué rutas son públicas y cuáles requieren token JWT o rol específico |
| `JpaConfig` | Le indica a Spring qué repositorios usan PostgreSQL |
| `MongoConfig` | Le indica a Spring qué repositorios usan MongoDB |
| `OpenApiConfig` | Configura Swagger con metadatos de la API y soporte para JWT |

## core/models/ — Enums de dominio

| Enum | Valores |
|---|---|
| `Role` | `USER`, `ADMIN` |
| `Category` | `RUNNING`, `GIMNASIO`, `CICLISMO`, `NATACION`, `DEPORTES_EQUIPO`, `OUTDOOR` |
| `ProductStatus` | `ACTIVE`, `INACTIVE` |
| `OrderStatus` | `PENDING`, `PAID`, `REJECTED` |
| `PaymentMethod` | `CREDIT_CARD`, `DEBIT_CARD`, `PSE` |
| `PaymentStatus` | `PENDING`, `APPROVED`, `REJECTED` |

## core/utils/ — Excepciones de dominio

Todas heredan de `BusinessException`, que lleva el código HTTP como parte del error. El `GlobalExceptionHandler` las captura y las convierte en la respuesta estándar.

| Excepción | Código HTTP | Cuándo usarla |
|---|---|---|
| `ResourceNotFoundException` | 404 | Recurso no encontrado |
| `ConflictException` | 409 | Estado incompatible (email duplicado, orden ya procesada) |
| `UnprocessableEntityException` | 422 | Regla de negocio violada (sin stock, carrito vacío) |

## controller/

| Clase | Qué hace |
|---|---|
| `GlobalExceptionHandler` | Captura todas las excepciones y devuelve siempre el mismo formato de error |
| `ErrorResponse` | DTO estándar de error: `status`, `error`, `message`, `path`, `timestamp` |
| `PageResponse<T>` | Wrapper genérico de paginación reutilizable: `content`, `totalItems`, `totalPages`, `currentPage` |

---

## Módulo: Auth (F-01, F-02)

| Clase | Capa | Qué hace |
|---|---|---|
| `UserEntity` | persistence | Tabla `users`, implementa `UserDetails` para Spring Security |
| `UserRepository` | persistence | `findByEmail`, `existsByEmail` |
| `JwtUtil` | core/utils | Genera, valida y extrae claims del token JWT |
| `JwtAuthFilter` | config | Intercepta cada request y autentica si el header `Authorization` es válido |
| `SecurityConfig` | config | Registra el filtro JWT y define reglas de acceso por ruta y rol |
| `UserDetailsServiceImpl` | core/services | Carga el usuario desde BD por email — usado por Spring Security |
| `AuthService` | core/services | Lógica de registro (hash BCrypt, email único) y login (mensaje genérico en error) |
| `AuthController` | controller | `POST /api/auth/register` → 201 · `POST /api/auth/login` → 200 |

---

## Módulo: Products (F-03, F-04, F-05, F-06, F-13)

| Clase | Capa | Qué hace |
|---|---|---|
| `ProductEntity` | persistence | Tabla `products` + tabla `product_images` para las URLs de imágenes |
| `ProductRepository` | persistence | Consultas por estado, categoría y nombre; búsqueda por ID activo |
| `ProductMapper` | persistence | MapStruct: convierte entre `ProductEntity` y `ProductResponse`/`ProductRequest` |
| `ProductService` | core/services | CRUD completo + consultas paginadas; eliminar inactiva en lugar de borrar |
| `ProductController` | controller | GET público · POST/PUT/DELETE solo `ADMIN` |

**Endpoints:**

| Método | Ruta | Acceso | Funcionalidad |
|---|---|---|---|
| `GET` | `/api/products` | Público | F-03 Listar |
| `GET` | `/api/products/category/{cat}` | Público | F-04 Filtrar por categoría |
| `GET` | `/api/products/search?name=` | Público | F-05 Buscar por nombre |
| `GET` | `/api/products/{id}` | Público | F-06 Ver detalle |
| `POST` | `/api/products` | ADMIN | F-13 Crear |
| `PUT` | `/api/products/{id}` | ADMIN | F-13 Actualizar |
| `DELETE` | `/api/products/{id}` | ADMIN | F-13 Eliminar (inactiva) |
