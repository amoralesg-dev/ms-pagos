package com.rassini.pagos.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rassini.pagos.dto.ClasificarPagosRequest;
import com.rassini.pagos.dto.PagoPendienteDTO;
import com.rassini.pagos.service.EmpresaTipoPagoCache;
import com.rassini.pagos.service.PagoService;

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

    @GetMapping("/pendientes/total")
    public long obtenerTotalPendientes() {
        return service.obtenerTotalPendientes();
    }

    @GetMapping("/pendientes/filtro")
    public List<PagoPendienteDTO> filtrar(
            @RequestParam(required = false) String codigoProveedor,
            @RequestParam(required = false) String rfcBeneficiario) {

        return service.filtrarPendientes(codigoProveedor, rfcBeneficiario);
    }

    @GetMapping("/pendientes/filtro/paginado")
    public Page<PagoPendienteDTO> filtrarPaginado(
            @RequestParam(required = false) String codigoProveedor,
            @RequestParam(required = false) String rfcBeneficiario,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return service.filtrarPendientesPaginado(codigoProveedor, rfcBeneficiario, pageable);
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

    @PostMapping("/validar")
    public int validarPagos() {
        return service.validarPagosPendientes();                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
    }
}