package com.clinica.recordatorios.channel;

import com.clinica.recordatorios.domain.CanalNotificacion;

public interface ReminderChannelFactory {
    ReminderChannel obtenerCanal(CanalNotificacion tipo);
}
