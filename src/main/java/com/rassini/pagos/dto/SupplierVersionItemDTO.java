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
public class SupplierVersionItemDTO {
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    /** null para el registro vigente (current=true). */
    private Integer versionNumber;
    private LocalDateTime createdAt;
    private boolean current;
    private List<String> changedFields;
    private String versionSource;
}
