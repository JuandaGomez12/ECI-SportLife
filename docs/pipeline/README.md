# Pipeline CI/CD — ECI SportLife

## ¿Qué es un pipeline de CI/CD?

**CI (Continuous Integration)** significa que cada vez que un desarrollador sube código, el sistema automáticamente lo compila, lo prueba y lo analiza. El objetivo es detectar errores lo antes posible, antes de que lleguen a producción.

**CD (Continuous Delivery/Deployment)** es la extensión de CI: si todos los pasos anteriores pasan correctamente, el sistema despliega automáticamente la aplicación al servidor.

La regla de oro: **si el pipeline falla, el código no avanza**. Ningún código llega a producción sin haber pasado primero por build, tests y análisis.

---

## Estrategia de ramas del proyecto

```
main          ← producción (solo recibe merges de develop)
  └── develop ← integración (recibe merges de feature/*)
        ├── feature/auth
        ├── feature/products-crud
        ├── feature/cart
        └── feature/checkout
```

Esta separación permite que:
- Los desarrolladores trabajen en ramas aisladas sin afectarse entre sí.
- `develop` sea siempre un estado integrado y verificado de todo el equipo.
- `main` sea siempre el estado que está corriendo en producción.

---

## Cuándo se ejecuta cada pipeline

| Evento | Pipeline que se activa | Etapas |
|---|---|---|
| Merge de `feature/*` → `develop` | `ci-develop.yml` | Build → Test → Analysis |
| Merge de `develop` → `main` | `ci-main.yml` | Build → Test → Analysis → Package → Deploy |

GitHub Actions detecta estos eventos como un `push` a la rama destino: cuando haces merge de una rama a `develop`, GitHub lo registra como un nuevo commit en `develop` y eso dispara el workflow.

---

## Pipeline 1: `ci-develop.yml` (feature → develop)

### Propósito
Verificar que el código integrado en `develop` compila, pasa todas las pruebas y cumple los estándares de calidad. **No despliega** — develop es un entorno de integración, no producción.

### Pasos

```
push a develop
      │
      ▼
┌─────────────────────────┐
│  1. Checkout del código │  Descarga el repositorio completo
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│  2. Configurar Java 21  │  Instala el JDK y cachea dependencias Maven
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│  3. Build               │  mvn clean compile — verifica que compila
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│  4. Test + JaCoCo       │  mvn test verify — ejecuta JUnit y mide cobertura
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│  5. Subir reporte HTML  │  El reporte de cobertura queda descargable en GitHub
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│  6. Análisis SonarQube  │  mvn sonar:sonar — detecta bugs, code smells, deuda técnica
└─────────────────────────┘
```

### ¿Qué mide JaCoCo?

JaCoCo (Java Code Coverage) mide qué porcentaje del código fuente fue ejecutado durante los tests:

| Métrica | Qué mide |
|---|---|
| **Line coverage** | % de líneas de código ejecutadas |
| **Branch coverage** | % de ramas de `if/else` probadas |
| **Method coverage** | % de métodos que fueron llamados |

El reporte queda guardado en `target/site/jacoco/index.html` y se puede descargar desde la pestaña **Artifacts** en GitHub Actions.

### ¿Qué revisa SonarQube?

SonarQube analiza el código estáticamente (sin ejecutarlo) y reporta:

| Categoría | Ejemplo |
|---|---|
| **Bugs** | Posibles NullPointerException, comparaciones incorrectas |
| **Vulnerabilidades** | Contraseñas en texto plano, SQL injection sin parametrizar |
| **Code Smells** | Métodos demasiado largos, código duplicado, variables sin usar |
| **Cobertura** | Integra el reporte de JaCoCo para mostrar cobertura en el dashboard |

---

## Pipeline 2: `ci-main.yml` (develop → main)

### Propósito
Verificar el código que va a producción y desplegarlo automáticamente si todo pasa. Tiene **dos jobs** separados: el primero hace lo mismo que el pipeline de develop más el empaquetado; el segundo despliega **solo si el primero fue exitoso**.

### Pasos

```
push a main
      │
      ▼
┌──────────────────────────────────────────┐
│         JOB 1: build-test-analysis       │
│                                          │
│  1. Checkout → 2. Java 21 → 3. Build    │
│  4. Test + JaCoCo → 5. Subir reporte   │
│  6. SonarQube → 7. Package (.jar)       │
│  8. Subir .jar como artefacto           │
└──────────────────┬───────────────────────┘
                   │ needs: build-test-analysis
                   │ (solo continúa si job 1 pasó)
                   ▼
┌──────────────────────────────────────────┐
│              JOB 2: deploy               │
│                                          │
│  9.  Descargar .jar del artefacto       │
│  10. Detener servicio en el servidor    │
│  11. Copiar .jar al servidor vía SCP    │
│  12. Arrancar el servicio con java -jar │
└──────────────────────────────────────────┘
```

### La palabra clave `needs`

```yaml
deploy:
  needs: build-test-analysis
```

Esto le dice a GitHub Actions que el job `deploy` **no puede correr** hasta que `build-test-analysis` termine exitosamente. Si los tests fallan o SonarQube detecta errores críticos, el deploy nunca ocurre. Así se garantiza que producción siempre tiene código verificado.

---

## Secrets requeridos

Los workflows usan variables secretas que nunca deben estar en el código. Se configuran en **GitHub → Settings → Secrets and variables → Actions**.

| Secret | Usado en | Descripción |
|---|---|---|
| `SONAR_TOKEN` | Ambos pipelines | Token de autenticación de SonarQube/SonarCloud |
| `SONAR_HOST_URL` | Ambos pipelines | URL del servidor SonarQube (ej: `https://sonarcloud.io`) |
| `DEPLOY_HOST` | `ci-main.yml` | IP o dominio del servidor de producción |
| `DEPLOY_USER` | `ci-main.yml` | Usuario SSH del servidor |
| `DEPLOY_SSH_KEY` | `ci-main.yml` | Clave privada SSH para conectarse al servidor |
| `JWT_SECRET` | `ci-main.yml` | Secreto para firmar los tokens JWT en producción |
| `JWT_EXPIRATION` | `ci-main.yml` | Tiempo de expiración del token en ms |

> **Importante:** nunca pongas contraseñas, tokens o IPs directamente en los archivos `.yml`. GitHub cifra los secrets y los inyecta como variables de entorno solo en tiempo de ejecución.

---

## Flujo completo de trabajo

```
Desarrollador trabaja en feature/nueva-funcionalidad
         │
         │  git merge feature/... develop
         ▼
    push a develop
         │
         ▼
  [ci-develop.yml]
  Build ✓ → Test ✓ → Analysis ✓
         │
         │  (cuando develop está listo para producción)
         │  git merge develop main
         ▼
    push a main
         │
         ▼
  [ci-main.yml]
  Build ✓ → Test ✓ → Analysis ✓ → Package ✓
         │
         ▼
       Deploy ✓
         │
         ▼
  Aplicación corriendo en el servidor de producción
```

---

## Dónde ver los resultados

1. Ir al repositorio en GitHub.
2. Clic en la pestaña **Actions**.
3. Se lista cada ejecución del pipeline con su estado (verde = pasó, rojo = falló).
4. Dentro de cada ejecución se puede ver el log de cada paso y descargar los artefactos (reporte JaCoCo, `.jar`).
