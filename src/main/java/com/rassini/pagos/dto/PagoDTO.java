package com.rassini.pagos.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PagoDTO {

    private Long id;

    private String empresa;
    private String referencia;
    private String fechaEnvio;
    private String fechaValor;
    private String fecha3;
    private String contador;
    private String codigoProveedor;
    private String rfcBeneficiario;
    private String informacionAdicional;
    private String monto;
    private String moneda;
    private String tipoCambio;
    private String nombreBeneficiario;
    private String cuentaBeneficiario;
    private String cuentaBancaria;
    private String campoVacio1;
    private String campoVacio2;
    private String campoVacio3;
    private String campoVacio4;
    private String monedaBeneficiario;
    private String monedaOrdenante;
    private String cuentaOrdenante;
    private String campoVacio5;
    private String campoVacio6;
    private String nombreArchivo;

    private String dealType; // clasificación
}