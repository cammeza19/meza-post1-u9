package com.clinica.recordatorios.service;

public interface CitaReminderService {
    int enviarRecordatoriosPendientes();
    void enviarRecordatorioUrgente(Long citaId);
}
