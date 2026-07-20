package com.rassini.pagos.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.rassini.pagos.dto.ClasificarPagoItem;
import com.rassini.pagos.dto.PagoPendienteDTO;
import com.rassini.pagos.dto.ReferenciaManualItemDTO;
import com.rassini.pagos.dto.ValidacionEnvioDTO;

public interface PagoService {

    List<PagoPendienteDTO> obtenerSinClasificar(String bu);

    long obtenerTotalPendientes(String bu);

    void clasificarPagos(List<ClasificarPagoItem> items, String bu);

    List<PagoPendienteDTO> filtrarPendientes(String bu, String codigoProveedor, String rfcBeneficiario);

    Page<PagoPendienteDTO> filtrarPendientesPaginado(String bu, String codigoProveedor, String rfcBeneficiario, String tipoPago, String estatus, Pageable pageable);

    Page<PagoPendienteDTO> filtrarEnviadosPaginado(String bu,String search,Pageable pageable);

    Page<PagoPendienteDTO> filtrarErroresPaginado(String bu, String search, Pageable pageable);

    ValidacionEnvioDTO  validarPagosPendientes(String bu);

    int enviarPagosPendientes(String bu);

    void rechazarPago(Long id);

    void rechazarPagos(List<Long> ids);

    
    void actualizarReferenciaManual(Long id,String referenciaManual);

    void actualizarReferenciasManuales(List<ReferenciaManualItemDTO> items);

}
