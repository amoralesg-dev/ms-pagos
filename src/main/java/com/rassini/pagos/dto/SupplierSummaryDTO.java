package com.rassini.pagos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierSummaryDTO {
    private Long id;
    private String supplierCode;
    private String erpIdQad;
    private String supplierName;
    private String rfc;
    private String businessUnitCode;
    /** Enmascarado: solo ultimos 4 caracteres visibles. */
    private String accountNumber;
    private String bankName;
    private long versionCount;
}
