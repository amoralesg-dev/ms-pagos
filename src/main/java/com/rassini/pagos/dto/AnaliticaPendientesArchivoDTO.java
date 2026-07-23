package com.rassini.pagos.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnaliticaPendientesArchivoDTO {

    private String nombreArchivo;
    private Long cantidadPagos;
    private BigDecimal montoTotal;

    private List<AnaliticaPendientesEmpresaDTO> empresas;

}
