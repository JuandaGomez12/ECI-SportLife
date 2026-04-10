# Persistencia en SportLife

SportLife usa **dos motores de persistencia en paralelo**: uno relacional (PostgreSQL vía JPA) y uno no relacional (MongoDB). Esta decisión permite usar cada tecnología donde mejor encaja, en lugar de forzar todos los datos al mismo molde.

---

## ¿Por qué dos bases de datos?

| Necesidad | Tecnología elegida | Razón |
|---|---|---|
| Usuarios, productos, órdenes, pagos | **PostgreSQL (relacional)** | Datos estructurados con relaciones claras y transacciones ACID |
| Logs de auditoría, historial de eventos, datos flexibles | **MongoDB (no relacional)** | Documentos sin esquema fijo, fácil de escalar horizontalmente |

Una base de datos relacional garantiza que si un pago se aprueba y el stock no se descuenta, ambas operaciones se revierten juntas (transacción ACID). Eso es imposible de garantizar de forma nativa en MongoDB, por eso los datos críticos del negocio van en PostgreSQL.

MongoDB es ideal para datos que cambian de forma con frecuencia o que no necesitan JOIN entre colecciones, como logs de actividad del usuario o registros de auditoría.

---

## 1. Persistencia Relacional — PostgreSQL + JPA

### ¿Qué es JPA?

**JPA** (Java Persistence API) es una especificación de Java que define cómo mapear objetos Java a tablas de una base de datos relacional. **Hibernate** es la implementación más popular de JPA y es la que usa Spring Boot por defecto.

En lugar de escribir SQL a mano, defines una clase Java con anotaciones y JPA genera las consultas automáticamente.

### Configuración en `application.yaml`

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/sportlife}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
    open-in-view: false
```

### Explicación de cada propiedad

| Propiedad | Valor | Qué hace |
|---|---|---|
| `url` | `jdbc:postgresql://localhost:5432/sportlife` | Dirección de la base de datos. Usa variable de entorno con fallback para desarrollo local |
| `username` / `password` | Variables de entorno | Nunca se hardcodean credenciales en el código |
| `hikari.maximum-pool-size` | `10` | HikariCP es el pool de conexiones de Spring Boot. Mantiene hasta 10 conexiones abiertas para reutilizarlas |
| `hikari.connection-timeout` | `30000` | Si no hay conexión disponible en 30 segundos, lanza error en lugar de esperar indefinidamente |
| `ddl-auto` | `update` | Hibernate actualiza el esquema automáticamente al arrancar. **En producción debe ser `validate` o `none`** |
| `show-sql` | `false` | No imprime cada SQL en consola (en desarrollo puede ponerse `true` para depurar) |
| `format_sql` | `true` | Si se activa show-sql, formatea el SQL para que sea legible |
| `dialect` | `PostgreSQLDialect` | Le dice a Hibernate que genere SQL específico para PostgreSQL |
| `open-in-view` | `false` | Deshabilita el antipatrón que mantiene sesiones de BD abiertas durante el renderizado de la vista |

### Clase de configuración — `JpaConfig.java`

```java
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "edu.dosw.parcial.persistence.repositories")
public class JpaConfig {
}
```

`@EnableJpaRepositories` le indica a Spring que las interfaces en el paquete `persistence.repositories` son repositorios JPA. Esto es necesario cuando conviven JPA y MongoDB en el mismo proyecto, porque Spring necesita saber a cuál de los dos sistemas pertenece cada repositorio.

`@EnableTransactionManagement` activa el soporte para la anotación `@Transactional`, que garantiza que un bloque de operaciones se ejecuta completamente o no se ejecuta en absoluto.

### Estructura de paquetes

```
persistence/
├── entities/          ← Clases Java anotadas con @Entity (mapean a tablas)
├── repositories/      ← Interfaces que extienden JpaRepository
└── mappers/           ← Conversión entre Entity y modelo de dominio
```

### Ejemplo de uso futuro

```java
// Entidad
@Entity
@Table(name = "products")
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;
}

// Repositorio
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {
    List<ProductEntity> findByStatusAndCategory(ProductStatus status, Category category);
}
```

---

## 2. Persistencia No Relacional — MongoDB

### ¿Qué es MongoDB?

MongoDB es una base de datos **orientada a documentos**. En lugar de tablas con filas y columnas, almacena documentos JSON (llamados BSON internamente). No tiene esquema fijo: dos documentos en la misma colección pueden tener campos diferentes.

### Configuración en `application.yaml`

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGO_URI:mongodb://localhost:27017/sportlife}
      database: ${MONGO_DB:sportlife}
      auto-index-creation: true
