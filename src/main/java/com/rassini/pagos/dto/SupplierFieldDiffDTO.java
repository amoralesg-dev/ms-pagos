package com.rassini.pagos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierFieldDiffDTO {
    /** Nombre de columna (snake_case), igual que en changed_fields. */
    private String field;
    /** true si el valor de la version difiere del vigente (comparacion sobre valores sin enmascarar). */
    private boolean differsFromCurrent;
    /** true si el campo esta listado en changed_fields de esta version. */
    private boolean changedInVersion;
}
