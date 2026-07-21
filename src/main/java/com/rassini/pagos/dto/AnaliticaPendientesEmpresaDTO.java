package com.rassini.pagos.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnaliticaPendientesEmpresaDTO {

    private String empresa;
    private Long cantidadPagos;
    private BigDecimal montoTotal;

    private List<AnaliticaPendientesTipoPagoDTO> tiposPago;

}
