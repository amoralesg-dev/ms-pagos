package com.rassini.pagos.service;

import com.rassini.pagos.entity.Supplier;

public interface FileLoaderService {

    void cargarArchivos();
    Supplier obtenerSupplierPadre(String codigoProveedor, String empresa);
    Supplier obtenerSupplierPadrePorCuenta(String codigoProveedor,String empresa,String ultimos8);
    String obtenerUltimos8DigitosCuenta(String cuentaBeneficiario);
}