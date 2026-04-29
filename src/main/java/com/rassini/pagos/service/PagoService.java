package com.rassini.pagos.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.rassini.pagos.dto.ClasificarPagoItem;
import com.rassini.pagos.dto.PagoPendienteDTO;

public interface PagoService {

    List<PagoPendienteDTO> obtenerSinClasificar();

    long obtenerTotalPendientes();

    void clasificarPagos(List<ClasificarPagoItem> items);

    List<PagoPendienteDTO> filtrarPendientes(String codigoProveedor, String rfcBeneficiario);

    Page<PagoPendienteDTO> filtrarPendientesPaginado(String codigoProveedor, String rfcBeneficiario, Pageable pageable);

    int validarPagosPendientes();
}