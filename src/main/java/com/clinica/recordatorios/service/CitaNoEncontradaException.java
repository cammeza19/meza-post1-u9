package com.clinica.recordatorios.service;

public class CitaNoEncontradaException extends RuntimeException {
    public CitaNoEncontradaException(Long citaId) {
        super("Cita no encontrada: " + citaId);
    }
}
