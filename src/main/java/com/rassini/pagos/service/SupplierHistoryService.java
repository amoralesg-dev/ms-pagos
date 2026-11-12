package com.rassini.pagos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.rassini.pagos.dto.SupplierSummaryDTO;
import com.rassini.pagos.dto.SupplierVersionDetailDTO;
import com.rassini.pagos.dto.SupplierVersionHistoryDTO;
import com.rassini.pagos.dto.SupplierVersionItemDTO;

public interface SupplierHistoryService {

    Page<SupplierSummaryDTO> buscarProveedores(String erpIdQad, String supplierCode, String supplierName,
            String taxId, Pageable pageable);

    SupplierVersionHistoryDTO obtenerVersiones(Long supplierId, Pageable pageable);

    SupplierVersionDetailDTO obtenerDetalleVersion(Long supplierId, Integer versionNumber);

    SupplierVersionItemDTO obtenerVigente(Long supplierId);
}
