package com.rassini.pagos.service;

import com.rassini.pagos.dto.ClasificarPagoItem;
import com.rassini.pagos.dto.PagoPendienteDTO;

import java.util.List;

public interface PagoService {

    List<PagoPendienteDTO> obtenerSinClasificar();

    void clasificarPagos(List<ClasificarPagoItem> items);

    List<PagoPendienteDTO> filtrarPendientes(String codigoProveedor, String rfcBeneficiario);
}