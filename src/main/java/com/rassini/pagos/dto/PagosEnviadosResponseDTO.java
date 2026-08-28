package com.rassini.pagos.dto;

import java.util.List;
import org.springframework.data.domain.Page;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagosEnviadosResponseDTO {
    private Page<PagoPendienteDTO> page;
    private long totalPagos;
    private List<SumaPorMonedaDTO> sumas;
}
