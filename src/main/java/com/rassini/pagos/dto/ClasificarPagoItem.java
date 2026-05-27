package com.rassini.pagos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClasificarPagoItem {

    private Long id;
    private String dealType;
    private String decisionDuplicado;
}