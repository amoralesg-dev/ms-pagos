package com.rassini.pagos.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnaliticaPendientesMonedaDTO {

    private String moneda;
    private Long cantidadPagos;
    private BigDecimal montoTotal;

}
