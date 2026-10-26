# ADR 0006: Pipeline de Integración Continua sin Contenedores de Base de Datos Externos

## Estado
Aceptado

## Contexto
El pipeline de Integración Continua (`.github/workflows/ci.yml`) debe compilar, empaquetar y validar rigurosamente toda la suite de pruebas unitarias y de integración del proyecto en cada `push` o `pull_request` a la rama principal (`main`).

Se debe evaluar si el archivo de workflow de GitHub Actions requiere configurar bloques de servicios de infraestructura externa (como `services: postgres` con contenedores Docker o Testcontainers) o si una ejecución nativa sobre el runner de Ubuntu es suficiente.

## Decisión
Se adopta un **pipeline liviano y determinista sin servicios de contenedores externos**:
1. El workflow define un único job `build-and-test` ejecutado en `ubuntu-latest`.
2. Se aprovisiona Java 21 LTS (distribución Temurin) con caché nativo de Maven (`actions/setup-java@v4`).
3. Se ejecuta directamente el comando:
   ```bash
   mvn -B verify --no-transfer-progress
   ```
4. No se incluye ningún contenedor auxiliar de base de datos gracias a la elección estratégica de **H2 en memoria** como Fake de persistencia para `@DataJpaTest` y `@SpringBootTest`.

## Alternativas Descartadas

- **Aprovisionar contenedores Docker mediante `services:` (ej. PostgreSQL)**:
  - Añadiría una sobrecarga considerable al pipeline: tiempo de descarga de imágenes Docker, inicialización de puertos, verificación de disponibilidad (health checks), y mayor fragilidad ante timeouts en los runners de GitHub Actions. Dado que la aplicación no utiliza extensiones propietarias de SQL ni tipos de datos propietarios, el motor H2 satisface con creces los requerimientos de verificación.
- **Utilizar Testcontainers en las pruebas de integración**:
  - Exigiría Docker-in-Docker o acceso al daemon de Docker en el host, aumentando los tiempos de ciclo y el consumo de recursos de computación sin aportar valor adicional sobre la lógica de negocio y mapeo JPA evaluados.

## Consecuencias

### Positivas
- **Velocidad de ejecución**: El pipeline concluye en menos de un minuto, habilitando ciclos de retroalimentación inmediata (Fast Feedback).
- **Simplicidad operativa**: El archivo `ci.yml` es conciso, legible y fácil de mantener, reduciendo puntos de fallo en la infraestructura de CI.
- **Portabilidad**: El mismo comando `mvn verify` corre idénticamente en estaciones locales de desarrollo (Windows/Linux/macOS) y en el agente de integración continua.

### Negativas
- En caso de que a futuro se requieran procedimientos almacenados o sintaxis propietaria del motor final de producción, será necesario migrar a contenedores dedicados.
