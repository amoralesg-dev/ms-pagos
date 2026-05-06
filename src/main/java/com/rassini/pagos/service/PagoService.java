package com.rassini.pagos.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.rassini.pagos.dto.ClasificarPagoItem;
import com.rassini.pagos.dto.PagoPendienteDTO;

public interface PagoService {

    List<PagoPendienteDTO> obtenerSinClasificar(String bu);

    long obtenerTotalPendientes(String bu);

    void clasificarPagos(List<ClasificarPagoItem> items, String bu);

    List<PagoPendienteDTO> filtrarPendientes(String bu, String codigoProveedor, String rfcBeneficiario);

    Page<PagoPendienteDTO> filtrarPendientesPaginado(String bu, String codigoProveedor, String rfcBeneficiario, Pageable pageable);

    int validarPagosPendientes(String bu);

    int enviarPagosPendientes(String bu);
}
