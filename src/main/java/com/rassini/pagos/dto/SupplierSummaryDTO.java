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
    private String accountNumber;
    private String bankCurrency;
    private String bankName;
    private long versionCount;
}
