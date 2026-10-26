# Recordatorios Citas Service

## Descripción
Repositorio del post-contenido de la Unidad 9 de Patrones de Diseño de Software — Sexto Semestre. Un único proyecto Spring Boot que envía recordatorios de citas por el canal preferido del paciente, construido desde el inicio con Strategy (`ReminderChannel`) y Factory Method (`ReminderChannelFactory`) inyectados por constructor.

## Parte 1 — Pruebas Unitarias con JUnit 5 y Mockito
`CitaReminderServiceImplTest` aísla `CitaReminderServiceImpl` de sus tres colaboradores. Ver "Decisiones de diseño" para la justificación de por qué cada uno se mockeó, se stubeó o se resolvió con un fake.

## Decisiones de diseño

### Justificación de Test Doubles para cada Colaborador

- **`CitaRepository` -> Mock**: No basta con stubbear `findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(...)` para que devuelva una lista de citas; también es indispensable VERIFICAR que `save()` se invoca exactamente una vez por cada cita procesada y con el atributo `recordatorioEnviado` actualizado a `true`. Cuando una prueba unitaria requiere tanto prefijar respuestas como verificar interacciones y efectos colaterales de salida, la categoría formal es Mock, no un simple Stub pasivo.
- **`ReminderChannelFactory` / `ReminderChannel` -> Mock**: El comportamiento central bajo prueba es la resolución dinámica de la estrategia correcta: que el servicio solicite a la fábrica el canal `EMAIL` cuando la cita así lo prefiere, y que la implementación devuelta reciba la llamada al método `enviar(...)` con la cita y el mensaje correspondiente. Esto se valida mediante la verificación de interacción (`verify`) sobre el canal y la fábrica, no inspeccionando un valor de retorno.
- **`Clock` -> Fake (no Mock)**: Mockear `java.time.Clock` mediante Mockito exigiría stubbear individualmente `instant()` y `getZone()`, con el riesgo latente de que métodos derivados como `LocalDateTime.now(clock)` se comporten de forma anómala o inconsistente. `Clock.fixed(...)` ya es una implementación completa, inmutable y determinista provista directamente por el JDK: utilizarla como Fake es más simple, legible, seguro y fiel a la semántica real del runtime.

### Impacto de los Patrones de Diseño en la Testabilidad

- **Por qué Strategy facilitó las pruebas**: Sin `ReminderChannel` como abstracción de estrategia, la lógica de transporte de mensajería (clientes SMTP para correo o librerías HTTP para SMS) estaría acoplada de manera monolítica dentro del servicio de recordatorios. Probar la selección de canal obligaría a mockear componentes de bajo nivel del protocolo de red. Gracias al patrón Strategy, un único `@Mock ReminderChannel` basta para comprobar cualquier variante de canal sin acoplarse al detalle de transporte.
- **Por qué Factory facilitó las pruebas**: Sin `ReminderChannelFactory`, el servicio contendría un bloque condicional (`if/else` o `switch`) instanciando o seleccionando directamente el canal concreto. Dicha selección solo podría observarse de forma indirecta. Con la fábrica inyectada como colaborador, la decisión de selección de canal se comprueba explícitamente mediante una interacción observable: `verify(reminderChannelFactory).obtenerCanal(CanalNotificacion.EMAIL)`.

## Ejecutar las pruebas
```bash
mvn test # Pruebas unitarias
```
