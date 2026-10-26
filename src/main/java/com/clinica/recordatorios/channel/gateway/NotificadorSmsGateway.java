package com.clinica.recordatorios.channel.gateway;

public interface NotificadorSmsGateway {
    void enviarSms(String numeroDestino, String texto);
}
