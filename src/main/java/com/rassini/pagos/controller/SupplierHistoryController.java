package com.rassini.pagos.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rassini.pagos.dto.SupplierSummaryDTO;
import com.rassini.pagos.dto.SupplierVersionDetailDTO;
import com.rassini.pagos.dto.SupplierVersionHistoryDTO;
import com.rassini.pagos.dto.SupplierVersionItemDTO;
import com.rassini.pagos.exception.BusinessException;
import com.rassini.pagos.service.SupplierHistoryService;

import lombok.RequiredArgsConstructor;

/**
 * Consulta (solo lectura) del historial de versiones bancarias de proveedores para Portal CxP.
 */
@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierHistoryController {

    static final int MAX_SIZE = 100;

    private static final Map<String, String> SUPPLIER_SORT_FIELDS = Map.of(
            "id", "id",
            "supplierCode", "supplierCode",
            "erpIdQad", "erpIdQad",
            "supplierName", "supplierName",
            "rfc", "rfc",
            "businessUnitCode", "businessUnitCode");

    private static final Map<String, String> VERSION_SORT_FIELDS = Map.of(
            "versionNumber", "versionNumber",
            "createdAt", "createdAt");

    private final SupplierHistoryService service;

    @GetMapping
    public ResponseEntity<Page<SupplierSummaryDTO>> buscarProveedores(
            @RequestParam(required = false) String erpIdQad,
            @RequestParam(required = false) String supplierCode,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String taxId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "supplierName") String sortField,
            @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = buildPageable(page, size, sortField, sortOrder, SUPPLIER_SORT_FIELDS);
        return ResponseEntity.ok(service.buscarProveedores(erpIdQad, supplierCode, supplierName, taxId, pageable));
    }

    @GetMapping("/{supplierId}/versions")
    public ResponseEntity<SupplierVersionHistoryDTO> obtenerVersiones(
            @PathVariable Long supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "versionNumber") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        Pageable pageable = buildPageable(page, size, sortField, sortOrder, VERSION_SORT_FIELDS);
        return ResponseEntity.ok(service.obtenerVersiones(supplierId, pageable));
    }

    @GetMapping("/{supplierId}/versions/{versionNumber}")
    public ResponseEntity<SupplierVersionDetailDTO> obtenerDetalleVersion(
            @PathVariable Long supplierId,
            @PathVariable Integer versionNumber) {
        return ResponseEntity.ok(service.obtenerDetalleVersion(supplierId, versionNumber));
    }

    @GetMapping("/{supplierId}/current")
    public ResponseEntity<SupplierVersionItemDTO> obtenerVigente(@PathVariable Long supplierId) {
        return ResponseEntity.ok(service.obtenerVigente(supplierId));
    }

    private static Pageable buildPageable(int page, int size, String sortField, String sortOrder,
            Map<String, String> allowed) {
        if (page < 0) {
            throw new BusinessException("page debe ser >= 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException("size debe estar entre 1 y " + MAX_SIZE);
        }
        String mapped = allowed.get(sortField);
        if (mapped == null) {
            throw new BusinessException("sortField no permitido: " + sortField);
        }
        if (!"ASC".equalsIgnoreCase(sortOrder) && !"DESC".equalsIgnoreCase(sortOrder)) {
            throw new BusinessException("sortOrder debe ser ASC o DESC");
        }
        Sort.Direction dir = "DESC".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(dir, mapped).and(Sort.by(Sort.Direction.ASC, "id")));
    }
}
