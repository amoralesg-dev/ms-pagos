package com.rassini.pagos.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LoginResponse {
    private Integer id;
    private String usuario;
    private String nombre;
    private String bu;
    private String rol;
    private LocalDateTime fechaCreacion;
    private Boolean activo;
}
