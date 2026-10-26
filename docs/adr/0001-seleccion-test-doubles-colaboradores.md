# ADR 0001: Selección Justificada de Test Doubles para los Colaboradores del Servicio

## Estado
Aceptado

## Contexto
El servicio central `CitaReminderServiceImpl` orquesta el envío automatizado de recordatorios para citas médicas programadas dentro de las próximas 24 horas que no han sido notificadas. Para cumplir su responsabilidad, interactúa con tres colaboradores esenciales inyectados por constructor:
1. `CitaRepository`: Acceso a la capa de persistencia para consultar y actualizar el estado de las citas.
2. `ReminderChannelFactory`: Fábrica encargada de resolver la estrategia de canal según la preferencia del paciente (`EMAIL` o `SMS`), devolviendo una instancia de `ReminderChannel`.
3. `Clock`: Abstracción temporal para obtener la fecha y hora de evaluación de la ventana de recordatorios.

Para aislar el servicio en pruebas unitarias deterministas y confiables con JUnit 5 y Mockito, se debe decidir qué tipo formal de Test Double (Dummy, Stub, Spy, Mock o Fake) emplear para cada colaborador, evitando duplicación innecesaria y pruebas frágiles.

## Decisión
Se establecen formalmente los siguientes Test Doubles:

1. **`CitaRepository` -> Mock (con Mockito `@Mock`)**:
   - Se utiliza un Mock y no un simple Stub pasivo porque la prueba unitaria no solo necesita programar las respuestas indirectas de consulta (`findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse`), sino primordialmente **verificar las interacciones y efectos colaterales de salida**. Específicamente, se requiere certificar mediante `verify(citaRepository, times(1)).save(cita)` que cada entidad procesada es persistida exactamente una vez y que su estado `recordatorioEnviado` ha cambiado a `true`. Además, en escenarios de lista vacía o error, se debe verificar que `save()` nunca fue invocado (`never().save(any())`).

2. **`ReminderChannelFactory` y `ReminderChannel` -> Mock (con Mockito `@Mock`)**:
   - El comportamiento crítico bajo prueba es la resolución dinámica de la estrategia correcta y el posterior envío del mensaje. En `ReminderChannelFactory`, se verifica que el método `obtenerCanal(canalEsperado)` se ejecute con el tipo enum exacto. En `ReminderChannel`, el método `enviar(cita, mensaje)` tiene retorno `void` (comando impuro con efectos externos), por lo que la única manera de comprobar su correcto uso es verificando la interacción con `verify(reminderChannelMock).enviar(eq(cita), anyString())`.

3. **`Clock` -> Fake (`Clock.fixed(...)` del JDK)**:
   - Se utiliza `Clock.fixed(...)` como Fake y se prohíbe explícitamente mockearlo con Mockito. Un objeto Fake es una implementación real, funcional y simplificada que no depende de infraestructura volátil. `Clock.fixed(Instant, ZoneId)` es provisto por la propia biblioteca estándar `java.time`, garantizando fidelidad matemática y temporal total.

## Alternativas Descartadas

- **Usar un Stub para `CitaRepository`**:
  - Un Stub únicamente suministra respuestas enlatadas a llamadas de entrada y no almacena ni valida interacciones de salida. Si se usara un Stub, no se podría garantizar que el servicio realmente actualizó y persistió la entidad en la base de datos tras enviar la notificación, lo que permitiría que bugs de pérdida de actualización pasaran desapercibidos.
- **Mockear `Clock` con Mockito (`@Mock Clock`)**:
  - Mockear `Clock` exige stubbear por separado los métodos primitivos `clock.instant()` y `clock.getZone()`. Cualquier método utilitario o clase externa que invoque `clock.millis()` o derivaciones de zona horaria fallaría o devolvería nulos. Además, agrega fragilidad y complejidad innecesarias frente a un Fake nativo de una sola línea.
- **Usar `Clock.systemDefaultZone()` en pruebas**:
  - Emplear el reloj del sistema genera pruebas no deterministas (flaky tests) que cambian sus valores de frontera de consulta en cada milisegundo y hora de ejecución, haciendo imposible validar con exactitud los límites de la ventana en `desdeCaptor` y `hastaCaptor`.

## Consecuencias

### Positivas
- Aislamiento unitario absoluto de `CitaReminderServiceImpl` sin levantar contextos de Spring ni dependencias externas.
- Determinismo total en el cálculo de intervalos de tiempo (`ahora` y `en24Horas`), reproducible en cualquier entorno de CI o huso horario.
- Cobertura precisa de efectos colaterales (llamadas a `save()` y `enviar()`) mediante aserciones de interacción de Mockito.

### Negativas
- El refactor de firmas en los métodos de `CitaRepository` o `ReminderChannel` requiere actualizar las verificaciones de Mockito en la suite unitaria.
