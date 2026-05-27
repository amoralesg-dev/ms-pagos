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
    public List<PagoPendienteDTO> pendientes(@RequestParam String bu) {
        return service.obtenerSinClasificar(bu);
    }

    @GetMapping("/pendientes/total")
    public long obtenerTotalPendientes(@RequestParam String bu) {
        return service.obtenerTotalPendientes(bu);
    }

    @GetMapping("/pendientes/filtro")
    public List<PagoPendienteDTO> filtrar(
            @RequestParam String bu,
            @RequestParam(required = false) String codigoProveedor,
            @RequestParam(required = false) String rfcBeneficiario) {

        return service.filtrarPendientes(bu, codigoProveedor, rfcBeneficiario);
    }

    @GetMapping("/pendientes/filtro/paginado")
    public Page<PagoPendienteDTO> filtrarPaginado(
            @RequestParam String bu,
            @RequestParam(required = false) String codigoProveedor,
            @RequestParam(required = false) String rfcBeneficiario,
            @RequestParam(required = false) String tipoPago,
            @RequestParam(required = false) String estatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return service.filtrarPendientesPaginado(bu, codigoProveedor, rfcBeneficiario, tipoPago, estatus, pageable);
    }

    @GetMapping("/enviados/filtro/paginado")
    public Page<PagoPendienteDTO> filtrarEnviadosPaginado(
            @RequestParam String bu,
            @RequestParam(required = false) String codigoProveedor,
            @RequestParam(required = false) String rfcBeneficiario,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return service.filtrarEnviadosPaginado(bu, codigoProveedor, rfcBeneficiario, pageable);
    }

    //  clasificar pago
    @PutMapping("/clasificacion")
    public String clasificar(@RequestParam String bu, @RequestBody ClasificarPagosRequest request) {

        service.clasificarPagos(request.getItems(), bu);

        return "Pagos actualizados correctamente";
    }
    @PostMapping("/cache/refresh")
    public String refreshCache() {
        cache.recargar();
        return "Cache actualizado";
    }

    @PostMapping("/validar")
    public int validarPagos(@RequestParam String bu) {
        return service.validarPagosPendientes(bu);                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
    }

    @PostMapping("/enviar")
    public int enviarPagos(@RequestParam String bu) {
        return service.enviarPagosPendientes(bu);                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
    }
}
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             