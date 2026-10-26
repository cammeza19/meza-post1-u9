package com.clinica.recordatorios.channel;

import com.clinica.recordatorios.domain.Cita;

public interface ReminderChannel {
    void enviar(Cita cita, String mensaje);
}
