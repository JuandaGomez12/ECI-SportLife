# API Operations — SportLife

La documentación interactiva completa está disponible en Swagger UI una vez el servidor esté corriendo:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api-docs`

---

## Auth — `/api/auth`

| Método | Ruta | Acceso | Código OK | Descripción |
|--------|------|--------|-----------|-------------|
| `POST` | `/api/auth/register` | Público | `201` | Registra un nuevo usuario |
| `POST` | `/api/auth/login` | Público | `200` | Autentica y devuelve token JWT |

### POST `/api/auth/register`
```json
// Request
{ "name": "Juan Pérez", "email": "juan@ejemplo.com", "password": "MiClave$123" }

// Response 201
{ "id": "uuid", "name": "Juan Pérez", "email": "juan@ejemplo.com", "createdAt": "2025-04-09T10:00:00" }
```

### POST `/api/auth/login`
```json
// Request
{ "email": "juan@ejemplo.com", "password": "MiClave$123" }

// Response 200
{ "token": "eyJ...", "tokenType": "Bearer", "expiresIn": 3600 }
```

---

## Products — `/api/products`

| Método | Ruta | Acceso | Código OK | Descripción |
|--------|------|--------|-----------|-------------|
| `GET` | `/api/products` | Público | `200` | Lista productos activos (paginado) |
| `GET` | `/api/products/category/{category}` | Público | `200` | Filtra por categoría |
| `GET` | `/api/products/search?name=` | Público | `200` | Busca por nombre |
| `GET` | `/api/products/{id}` | Público | `200` | Detalle de un producto |
| `POST` | `/api/products` | ADMIN | `201` | Crea un producto |
| `PUT` | `/api/products/{id}` | ADMIN | `200` | Actualiza un producto |
| `DELETE` | `/api/products/{id}` | ADMIN | `204` | Inactiva un producto |

### GET `/api/products?page=0&size=10`
```json
// Response 200
{
  "content": [{ "id": "uuid", "name": "Camiseta Running", "category": "RUNNING", "price": 79900.00, "stock": 45, "status": "ACTIVE" }],
  "totalItems": 38, "totalPages": 4, "currentPage": 0
}
```

### POST `/api/products` *(requiere Bearer token ADMIN)*
```json
// Request
{ "name": "Camiseta Running", "description": "Secado rápido", "category": "RUNNING", "price": 79900.00, "stock": 45 }

// Response 201
{ "id": "uuid", "name": "Camiseta Running", "category": "RUNNING", "price": 79900.00, "stock": 45, "status": "ACTIVE", "createdAt": "2025-04-09T10:00:00" }
```

---

## Cart — `/api/cart` *(requiere Bearer token USER)*

| Método | Ruta | Acceso | Código OK | Descripción |
|--------|------|--------|-----------|-------------|
| `POST` | `/api/cart/items` | USER | `201` | Agrega un producto al carrito |
| `GET` | `/api/cart` | USER | `200` | Ver resumen del carrito |

### POST `/api/cart/items`
```json
// Request
{ "productId": "uuid", "quantity": 2 }

// Response 201
{ "cartItemId": "uuid", "productName": "Camiseta Running", "quantity": 2, "unitPrice": 79900.00, "subtotal": 159800.00 }
```

### GET `/api/cart`
```json
// Response 200
{
  "items": [{ "cartItemId": "uuid", "productName": "Camiseta Running", "quantity": 2, "unitPrice": 79900.00, "subtotal": 159800.00 }],
  "total": 159800.00, "itemCount": 2
}
```

---

## Orders — `/api/orders` *(requiere Bearer token USER)*

| Método | Ruta | Acceso | Código OK | Descripción |
|--------|------|--------|-----------|-------------|
| `POST` | `/api/orders/checkout` | USER | `201` | Inicia el proceso de pago |
| `PATCH` | `/api/orders/{id}/approve` | USER | `200` | Confirma pago aprobado |
| `PATCH` | `/api/orders/{id}/reject` | USER | `200` | Registra pago rechazado |
| `POST` | `/api/orders/{id}/retry` | USER | `201` | Reintenta el pago |

### POST `/api/orders/checkout`
```json
// Request
{ "paymentMethod": "CREDIT_CARD", "paymentToken": "tok_sandbox_abc" }

// Response 201
{ "orderId": "uuid", "status": "PENDING", "total": 159800.00, "createdAt": "2025-04-09T11:00:00" }
```

### PATCH `/api/orders/{id}/approve`
```json
// Request
{ "transactionId": "TXN-20250409-001" }

// Response 200
{ "orderId": "uuid", "status": "PAID", "transactionId": "TXN-20250409-001", "total": 159800.00, "message": "Tu compra fue exitosa." }
```

---

## Códigos de error comunes

```json
// 400 — campo inválido
{ "status": 400, "error": "Bad Request", "message": "email: El formato del correo no es válido", "path": "/api/auth/register" }

// 401 — sin token o credenciales inválidas
{ "status": 401, "error": "Unauthorized", "message": "Credenciales inválidas", "path": "/api/auth/login" }

// 403 — autenticado pero sin permisos
{ "status": 403, "error": "Forbidden", "message": "No tienes permisos para realizar esta acción", "path": "/api/products" }

// 404 — recurso no encontrado
{ "status": 404, "error": "Not Found", "message": "No se encontró ningún producto con el ID indicado", "path": "/api/products/uuid" }

// 409 — conflicto de estado
{ "status": 409, "error": "Conflict", "message": "Ya existe una cuenta con este correo", "path": "/api/auth/register" }

// 422 — regla de negocio violada
{ "status": 422, "error": "Unprocessable Entity", "message": "Stock insuficiente. Disponible: 3, solicitado: 10", "path": "/api/cart/items" }
```
