package com.clinica.recordatorios.repository;

import com.clinica.recordatorios.domain.CanalNotificacion;
import com.clinica.recordatorios.domain.Cita;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CitaRepositoryTest {

    @Autowired
    private CitaRepository citaRepository;

    @Test
    void findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse_soloRetornaCitasPendientesEnVentana() {
        citaRepository.save(new Cita(null, "Dentro de ventana, pendiente", "a@mail.com",
                LocalDateTime.of(2026, 8, 10, 12, 0), CanalNotificacion.EMAIL, false));
        citaRepository.save(new Cita(null, "Dentro de ventana, ya enviada", "b@mail.com",
                LocalDateTime.of(2026, 8, 10, 14, 0), CanalNotificacion.EMAIL, true));
        citaRepository.save(new Cita(null, "Fuera de ventana", "c@mail.com",
                LocalDateTime.of(2026, 8, 20, 9, 0), CanalNotificacion.SMS, false));

        List<Cita> resultado = citaRepository
                .findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(
                        LocalDateTime.of(2026, 8, 10, 0, 0),
                        LocalDateTime.of(2026, 8, 11, 0, 0));

        assertEquals(1, resultado.size());
        assertEquals("Dentro de ventana, pendiente", resultado.get(0).getPacienteNombre());
    }
}
