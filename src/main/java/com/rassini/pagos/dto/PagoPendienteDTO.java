package com.rassini.pagos.dto;


import lombok.Data;

@Data
public class PagoPendienteDTO {

    private Long id;
    private String empresa;
    private String referencia;
    private String fechaEnvio;
    private String fechaValor;
    private String codigoProveedor;
    private String rfcBeneficiario;
    private String informacionAdicional;
    private String monto;
    private String moneda;
    private String tipoCambio;
    private String nombreBeneficiario;
    private String cuentaBeneficiario;
}
