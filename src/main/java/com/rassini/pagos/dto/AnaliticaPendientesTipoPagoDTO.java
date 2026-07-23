package com.rassini.pagos.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnaliticaPendientesTipoPagoDTO {

    private String tipoPago;
    private Long cantidadPagos;
    private BigDecimal montoTotal;

    private List<AnaliticaPendientesMonedaDTO> monedas;

}
