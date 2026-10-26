# ADR 0002: Desacoplamiento de Mecanismos de Notificación mediante Patrón Strategy

## Estado
Aceptado

## Contexto
El sistema de recordatorios de citas clínicas requiere soportar múltiples canales de comunicación con los pacientes, tales como correo electrónico (`EmailReminderChannel`) y mensajería SMS (`SmsReminderChannel`), permitiendo en el futuro incorporar canales adicionales (como WhatsApp o notificaciones push) sin alterar la lógica de negocio central. 

Si el servicio `CitaReminderServiceImpl` contuviera directamente la lógica de conexión SMTP o llamadas a APIs REST externas de proveedores SMS, la clase violaría el Principio de Responsabilidad Única (SRP) y resultaría sumamente difícil de probar en aislamiento.

## Decisión
Se implementa el patrón de diseño de comportamiento **Strategy**:
1. Se define la interfaz común de estrategia `ReminderChannel` con el contrato de operación `void enviar(Cita cita, String mensaje)`.
2. Cada canal concreto implementa dicha interfaz de forma autocontenida:
   - `EmailReminderChannel`: Encapsula el envío de correos electrónicos.
   - `SmsReminderChannel`: Encapsula el envío de mensajes SMS delegando en una pasarela `NotificadorSmsGateway`.
3. El servicio `CitaReminderServiceImpl` interactúa exclusivamente con la abstracción `ReminderChannel`, ignorando por completo los detalles de protocolo, transporte o autenticación.

## Alternativas Descartadas

- **Lógica de notificación embebida con condicionales dentro del servicio**:
  - Implementar bloques `if (cita.getCanalPreferido() == EMAIL) { ... } else if (...) { ... }` directamente dentro de `enviarRecordatoriosPendientes()`. Esta alternativa acopla fuertemente el servicio a las librerías de infraestructura SMTP/HTTP, violando el Principio Abierto/Cerrado (OCP) y haciendo imposible probar el servicio sin levantar clientes de red o mockear clases complejas de bajo nivel.
- **Jerarquía de herencia con clases base abstractas (`Template Method`)**:
  - Crear una clase base abstracta de servicio de recordatorios y subclases como `EmailCitaReminderService` y `SmsCitaReminderService`. Se descartó porque una cita individual debe poder enviarse por un canal distinto en la misma iteración sin necesidad de instanciar un servicio de negocio diferente.

## Consecuencias

### Positivas
- **Alta testabilidad**: Para probar el servicio `CitaReminderServiceImpl`, basta con suministrar una instancia simulada (`@Mock ReminderChannel`), verificando el método `enviar(cita, mensaje)` sin importar el canal concreto subyacente.
- **Extensibilidad**: Incorporar nuevos canales (por ejemplo, `WhatsAppReminderChannel`) no requiere modificar ninguna línea de `CitaReminderServiceImpl`.
- **Adherencia a SOLID**: Se respeta el Principio de Responsabilidad Única y el Principio Abierto/Cerrado.

### Negativas
- Incremento en el número total de clases e interfaces en la arquitectura del proyecto.
