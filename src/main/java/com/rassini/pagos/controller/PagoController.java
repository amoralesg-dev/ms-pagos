package com.rassini.pagos.controller;

import com.rassini.pagos.service.PagoService;
import com.rassini.pagos.service.EmpresaTipoPagoCache;
import org.springframework.web.bind.annotation.*;
import com.rassini.pagos.dto.ClasificarPagosRequest;
import com.rassini.pagos.dto.PagoPendienteDTO;

import java.util.List;

@RestController
@RequestMapping("/pagos")
public class PagoController {

    private final PagoService service;
    private final EmpresaTipoPagoCache cache;

    public PagoController(PagoService service,
                          EmpresaTipoPagoCache cache) { //
        this.service = service;
        this.cache = cache;
    }

    //  obtener pagos sin clasificar
    @GetMapping("/pendientes")
    public List<PagoPendienteDTO> pendientes() {
        return service.obtenerSinClasificar();
    }

    @GetMapping("/pendientes/filtro")
    public List<PagoPendienteDTO> filtrar(
            @RequestParam(required = false) String codigoProveedor,
            @RequestParam(required = false) String rfcBeneficiario) {

        return service.filtrarPendientes(codigoProveedor, rfcBeneficiario);
    }

    //  clasificar pago
    @PutMapping("/clasificacion")
    public String clasificar(@RequestBody ClasificarPagosRequest request) {

        service.clasificarPagos(request.getItems());

        return "Pagos actualizados correctamente";
    }
    @PostMapping("/cache/refresh")
    public String refreshCache() {
        cache.recargar();
        return "Cache actualizado";
    }
}