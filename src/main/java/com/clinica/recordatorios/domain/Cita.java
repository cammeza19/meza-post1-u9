package com.clinica.recordatorios.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String pacienteNombre;

    @Column(nullable = false)
    private String pacienteContacto; // email o teléfono, según canalPreferido

    @Column(nullable = false)
    private LocalDateTime fechaHoraCita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CanalNotificacion canalPreferido;

    @Column(nullable = false)
    private boolean recordatorioEnviado;
}
