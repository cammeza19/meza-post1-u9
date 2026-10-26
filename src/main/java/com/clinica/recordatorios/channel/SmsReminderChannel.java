package com.clinica.recordatorios.channel;

import com.clinica.recordatorios.channel.gateway.NotificadorSmsGateway;
import com.clinica.recordatorios.domain.Cita;
import org.springframework.stereotype.Component;

@Component
public class SmsReminderChannel implements ReminderChannel {

    private final NotificadorSmsGateway gateway;

    public SmsReminderChannel(NotificadorSmsGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public void enviar(Cita cita, String mensaje) {
        gateway.enviarSms(cita.getPacienteContacto(), mensaje);
    }
}
