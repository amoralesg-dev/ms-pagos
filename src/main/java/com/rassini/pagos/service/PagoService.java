package com.rassini.pagos.service;

import com.rassini.pagos.dto.ClasificarPagoItem;
import com.rassini.pagos.dto.PagoPendienteDTO;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PagoService {

    List<PagoPendienteDTO> obtenerSinClasificar();

    void clasificarPagos(List<ClasificarPagoItem> items);

    List<PagoPendienteDTO> filtrarPendientes(String codigoProveedor, String rfcBeneficiario);

    Page<PagoPendienteDTO> filtrarPendientesPaginado(String codigoProveedor, String rfcBeneficiario, Pageable pageable);
}