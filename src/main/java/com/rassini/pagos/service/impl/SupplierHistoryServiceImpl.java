package com.rassini.pagos.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rassini.pagos.dto.SupplierBankFieldsDTO;
import com.rassini.pagos.dto.SupplierFieldDiffDTO;
import com.rassini.pagos.dto.SupplierSummaryDTO;
import com.rassini.pagos.dto.SupplierVersionDetailDTO;
import com.rassini.pagos.dto.SupplierVersionHistoryDTO;
import com.rassini.pagos.dto.SupplierVersionItemDTO;
import com.rassini.pagos.entity.Supplier;
import com.rassini.pagos.entity.SupplierVersion;
import com.rassini.pagos.exception.ResourceNotFoundException;
import com.rassini.pagos.repository.SupplierRepository;
import com.rassini.pagos.repository.SupplierVersionRepository;
import com.rassini.pagos.service.SupplierHistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierHistoryServiceImpl implements SupplierHistoryService {

    /** Campos bancarios comparados, en orden: columna -> [getter version, getter vigente]. */
    private static final List<String> BANK_FIELDS = List.of(
            "beneficiary_bank_name", "beneficiary_account_name", "account_number", "bank_currency",
            "bank_country", "routing_code_aba", "routing_code_swift", "intermediary_bank_name",
            "intermediary_account", "intermediary_account_country", "intermediary_routing_code_aba",
            "intermediary_routing_code_swift");

    private final SupplierRepository supplierRepository;
    private final SupplierVersionRepository versionRepository;

    @Override
    public Page<SupplierSummaryDTO> buscarProveedores(String erpIdQad, String supplierCode, String supplierName,
            String taxId, Pageable pageable) {
        Page<Supplier> page = supplierRepository.buscarParaHistorial(blankToNull(erpIdQad), blankToNull(supplierCode),
                blankToNull(supplierName), blankToNull(taxId), pageable);

        Map<Long, Long> counts = new HashMap<>();
        List<Long> ids = page.getContent().stream().map(Supplier::getId).toList();
        if (!ids.isEmpty()) {
            for (Object[] row : versionRepository.countBySupplierIds(ids)) {
                counts.put((Long) row[0], ((Number) row[1]).longValue());
            }
        }
        return page.map(s -> SupplierSummaryDTO.builder()
                .id(s.getId())
                .supplierCode(s.getSupplierCode())
                .erpIdQad(s.getErpIdQad())
                .supplierName(s.getSupplierName())
                .rfc(s.getRfc())
                .businessUnitCode(s.getBusinessUnitCode())
                .accountNumber(s.getAccountNumber())
                .bankCurrency(s.getBankCurrency())
                .bankName(s.getBeneficiaryBankName())
                .versionCount(counts.getOrDefault(s.getId(), 0L))
                .build());
    }

    @Override
    public SupplierVersionHistoryDTO obtenerVersiones(Long supplierId, Pageable pageable) {
        Supplier supplier = findSupplier(supplierId);
        Page<SupplierVersionItemDTO> versions = versionRepository.findBySupplierId(supplierId, pageable)
                .map(this::toItem);
        return SupplierVersionHistoryDTO.builder()
                .current(toCurrentItem(supplier))
                .versions(versions)
                .build();
    }

    @Override
    public SupplierVersionDetailDTO obtenerDetalleVersion(Long supplierId, Integer versionNumber) {
        Supplier supplier = findSupplier(supplierId);
        SupplierVersion v = versionRepository.findBySupplierIdAndVersionNumber(supplierId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " no encontrada para el proveedor " + supplierId));

        List<String> changed = parseChangedFields(v.getChangedFields());
        Map<String, Function<SupplierVersion, String>> vGet = versionGetters();
        Map<String, Function<Supplier, String>> sGet = supplierGetters();
        List<SupplierFieldDiffDTO> diffs = new ArrayList<>();
        for (String f : BANK_FIELDS) {
            diffs.add(SupplierFieldDiffDTO.builder()
                    .field(f)
                    .differsFromCurrent(!Objects.equals(norm(vGet.get(f).apply(v)), norm(sGet.get(f).apply(supplier))))
                    .changedInVersion(changed.contains(f))
                    .build());
        }

        return SupplierVersionDetailDTO.builder()
                .supplierId(supplier.getId())
                .supplierCode(v.getSupplierCode())
                .erpIdQad(v.getErpIdQad())
                .supplierName(v.getSupplierName())
                .rfc(v.getRfc())
                .businessUnitCode(v.getBusinessUnitCode())
                .versionNumber(v.getVersionNumber())
                .createdAt(v.getCreatedAt())
                .versionSource(v.getVersionSource())
                .changedFields(changed)
                .versionBank(bankOf(v))
                .currentBank(bankOf(supplier))
                .diffs(diffs)
                .build();
    }

    @Override
    public SupplierVersionItemDTO obtenerVigente(Long supplierId) {
        return toCurrentItem(findSupplier(supplierId));
    }

    // ---------------------------------------------------------------------

    private Supplier findSupplier(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor " + supplierId + " no encontrado"));
    }

    private SupplierVersionItemDTO toItem(SupplierVersion v) {
        return SupplierVersionItemDTO.builder()
                .supplierId(v.getSupplierId())
                .supplierCode(v.getSupplierCode())
                .supplierName(v.getSupplierName())
                .versionNumber(v.getVersionNumber())
                .createdAt(v.getCreatedAt())
                .current(false)
                .changedFields(parseChangedFields(v.getChangedFields()))
                .versionSource(v.getVersionSource())
                .build();
    }

    private SupplierVersionItemDTO toCurrentItem(Supplier s) {
        return SupplierVersionItemDTO.builder()
                .supplierId(s.getId())
                .supplierCode(s.getSupplierCode())
                .supplierName(s.getSupplierName())
                .versionNumber(null)
                .current(true)
                .changedFields(List.of())
                .build();
    }

    static List<String> parseChangedFields(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .distinct()
                .toList();
    }

    private static SupplierBankFieldsDTO bankOf(SupplierVersion v) {
        return SupplierBankFieldsDTO.builder()
                .beneficiaryBankName(v.getBeneficiaryBankName())
                .beneficiaryAccountName(v.getBeneficiaryAccountName())
                .accountNumber(v.getAccountNumber())
                .bankCurrency(v.getBankCurrency())
                .bankCountry(v.getBankCountry())
                .routingCodeAba(v.getRoutingCodeAba())
                .routingCodeSwift(v.getRoutingCodeSwift())
                .intermediaryBankName(v.getIntermediaryBankName())
                .intermediaryAccount(v.getIntermediaryAccount())
                .intermediaryAccountCountry(v.getIntermediaryAccountCountry())
                .intermediaryRoutingCodeAba(v.getIntermediaryRoutingCodeAba())
                .intermediaryRoutingCodeSwift(v.getIntermediaryRoutingCodeSwift())
                .build();
    }

    private static SupplierBankFieldsDTO bankOf(Supplier s) {
        return SupplierBankFieldsDTO.builder()
                .beneficiaryBankName(s.getBeneficiaryBankName())
                .beneficiaryAccountName(s.getBeneficiaryAccountName())
                .accountNumber(s.getAccountNumber())
                .bankCurrency(s.getBankCurrency())
                .bankCountry(s.getBankCountry())
                .routingCodeAba(s.getRoutingCodeAba())
                .routingCodeSwift(s.getRoutingCodeSwift())
                .intermediaryBankName(s.getIntermediaryBankName())
                .intermediaryAccount(s.getIntermediaryAccount())
                .intermediaryAccountCountry(s.getIntermediaryAccountCountry())
                .intermediaryRoutingCodeAba(s.getIntermediaryRoutingCodeAba())
                .intermediaryRoutingCodeSwift(s.getIntermediaryRoutingCodeSwift())
                .build();
    }

    private static Map<String, Function<SupplierVersion, String>> versionGetters() {
        Map<String, Function<SupplierVersion, String>> m = new HashMap<>();
        m.put("beneficiary_bank_name", SupplierVersion::getBeneficiaryBankName);
        m.put("beneficiary_account_name", SupplierVersion::getBeneficiaryAccountName);
        m.put("account_number", SupplierVersion::getAccountNumber);
        m.put("bank_currency", SupplierVersion::getBankCurrency);
        m.put("bank_country", SupplierVersion::getBankCountry);
        m.put("routing_code_aba", SupplierVersion::getRoutingCodeAba);
        m.put("routing_code_swift", SupplierVersion::getRoutingCodeSwift);
        m.put("intermediary_bank_name", SupplierVersion::getIntermediaryBankName);
        m.put("intermediary_account", SupplierVersion::getIntermediaryAccount);
        m.put("intermediary_account_country", SupplierVersion::getIntermediaryAccountCountry);
        m.put("intermediary_routing_code_aba", SupplierVersion::getIntermediaryRoutingCodeAba);
        m.put("intermediary_routing_code_swift", SupplierVersion::getIntermediaryRoutingCodeSwift);
        return m;
    }

    private static Map<String, Function<Supplier, String>> supplierGetters() {
        Map<String, Function<Supplier, String>> m = new HashMap<>();
        m.put("beneficiary_bank_name", Supplier::getBeneficiaryBankName);
        m.put("beneficiary_account_name", Supplier::getBeneficiaryAccountName);
        m.put("account_number", Supplier::getAccountNumber);
        m.put("bank_currency", Supplier::getBankCurrency);
        m.put("bank_country", Supplier::getBankCountry);
        m.put("routing_code_aba", Supplier::getRoutingCodeAba);
        m.put("routing_code_swift", Supplier::getRoutingCodeSwift);
        m.put("intermediary_bank_name", Supplier::getIntermediaryBankName);
        m.put("intermediary_account", Supplier::getIntermediaryAccount);
        m.put("intermediary_account_country", Supplier::getIntermediaryAccountCountry);
        m.put("intermediary_routing_code_aba", Supplier::getIntermediaryRoutingCodeAba);
        m.put("intermediary_routing_code_swift", Supplier::getIntermediaryRoutingCodeSwift);
        return m;
    }

    private static String norm(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
