package com.rassini.pagos.dto;

import java.util.List;
import lombok.Data;

@Data
public class ActualizarTipoPagoSeleccionadoDTO {

    private List<TipoPagoSeleccionadoItemDTO> items;
}

