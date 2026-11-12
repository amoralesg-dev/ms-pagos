package com.rassini.pagos.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierVersionDetailDTO {
    private Long supplierId;
    private String supplierCode;
    private String erpIdQad;
    private String supplierName;
    private String rfc;
    private String businessUnitCode;
    private Integer versionNumber;
    private LocalDateTime createdAt;
    private String versionSource;
    private List<String> changedFields;
    private SupplierBankFieldsDTO versionBank;
    private SupplierBankFieldsDTO currentBank;
    private List<SupplierFieldDiffDTO> diffs;
}
