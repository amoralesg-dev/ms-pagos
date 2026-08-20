package com.rassini.pagos.service;

import java.util.Map;

import com.rassini.pagos.entity.CatalogoTipoPago;
import com.rassini.pagos.entity.EquivalencesDealType;
import com.rassini.pagos.entity.PagoReferenciaProveedor;

public interface EmpresaTipoPagoCache {

    boolean esValido(String empresa, String dealType);

    void recargar();

    Map<String, EquivalencesDealType> getEquivalencesDealTypeMap();

    Map<String, CatalogoTipoPago> getCatalogoTipoPagoMap();

    Map<String, PagoReferenciaProveedor> getReferenciaProveedorMap();
}