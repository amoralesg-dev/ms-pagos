package com.rassini.pagos.service;

public interface EmpresaTipoPagoCache {

    boolean esValido(String empresa, String dealType);

    void recargar();
}