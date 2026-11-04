package com.rassini.pagos.dto;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Historial: el registro vigente va en {@code current} (separado, no forma parte de la paginacion)
 * y las versiones historicas en {@code versions}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierVersionHistoryDTO {
    private SupplierVersionItemDTO current;
    private Page<SupplierVersionItemDTO> versions;
}
