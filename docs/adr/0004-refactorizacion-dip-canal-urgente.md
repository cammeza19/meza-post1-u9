# ADR 0004: Refactorización hacia Inversión de Dependencias (DIP) en Recordatorios Urgentes

## Estado
Aceptado

## Contexto
En la Parte 2 se introdujo la funcionalidad de recordatorios urgentes para citas próximas dentro de las siguientes dos horas: `enviarRecordatorioUrgente(Long citaId)`.

En su diseño inicial de partida, el método contenía la siguiente construcción directa:
```java
ReminderChannel canalUrgente = new SmsReminderChannel(new TwilioNotificadorSmsGateway());
```
Este código presentaba un problema de diseño crítico: violaba flagrantemente el **Principio de Inversión de Dependencias (DIP)**. Al utilizar el operador `new` directamente dentro de la lógica del método de negocio, `CitaReminderServiceImpl` dependía de dos implementaciones concretas (`SmsReminderChannel` y `TwilioNotificadorSmsGateway`) en lugar de depender de abstracciones.

Como consecuencia directa, **resultaba completamente imposible escribir una prueba unitaria para el escenario donde la pasarela de SMS fallaba**:
- Si la red o el servicio SMS no respondían, la excepción impedía completar la operación, pero no existía ningún punto de extensión o "seam" donde Mockito pudiera inyectar un test double que simulara dicha excepción.
- La prueba `enviarRecordatorioUrgente_gatewayFalla_noMarcaComoEnviado()` era irrealizable sin ejecutar código real de Twilio y sin alterar el estado real de la base de datos.
- Intentar interceptar la construcción mediante trucos avanzados de bytecode como `Mockito.mockConstruction()` es un síntoma de mal diseño, no una solución arquitectónica limpia.

## Decisión
Se refactoriza la arquitectura aplicando el Principio de Inversión de Dependencias y el concepto de **Composition Root**:
1. Se traslada la construcción de la dependencia al Composition Root de Spring en una clase de configuración dedicada: `CanalUrgenteConfig` anotada con `@Configuration`.
   ```java
   @Configuration
   public class CanalUrgenteConfig {
       @Bean
       public ReminderChannel canalUrgente(NotificadorSmsGateway gateway) {
           return new SmsReminderChannel(gateway);
       }
   }
   ```
2. Se inyecta la abstracción `ReminderChannel` resultante por constructor en `CitaReminderServiceImpl`, utilizando el calificador `@Qualifier("canalUrgente")` para distinguirlo de otras estrategias registradas.
3. Se elimina cualquier operador `new` dentro del método `enviarRecordatorioUrgente(Long citaId)`, dejando la lógica de negocio limpia y dependiente de interfaces.

## Alternativas Descartadas

- **Mantener el operador `new` y usar `Mockito.mockConstruction()`**:
  - Mockito 5 permite interceptar llamadas al constructor mediante mock maker inline. Se descartó enérgicamente porque enmascara un error de diseño estructural bajo una herramienta de prueba avanzada, aumentando la complejidad del runtime y el acoplamiento al compilador.
- **Inyección por campo (`@Autowired private ReminderChannel canalUrgente;`)**:
  - Viola los principios de diseño de inyección limpia: oculta las dependencias reales del objeto, impide la inmutabilidad de los campos (`final`), y dificulta la instanciación directa del servicio en pruebas unitarias puras sin contenedor de Spring.

## Consecuencias

### Positivas
- **Testabilidad 100% habilitada**: Ahora es posible inyectar un `@Mock private ReminderChannel canalUrgenteMock` en el constructor del servicio en `@BeforeEach`, permitiendo verificar el comportamiento tanto en casos de éxito (`enviarRecordatorioUrgente_citaExistente_usaCanalUrgenteInyectado`) como en escenarios de excepción simulada (`enviarRecordatorioUrgente_gatewayFalla_noMarcaComoEnviado`).
- **Segregación de responsabilidades**: La lógica de negocio (`service`) desconoce los detalles de ensamblado, los cuales quedan centralizados exclusivamente en el paquete `config/`.
- **Inmutabilidad**: Todos los campos del servicio se mantienen inmutables (`private final`).

### Negativas
- Requiere el uso de `@Qualifier("canalUrgente")` para resolver la ambigüedad en el contenedor de Spring, debido a la coexistencia de múltiples beans que implementan `ReminderChannel`.
