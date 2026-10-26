# Guía de Pruebas Automatizadas y Verificación

Esta guía describe detalladamente la estrategia de verificación, ejecución y validación de las suites de pruebas unitarias y de integración del proyecto `recordatorios-citas-service`.

---

## 1. Comandos de Ejecución

### En Windows (PowerShell)
```powershell
# Asegurar JAVA_HOME apuntando a JDK 21
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"

# Compilación y verificación de sintaxis
.\mvnw.cmd compile

# Ejecución exclusiva de pruebas unitarias (Parte 1 y 2)
.\mvnw.cmd test

# Ejecución completa (unitarias + integración con H2 y Spring Context + JaCoCo)
.\mvnw.cmd verify

# Generación explícita del reporte de cobertura JaCoCo
.\mvnw.cmd jacoco:report
```

### En Linux / macOS / Bash
```bash
# Compilación y verificación de sintaxis
./mvnw compile

# Ejecución exclusiva de pruebas unitarias
./mvnw test

# Ejecución completa (unitarias + integración + JaCoCo)
./mvnw verify

# Generación explícita del reporte de cobertura JaCoCo
./mvnw jacoco:report
```

---

## 2. Inventario de Pruebas y Cobertura

El proyecto cuenta con **8 pruebas automatizadas** organizadas en tres suites complementarias:

| Suite / Clase de Prueba | Tipo de Prueba | Anotación Principal | Métodos de Prueba | Qué Verifica |
|---|---|---|---|---|
| `CitaReminderServiceImplTest` | Unitaria (Aislada) | `@ExtendWith(MockitoExtension.class)` | 6 | Aislamiento estricto de `CitaReminderServiceImpl`. Selección de canal por estrategia y fábrica, ventana de 24h determinista con Fake Clock, y manejo de recordatorios urgentes (éxito, fallo de gateway y cita inexistente). |
| `CitaRepositoryTest` | Integración (Persistencia) | `@DataJpaTest` | 1 | Comportamiento del repositorio Spring Data JPA contra base de datos en memoria H2. Filtro estricto por rango temporal y exclusión de citas ya notificadas. |
| `CitaReminderServiceIntegrationTest` | Integración (Contexto Spring) | `@SpringBootTest` | 1 | Inicialización del contexto completo de Spring Boot, persistencia real en H2, sobreescritura de `Clock` con Fake y mockeo de la pasarela SMS externa mediante `@MockBean`. |

---

## 3. Detalle de Casos de Prueba por Método

### A. Suite Unitaria: `CitaReminderServiceImplTest`
Ruta: `src/test/java/com/clinica/recordatorios/service/CitaReminderServiceImplTest.java`

1. **`enviarRecordatoriosPendientes_citaEnVentana_usaElCanalPreferido`**:
   - **Condición**: Existe una cita dentro de la ventana de 24 horas (`2026-08-10T15:00`) con preferencia `EMAIL` y `recordatorioEnviado = false`.
   - **Acción**: Ejecutar `enviarRecordatoriosPendientes()`.
   - **Resultado esperado**: Retorna `1`, invoca `reminderChannelMock.enviar(...)` exactamente una vez con la cita, invoca `citaRepository.save(cita)`, y la cita queda con `recordatorioEnviado = true`.

2. **`enviarRecordatoriosPendientes_ventanaDeterministaSegunClockFijo_consultaLosLimitesExactos`**:
   - **Condición**: Reloj fijado en `2026-08-10T09:00:00Z` mediante el Fake `Clock.fixed(...)`.
   - **Acción**: Ejecutar `enviarRecordatoriosPendientes()`.
   - **Resultado esperado**: Mediante `ArgumentCaptor`, se verifica que `findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse` fue invocado con límite inferior exacto `2026-08-10T09:00` y límite superior exacto `2026-08-11T09:00`.

