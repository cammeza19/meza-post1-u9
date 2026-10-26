# ADR 0003: Selección Dinámica de Estrategias mediante Patrón Factory Method

## Estado
Aceptado

## Contexto
Dado que el paciente define dinámicamente en su perfil el canal preferido para recibir notificaciones (`CanalNotificacion.EMAIL` o `CanalNotificacion.SMS`), el servicio `CitaReminderServiceImpl` debe resolver en tiempo de ejecución qué estrategia de `ReminderChannel` corresponde a cada cita procesada.

Si el servicio tuviera que instanciar o mapear manualmente las implementaciones concretas, se rompería el Principio de Inversión de Dependencias (DIP) y se dificultaría la observación de la toma de decisiones en pruebas unitarias.

## Decisión
Se implementa el patrón de creación **Factory Method**:
1. Se define la interfaz de fábrica `ReminderChannelFactory` con el método `ReminderChannel obtenerCanal(CanalNotificacion tipo)`.
2. Se implementa la fábrica concreta `ReminderChannelFactoryImpl` anotada con `@Component`, la cual recibe por inyección de dependencias en su constructor las implementaciones registradas en el contenedor (`EmailReminderChannel` y `SmsReminderChannel`).
3. El servicio `CitaReminderServiceImpl` recibe por constructor la abstracción `ReminderChannelFactory` y delega en ella la resolución del canal para cada cita.

## Alternativas Descartadas

- **Construcción manual de canales dentro de la fábrica mediante operador `new`**:
  - Si la fábrica creara `new EmailReminderChannel()` o `new SmsReminderChannel(...)` internamente, no podría aprovechar el ciclo de vida, configuración e inyección de beans gestionados por Spring (por ejemplo, inyectar el `NotificadorSmsGateway` a `SmsReminderChannel`). La fábrica recibe las estrategias por constructor, manteniendo el Composition Root en Spring.
- **Switch embebido directamente dentro del servicio**:
  - Resolver el canal directamente dentro de `enviarRecordatoriosPendientes()` mediante un switch interno. Se descartó porque la decisión de selección de estrategia quedaría oculta y no sería observable directamente en pruebas unitarias; solo se podría verificar observando efectos secundarios remotos o impresiones en consola. Con la fábrica, la invocación `verify(reminderChannelFactory).obtenerCanal(CanalNotificacion.EMAIL)` hace explícita y auditable la decisión.

## Consecuencias

### Positivas
- **Verificabilidad explícita**: En pruebas unitarias, se puede asegurar fehacientemente que el servicio consultó a la fábrica con el valor enum exacto preferido por el paciente, mediante `verify(reminderChannelFactory).obtenerCanal(...)`.
- **Composición modular**: Las estrategias pueden ser enriquecidas por el contenedor de Spring sin afectar ni a la fábrica ni al servicio de recordatorios.

### Negativas
- Añade una indirección adicional entre el servicio y los canales de comunicación.
