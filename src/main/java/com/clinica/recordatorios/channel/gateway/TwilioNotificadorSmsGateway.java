package com.clinica.recordatorios.channel.gateway;

import org.springframework.stereotype.Component;

@Component
public class TwilioNotificadorSmsGateway implements NotificadorSmsGateway {

    @Override
    public void enviarSms(String numeroDestino, String texto) {
        // En producción: llamada HTTP real a la API de un proveedor de SMS.
        // Se simula aquí porque el laboratorio no depende de credenciales
        // externas ni de red.
        System.out.printf("[SMS a %s] %s%n", numeroDestino, texto);
    }
}