3. **`enviarRecordatoriosPendientes_sinCitasPendientes_noConsultaLaFactory`**:
   - **Condición**: El repositorio no retorna ninguna cita pendiente.
   - **Acción**: Ejecutar `enviarRecordatoriosPendientes()`.
   - **Resultado esperado**: Retorna `0`, se verifica que `reminderChannelFactory` no recibió interacciones (`verifyNoInteractions`) y el repositorio nunca guardó nada (`verify(citaRepository, never()).save(...)`).

4. **`enviarRecordatorioUrgente_citaExistente_usaCanalUrgenteInyectado`**:
   - **Condición**: Existe una cita en base de datos con id `5L`.
   - **Acción**: Invocar `enviarRecordatorioUrgente(5L)`.
   - **Resultado esperado**: El canal urgente inyectado recibe `enviar(...)` con el texto que contiene "URGENTE", se guarda la cita en el repositorio, y su indicador queda en `true`.

5. **`enviarRecordatorioUrgente_gatewayFalla_noMarcaComoEnviado`**:
   - **Condición**: La pasarela o canal urgente lanza una excepción (`RuntimeException("Gateway de SMS no disponible")`).
   - **Acción**: Invocar `enviarRecordatorioUrgente(6L)`.
   - **Resultado esperado**: Se propaga la excepción hacia el llamador y se verifica explícitamente que la cita **NUNCA** fue guardada como enviada (`verify(citaRepository, never()).save(any())`). Demuestra la necesidad del refactor DIP.

6. **`enviarRecordatorioUrgente_citaInexistente_lanzaCitaNoEncontradaException`**:
   - **Condición**: El repositorio retorna `Optional.empty()` para el id solicitado (`99L`).
   - **Acción**: Invocar `enviarRecordatorioUrgente(99L)`.
   - **Resultado esperado**: Lanza la excepción de dominio `CitaNoEncontradaException` y no interactúa con el canal urgente (`verifyNoInteractions(canalUrgenteMock)`).

---

### B. Suite de Persistencia: `CitaRepositoryTest`
Ruta: `src/test/java/com/clinica/recordatorios/repository/CitaRepositoryTest.java`

1. **`findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse_soloRetornaCitasPendientesEnVentana`**:
   - **Condición**: Se insertan en la base H2 tres registros:
     - Cita 1: dentro de ventana (día 10 a las 12:00), `recordatorioEnviado = false`.
     - Cita 2: dentro de ventana (día 10 a las 14:00), `recordatorioEnviado = true`.
     - Cita 3: fuera de ventana (día 20 a las 09:00), `recordatorioEnviado = false`.
   - **Acción**: Consultar entre `2026-08-10T00:00` y `2026-08-11T00:00`.
   - **Resultado esperado**: Retorna una lista con exactamente 1 elemento correspondiente a la Cita 1 ("Dentro de ventana, pendiente").

---

### C. Suite de Contexto Completo: `CitaReminderServiceIntegrationTest`
Ruta: `src/test/java/com/clinica/recordatorios/CitaReminderServiceIntegrationTest.java`

1. **`enviarRecordatoriosPendientes_contextoCompletoDeSpring_persisteYNotificaCitasEnVentana`**:
   - **Condición**: Contexto de Spring levantado con beans reales (`CitaRepository`, `CitaReminderServiceImpl`, etc.), `Clock` sobreescrito con `@Primary Clock.fixed(...)` y `NotificadorSmsGateway` mockeado con `@MockBean`. Se persiste en H2 una cita pendiente para las `2026-08-10T18:00`.
   - **Acción**: Invocar `citaReminderService.enviarRecordatoriosPendientes()`.
   - **Resultado esperado**: Retorna `1`, el mock de `notificadorSmsGateway` recibe la llamada `enviarSms(...)` y en la base H2 real el registro queda efectivamente persistido con `recordatorioEnviado = true`.

---

## 4. Reporte de Cobertura JaCoCo

Al ejecutar `mvn verify`, el plugin `jacoco-maven-plugin` genera el reporte interactivo en:
- `target/site/jacoco/index.html`

El reporte demuestra un **100% de cobertura de instrucciones y ramas** sobre la lógica de orquestación y reglas de negocio del servicio `CitaReminderServiceImpl`.
