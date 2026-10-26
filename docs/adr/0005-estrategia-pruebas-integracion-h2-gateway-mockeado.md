# ADR 0005: Estrategia de Pruebas de Integración: Base de Datos H2 Real vs Gateway SMS Mockeado

## Estado
Aceptado

## Contexto
En la prueba de integración de contexto completo `CitaReminderServiceIntegrationTest` (`@SpringBootTest`), surge una aparente paradoja arquitectónica:
- Se utiliza un motor de base de datos relacional real en memoria (H2).
- Simultáneamente, se sustituye el gateway de SMS con un `@MockBean private NotificadorSmsGateway notificadorSmsGateway`.

Es fundamental analizar si esta coexistencia es metodológicamente coherente o si representa una contradicción en la estrategia de pruebas de integración.

## Decisión
Se ratifica como válida y óptima la combinación de **H2 como Fake de persistencia** junto con un **MockBean para la pasarela de SMS externa**:

1. **H2 como Fake de persistencia**:
   - Una base de datos relacional en memoria (H2) implementa de manera completa y fiel los estándares SQL, transaccionales y de mapeo objeto-relacional (JPA/Hibernate) equivalentes a motores de producción relacionales.
   - Es liviana, determinista, arranca en milisegundos, no requiere instalación de demonios ni puertos de red externos, y se limpia automáticamente entre ejecuciones. Cumple a cabalidad con la definición formal de **Fake**.

2. **`NotificadorSmsGateway` como Mock (`@MockBean`)**:
   - A diferencia de las bases de datos relacionales (que cuentan con implementaciones estándar equivalentes como H2), los proveedores comerciales de mensajería SMS (como Twilio o AWS SNS) **no disponen de un sustituto en memoria estandarizado y determinista**.
   - Conectarse al proveedor real en cada ejecución de prueba exigiría credenciales de producción, incurriría en costos económicos por mensaje, requeriría conectividad de red ilimitada y convertiría la suite de pruebas en no determinista ante eventuales caídas del servicio externo.
   - El `@MockBean` permite verificar de manera determinista que el flujo completo de Spring orquestó correctamente la invocación a la pasarela sin generar costos ni efectos secundarios externos.

## Alternativas Descartadas

- **Mockear `CitaRepository` también en la prueba de integración**:
  - Desvirtuaría por completo el propósito de `@SpringBootTest` y `@DataJpaTest`, impidiendo validar que las consultas derivadas de Spring Data JPA (`findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse`) se traduzcan a sentencias SQL correctas y que las transacciones y mapeos `@Entity` funcionen en el runtime real.
- **Utilizar un proveedor SMS real con credenciales de prueba en CI**:
  - Inviable debido a costos acumulativos, necesidad de exponer secretos o API tokens en pipelines, y vulnerabilidad a fallos por cuotas o latencias de red externa.

## Consecuencias

### Positivas
- Se valida la integración real entre Spring Data JPA, Hibernate, el dialecto SQL y el esquema de base de datos en memoria.
- Se garantiza la independencia absoluta respecto a redes externas y proveedores terceros.
- Las pruebas de integración se ejecutan de manera rápida y determinista en cualquier máquina de desarrollo y en el entorno de CI.

### Negativas
- Posibles diferencias dialectales muy específicas de SQL entre H2 y motores como PostgreSQL/Oracle (mitigadas al apegarse al estándar JPA puro).
