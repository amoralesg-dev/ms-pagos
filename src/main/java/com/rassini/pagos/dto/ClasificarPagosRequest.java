package com.rassini.pagos.dto;

import lombok.Data;
import java.util.List;

@Data
public class ClasificarPagosRequest {

    private List<ClasificarPagoItem> items;
}