```

### Explicación de cada propiedad

| Propiedad | Valor | Qué hace |
|---|---|---|
| `uri` | `mongodb://localhost:27017/sportlife` | Dirección del servidor MongoDB y nombre de la base de datos |
| `database` | `sportlife` | Nombre de la base de datos dentro de MongoDB |
| `auto-index-creation` | `true` | Crea automáticamente los índices definidos con `@Indexed` en los documentos |

### Clase de configuración — `MongoConfig.java`

```java
@Configuration
@EnableMongoRepositories(basePackages = "edu.dosw.parcial.persistence.mongo.repositories")
public class MongoConfig {
}
```

`@EnableMongoRepositories` hace lo mismo que `@EnableJpaRepositories` pero para MongoDB: le dice a Spring que las interfaces en ese paquete son repositorios de MongoDB, no de JPA.

### Estructura de paquetes

```
persistence/
├── mongo/
│   ├── documents/     ← Clases Java anotadas con @Document (mapean a colecciones)
│   └── repositories/  ← Interfaces que extienden MongoRepository
```

### Ejemplo de uso futuro

```java
// Documento
@Document(collection = "audit_logs")
public class AuditLogDocument {
    @Id
    private String id;

    private String userId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details;
}

// Repositorio
public interface AuditLogRepository extends MongoRepository<AuditLogDocument, String> {
    List<AuditLogDocument> findByUserIdOrderByTimestampDesc(String userId);
}
```

---

## 3. ¿Por qué se necesita configurar ambos explícitamente?

Cuando Spring Boot detecta que tienes tanto `spring-boot-starter-data-jpa` como `spring-boot-starter-data-mongodb` en el classpath, intenta auto-configurar ambos. El problema es que al escanear los repositorios no sabe a cuál sistema pertenece cada interfaz.

Si no se especifica nada, Spring puede lanzar este error:

```
No qualifying bean of type 'javax.persistence.EntityManagerFactory'
```

La solución es decirle explícitamente mediante las anotaciones:
- `@EnableJpaRepositories(basePackages = "...repositories")` → estos usan PostgreSQL
- `@EnableMongoRepositories(basePackages = "...mongo.repositories")` → estos usan MongoDB

Al tener paquetes separados, Spring sabe sin ambigüedad qué motor usar para cada interfaz.

---

## 4. Documentación — Swagger / OpenAPI

### Configuración en `application.yaml`

```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
    display-request-duration: true
```

| Propiedad | Qué hace |
|---|---|
| `api-docs.path` | Expone la especificación OpenAPI en formato JSON en `/api-docs` |
| `swagger-ui.path` | Expone la interfaz gráfica de Swagger en `/swagger-ui.html` |
| `tags-sorter: alpha` | Ordena los grupos de endpoints alfabéticamente |
| `operations-sorter: alpha` | Ordena los endpoints dentro de cada grupo alfabéticamente |
| `display-request-duration` | Muestra cuánto tardó cada petición al probarla desde Swagger |

### Clase de configuración — `OpenApiConfig.java`

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sportLifeOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("SportLife API")
                        .description("API REST para la tienda virtual SportLife — MVP")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
```

Esta configuración hace dos cosas importantes:
1. Define los metadatos de la API (título, descripción, versión).
2. Registra el esquema de seguridad JWT en Swagger, de modo que el botón **"Authorize"** en la UI permita pegar el token y que todas las peticiones de prueba lo incluyan automáticamente en el header `Authorization: Bearer ...`.

### URLs disponibles tras arrancar el proyecto

| URL | Qué muestra |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interfaz gráfica interactiva para probar los endpoints |
| `http://localhost:8080/api-docs` | Especificación OpenAPI en JSON (para importar en Postman, etc.) |

---

## 5. Variables de entorno recomendadas

Nunca se deben hardcodear credenciales en `application.yaml`. Las variables con `${VAR:default}` usan el valor de la variable de entorno si existe, o el valor por defecto si no.

| Variable | Descripción | Valor por defecto (dev) |
|---|---|---|
| `DB_URL` | URL completa de PostgreSQL | `jdbc:postgresql://localhost:5432/sportlife` |
| `DB_USERNAME` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `postgres` |
| `MONGO_URI` | URI de conexión a MongoDB | `mongodb://localhost:27017/sportlife` |
| `MONGO_DB` | Nombre de la base de datos en MongoDB | `sportlife` |
| `JWT_SECRET` | Clave secreta para firmar tokens JWT | Valor de desarrollo (cambiar en producción) |
| `JWT_EXPIRATION` | Segundos de validez del token | `3600` (1 hora) |

En producción estas variables se configuran en el servidor, en un gestor de secretos (AWS Secrets Manager, Vault) o en las variables de entorno del contenedor Docker, **nunca en el repositorio de código**.
