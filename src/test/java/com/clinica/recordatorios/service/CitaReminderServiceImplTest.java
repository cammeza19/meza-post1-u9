package com.clinica.recordatorios.service;

import com.clinica.recordatorios.channel.ReminderChannel;
import com.clinica.recordatorios.channel.ReminderChannelFactory;
import com.clinica.recordatorios.domain.CanalNotificacion;
import com.clinica.recordatorios.domain.Cita;
import com.clinica.recordatorios.repository.CitaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaReminderServiceImplTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ReminderChannelFactory reminderChannelFactory;

    @Mock
    private ReminderChannel reminderChannelMock;

    @Mock
    private ReminderChannel canalUrgenteMock;

    // Fake — deliberadamente NO se mockea con Mockito. Clock.fixed(...) es
    // una implementación real y completa de java.time.Clock: se comporta
    // exactamente igual que en producción (LocalDateTime.now(clock) sigue
    // funcionando de verdad), solo que con un instante congelado.
    private final Clock clockFijo =
            Clock.fixed(Instant.parse("2026-08-10T09:00:00Z"), ZoneId.of("UTC"));

    private CitaReminderServiceImpl citaReminderService;

    @BeforeEach
    void setUp() {
        citaReminderService = new CitaReminderServiceImpl(
                citaRepository, reminderChannelFactory, canalUrgenteMock, clockFijo);
    }

    @Test
    void enviarRecordatoriosPendientes_citaEnVentana_usaElCanalPreferido() {
        Cita cita = new Cita(1L, "Ana Gómez", "ana@correo.com",
                LocalDateTime.of(2026, 8, 10, 15, 0), CanalNotificacion.EMAIL, false);

        when(citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(any(), any()))
                .thenReturn(List.of(cita));
        when(reminderChannelFactory.obtenerCanal(CanalNotificacion.EMAIL))
                .thenReturn(reminderChannelMock);

        int enviados = citaReminderService.enviarRecordatoriosPendientes();

        assertEquals(1, enviados);
        verify(reminderChannelMock, times(1)).enviar(eq(cita), anyString());
        verify(citaRepository, times(1)).save(cita);
        assertTrue(cita.isRecordatorioEnviado());
    }

    @Test
    void enviarRecordatoriosPendientes_ventanaDeterministaSegunClockFijo_consultaLosLimitesExactos() {
        when(citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(any(), any()))
                .thenReturn(List.of());

        citaReminderService.enviarRecordatoriosPendientes();

        ArgumentCaptor<LocalDateTime> desdeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hastaCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(citaRepository).findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(
                desdeCaptor.capture(), hastaCaptor.capture());

        // Con Clock.systemDefaultZone() esta aserción sería no determinista
        // (cambiaría en cada ejecución); con el Fake, el resultado es exacto
        // y reproducible en cualquier máquina y a cualquier hora del día.
        assertEquals(LocalDateTime.of(2026, 8, 10, 9, 0), desdeCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 8, 11, 9, 0), hastaCaptor.getValue());
    }

    @Test
    void enviarRecordatoriosPendientes_sinCitasPendientes_noConsultaLaFactory() {
        when(citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(any(), any()))
                .thenReturn(List.of());

        int enviados = citaReminderService.enviarRecordatoriosPendientes();

        assertEquals(0, enviados);
        verifyNoInteractions(reminderChannelFactory);
        verify(citaRepository, never()).save(any());
    }

    @Test
    void enviarRecordatorioUrgente_citaExistente_usaCanalUrgenteInyectado() {
        Cita cita = new Cita(5L, "Luis Peña", "3001234567",
                LocalDateTime.of(2026, 8, 10, 10, 30), CanalNotificacion.SMS, false);
        when(citaRepository.findById(5L)).thenReturn(Optional.of(cita));

        citaReminderService.enviarRecordatorioUrgente(5L);

        verify(canalUrgenteMock, times(1)).enviar(eq(cita), contains("URGENTE"));
        verify(citaRepository).save(cita);
        assertTrue(cita.isRecordatorioEnviado());
    }

    @Test
    void enviarRecordatorioUrgente_gatewayFalla_noMarcaComoEnviado() {
        // Esta prueba era IMPOSIBLE de escribir antes del refactor del Paso 8:
        // no existía ningún punto donde sustituir el canal por un mock que
        // simule el fallo del gateway de SMS.
        Cita cita = new Cita(6L, "Marta Ruiz", "3009876543",
                LocalDateTime.of(2026, 8, 10, 10, 30), CanalNotificacion.SMS, false);
        when(citaRepository.findById(6L)).thenReturn(Optional.of(cita));
        doThrow(new RuntimeException("Gateway de SMS no disponible"))
                .when(canalUrgenteMock).enviar(any(), anyString());

        assertThrows(RuntimeException.class,
                () -> citaReminderService.enviarRecordatorioUrgente(6L));

        verify(citaRepository, never()).save(any());
    }

    @Test
    void enviarRecordatorioUrgente_citaInexistente_lanzaCitaNoEncontradaException() {
        when(citaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CitaNoEncontradaException.class,
                () -> citaReminderService.enviarRecordatorioUrgente(99L));

        verifyNoInteractions(canalUrgenteMock);
    }
}
