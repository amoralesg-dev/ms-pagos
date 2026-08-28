package com.rassini.pagos.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rassini.pagos.dto.ActualizarReferenciasManualDTO;
import com.rassini.pagos.dto.AnaliticaPendientesArchivoDTO;
import com.rassini.pagos.dto.ClasificarPagosRequest;
import com.rassini.pagos.dto.PagoPendienteDTO;
import com.rassini.pagos.dto.ReferenciaManualDTO;
import com.rassini.pagos.dto.ValidacionEnvioDTO;
import com.rassini.pagos.service.EmpresaTipoPagoCache;
import com.rassini.pagos.service.PagoService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.JpaSort;

@RestController
@RequestMapping("/pagos")
public class PagoController {

    private final PagoService service;
    private final EmpresaTipoPagoCache cache;
    private static final Logger log = LoggerFactory.getLogger(PagoController.class);

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.ofEntries(
        Map.entry("id", "id"),
        Map.entry("empresa", "empresa"),
        Map.entry("codigoProveedor", "codigoProveedor"),
        Map.entry("rfcBeneficiario", "rfcBeneficiario"),
        Map.entry("nombreBeneficiario", "nombreBeneficiario"),
        Map.entry("monto", "monto"),
        Map.entry("moneda", "moneda"),
        Map.entry("referencia", "referencia"),
        Map.entry("nombreArchivo", "nombreArchivo"),
        Map.entry("nombreArchivoEnvio", "nombreArchivoEnvio"),
        Map.entry("tipoPago", "tipoPago.dealType"),
        Map.entry("estatus", "estatus"),
        Map.entry("fechaEnvio", "fechaEnvio")
    );

    private Pageable createSafePageable(int page, int size, String sortField, String sortOrder) {
        if (sortField == null || sortField.isBlank()) {
            return PageRequest.of(page, size);
        }

        String mappedField = ALLOWED_SORT_FIELDS.get(sortField);
        if (mappedField == null) {
            log.warn("Invalid sortField requested: {}", sortField);
            return PageRequest.of(page, size);
        }

        Sort.Direction direction = "DESC".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;

        if ("monto".equals(mappedField)) {
            return PageRequest.of(page, size, JpaSort.unsafe(direction, "CAST(monto AS big_decimal)"));
        }

        return PageRequest.of(page, size, Sort.by(direction, mappedField));
    }

    public PagoController(PagoService service,
                          EmpresaTipoPagoCache cache) {
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
            @RequestParam(required = false) String moneda,
            @RequestParam(required = false) String monto,
            @RequestParam(required = false) String proveedor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {

        Pageable pageable = createSafePageable(page, size, sortField, sortOrder);

        return service.filtrarPendientesPaginado(
                bu, codigoProveedor, rfcBeneficiario, tipoPago, estatus, moneda, monto, proveedor, pageable);
    }

    @GetMapping("/enviados/filtro/paginado")
    public com.rassini.pagos.dto.PagosEnviadosResponseDTO filtrarEnviadosPaginado(
            @RequestParam String bu,
            @RequestParam(required = false) String codigoProveedor,
            @RequestParam(required = false) String rfcBeneficiario,
            @RequestParam(required = false) String tipoPago,
            @RequestParam(required = false) String moneda,
            @RequestParam(required = false) String monto,
            @RequestParam(required = false) String proveedor,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {

        Pageable pageable = createSafePageable(page, size, sortField, sortOrder);

        return service.filtrarEnviadosPaginado(
                bu,
                codigoProveedor,
                rfcBeneficiario,
                tipoPago,
                moneda,
                monto,
                proveedor,
                fechaInicio,
                fechaFin,
                pageable
        );

    }

    @GetMapping("/errores/filtro/paginado")
    public Page<PagoPendienteDTO> filtrarErroresPaginado(
            @RequestParam String bu,
            @RequestParam(required = false) String codigoProveedor,
            @RequestParam(required = false) String rfcBeneficiario,
            @RequestParam(required = false) String tipoPago,
            @RequestParam(required = false) String moneda,
            @RequestParam(required = false) String monto,
            @RequestParam(required = false) String proveedor,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {

            Pageable pageable = createSafePageable(page, size, sortField, sortOrder);

            return service.filtrarErroresPaginado(
                    bu,
                    codigoProveedor,
                    rfcBeneficiario,
                    tipoPago,
                    moneda,
                    monto,
                    proveedor,
                    fechaInicio,
                    fechaFin,
                    pageable);
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
    public ValidacionEnvioDTO validarPagos(@RequestParam String bu) {
        return service.validarPagosPendientes(bu);                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
    }

    @PostMapping("/enviar")
    public int enviarPagos(@RequestParam String bu) {
        return service.enviarPagosPendientes(bu);                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
    }

    @PutMapping("/rechazar/{id}")
    public String rechazarPago(@PathVariable Long id) {

        service.rechazarPago(id);

        return "Pago rechazado correctamente";
    }
    @PutMapping("/rechazar")
    public String rechazarPagos(
            @RequestBody List<Long> ids) {

        service.rechazarPagos(ids);

        return "Pagos rechazados correctamente";
    }

    @PutMapping("/{id}/referencia-manual")
    public String actualizarReferenciaManual(
            @PathVariable Long id,
            @RequestBody ReferenciaManualDTO request) {

        service.actualizarReferenciaManual(
                id,
                request.getReferenciaManual()
        );

        return "Referencia manual actualizada correctamente";
    }

    @PutMapping("/referencias-manuales")
    public String actualizarReferenciasManuales(
            @RequestBody ActualizarReferenciasManualDTO request) {

        service.actualizarReferenciasManuales(
                request.getItems()
        );

        return "Referencias manuales actualizadas correctamente";
    }

    @GetMapping("/analitica-pendientes")
    public List<AnaliticaPendientesArchivoDTO> obtenerAnaliticaPendientes(
            @RequestParam String bu) {
                

        return service.obtenerAnaliticaPendientes(bu);
    }
    
}
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             