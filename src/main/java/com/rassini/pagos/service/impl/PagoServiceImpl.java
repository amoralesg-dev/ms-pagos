package com.rassini.pagos.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;

import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import com.rassini.pagos.dto.AnaliticaPendientesArchivoDTO;
import com.rassini.pagos.dto.AnaliticaPendientesEmpresaDTO;
import com.rassini.pagos.dto.AnaliticaPendientesMonedaDTO;
import com.rassini.pagos.dto.AnaliticaPendientesTipoPagoDTO;
import com.rassini.pagos.dto.ClasificarPagoItem;
import com.rassini.pagos.dto.PagoPendienteDTO;
import com.rassini.pagos.dto.ReferenciaManualItemDTO;
import com.rassini.pagos.dto.SumaPorMonedaDTO;
import com.rassini.pagos.dto.ValidacionEnvioDTO;
import com.rassini.pagos.entity.CatalogoTipoPago;
import com.rassini.pagos.entity.PagosArchivo;
import com.rassini.pagos.entity.Supplier;
import com.rassini.pagos.exception.BusinessException;
import com.rassini.pagos.mapper.PagoMapper;
import com.rassini.pagos.repository.CatalogoTipoPagoRepository;
import com.rassini.pagos.repository.PagosArchivoRepository;
import com.rassini.pagos.repository.SupplierRepository;
import com.rassini.pagos.service.EmpresaTipoPagoCache;
import com.rassini.pagos.service.FileLoaderService;
import com.rassini.pagos.service.PagoService;
import com.rassini.pagos.util.AnaliticaPendientesUtils;
import com.rassini.pagos.util.ResolucionTipoPago;
import com.rassini.pagos.util.BuUtils;
import com.rassini.pagos.util.ConstantsSuppliers;
import com.rassini.pagos.util.EmpresaUtils;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class PagoServiceImpl implements PagoService {

    private final PagosArchivoRepository pagosRepo;
    private final CatalogoTipoPagoRepository catalogoRepo;
    private final EmpresaTipoPagoCache cache;
    private final SupplierRepository supplierRepo;
    private final FileLoaderService fileLoaderService;

    @Value("${loader.output.path}")
    private String outputPathBase;
    

    public PagoServiceImpl(PagosArchivoRepository pagosRepo,
            CatalogoTipoPagoRepository catalogoRepo,
            EmpresaTipoPagoCache cache,
            SupplierRepository supplierRepo,
            FileLoaderService fileLoaderService) {
        this.pagosRepo = pagosRepo;
        this.catalogoRepo = catalogoRepo;
        this.cache = cache;
        this.supplierRepo = supplierRepo;
        this.fileLoaderService=fileLoaderService;
    }

    /**
     * Obtener pagos sin clasificar
     */
    @Override
    public List<PagoPendienteDTO> obtenerSinClasificar(String bu) {

        return pagosRepo.findByEmpresaAndEstatus(bu, "PENDIENTE")
                .stream()
                .map(PagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long obtenerTotalPendientes(String bu) {
        return pagosRepo.countByEmpresaAndTipoPagoIsNull(bu);
    }

    /**
     * Clasificar pago con validación en cache (sin DB extra)
     */
    @Override
    public void clasificarPagos(List<ClasificarPagoItem> items, String bu) {

        List<PagosArchivo> pagosActualizar = new ArrayList<>();

        for (ClasificarPagoItem item : items) {

            PagosArchivo pago = pagosRepo.findById(item.getId())
                    .orElseThrow(() -> new BusinessException("Pago no encontrado: " + item.getId()));

            if (!BuUtils.isAll(bu)
            && !BuUtils.splitBus(bu).contains(pago.getEmpresa())) {

                throw new BusinessException(
                    "El pago no pertenece a la unidad de negocio especificada");
            }

            CatalogoTipoPago tipo = catalogoRepo.findByDealType(item.getDealType())
                    .orElseThrow(() -> new BusinessException("Tipo inválido: " + item.getDealType()));

            
            String empresaPadre =
                    EmpresaUtils.obtenerEmpresaPadre(
                            pago.getEmpresa());


            if (!cache.esValido(empresaPadre, item.getDealType())) {
                throw new BusinessException(
                        "Tipo '" + item.getDealType() +
                                "' no permitido para empresa " + pago.getEmpresa() +
                                " (pago id: " + item.getId() + ")");
            }

            pago.setTipoPago(tipo);
            pagosActualizar.add(pago);
        }

        pagosRepo.saveAll(pagosActualizar);

    }

    @Override
    public List<PagoPendienteDTO> filtrarPendientes(String bu, String codigoProveedor, String rfcBeneficiario) {

        return pagosRepo.filtrar(bu, codigoProveedor, rfcBeneficiario)
                .stream()
                .map(PagoMapper::toDTO)
                .toList();
    }

    @Override
    public Page<PagoPendienteDTO> filtrarPendientesPaginado(
            String bu,
            String codigoProveedor,
            String rfcBeneficiario,
            String tipoPago,
            String estatus,
            String moneda,
            String monto,
            String proveedor,
            Pageable pageable) {

        Page<PagosArchivo> page;

        if (BuUtils.isAll(bu)) {

            page = pagosRepo.filtrarPaginadoAll(
                    codigoProveedor,
                    rfcBeneficiario,
                    tipoPago,
                    estatus,
                    moneda,
                    monto,
                    proveedor,
                    pageable);

        } else {

            page = pagosRepo.filtrarPaginadoMultiBu(
                    BuUtils.splitBus(bu),
                    codigoProveedor,
                    rfcBeneficiario,
                    tipoPago,
                    estatus,
                    moneda,
                    monto,
                    proveedor,
                    pageable);

        }

        return page.map(entity -> {
            PagoPendienteDTO dto = PagoMapper.toDTO(entity);
            boolean tieneAba = false;
            boolean tieneSwift = false;
            String paisBeneficiario = null;

            Supplier supplier = null;
            if (entity.getCodigoProveedor() != null && !entity.getCodigoProveedor().isBlank()
                    && entity.getCuentaBeneficiario() != null && !entity.getCuentaBeneficiario().isBlank()) {
                try {
                    String ultimos8 = fileLoaderService.obtenerUltimos8DigitosCuenta(entity.getCuentaBeneficiario());
                    supplier = fileLoaderService.obtenerSupplierPadrePorCuenta(
                            entity.getCodigoProveedor(),
                            entity.getEmpresa(),
                            ultimos8);
                } catch (Exception e) {
                    log.debug("No se pudo resolver supplier específico por cuenta para pago id {}: {}", entity.getId(), e.getMessage());
                }
            }

            // Si no se encontró por cuenta exacta o venía sin cuenta, intentar por empresa padre
            if (supplier == null && entity.getCodigoProveedor() != null && !entity.getCodigoProveedor().isBlank()
                    && entity.getEmpresa() != null && !entity.getEmpresa().isBlank()) {
                try {
                    supplier = fileLoaderService.obtenerSupplierPadre(entity.getCodigoProveedor(), entity.getEmpresa());
                } catch (Exception e) {
                    log.debug("No se pudo resolver supplier por empresa padre para pago id {}: {}", entity.getId(), e.getMessage());
                }
            }

            // Extraer capacidades y país estrictamente del MISMO supplier resuelto
            if (supplier != null) {
                tieneAba = supplier.getRoutingCodeAba() != null && !supplier.getRoutingCodeAba().isBlank();
                tieneSwift = supplier.getRoutingCodeSwift() != null && !supplier.getRoutingCodeSwift().isBlank();
                paisBeneficiario = supplier.getCountryCode();

                ResolucionTipoPago resolucion = EmpresaUtils.resolverTipoPago(
                        entity.getMoneda(),
                        paisBeneficiario,
                        tieneAba,
                        tieneSwift,
                        entity.getTipoPagoSeleccionado()
                );

                dto.setTieneAba(tieneAba);
                dto.setTieneSwift(tieneSwift);
                dto.setTipoSugerido(resolucion.getTipoSugerido());
                dto.setOpcionesTipoPago(resolucion.getOpcionesValidas());
                dto.setTipoPagoSeleccionado(resolucion.getTipoResuelto());
                dto.setAdvertenciaTipoPago(resolucion.isErrorDatosBancarios() ? resolucion.getAdvertencia() : null);
            } else {
                dto.setTieneAba(false);
                dto.setTieneSwift(false);
                dto.setTipoSugerido(EmpresaUtils.calcularTipoPagoAutomatico(entity.getMoneda(), null));
                dto.setOpcionesTipoPago(Collections.emptyList());
                dto.setTipoPagoSeleccionado(entity.getTipoPagoSeleccionado());
                dto.setAdvertenciaTipoPago(null);
            }
            return dto;
        });


    }
    @Override
    public com.rassini.pagos.dto.PagosEnviadosResponseDTO filtrarEnviadosPaginado(
            String bu,
            String codigoProveedor,
            String rfcBeneficiario,
            String tipoPago,
            String moneda,
            String monto,
            String proveedor,
            String fechaInicio,
            String fechaFin,
            Pageable pageable) {

        String buParaConsulta = bu;
        Page<PagosArchivo> page;
        List<PagosArchivo> allList;

        if (BuUtils.isAll(buParaConsulta)) {

            page = pagosRepo.filtrarEnviadosPaginadoAll(
                        codigoProveedor,
                        rfcBeneficiario,
                        tipoPago,
                        moneda,
                        monto,
                        proveedor,
                        fechaInicio,
                        fechaFin,
                        pageable);

            allList = pagosRepo.filtrarEnviadosListAll(
                        codigoProveedor,
                        rfcBeneficiario,
                        tipoPago,
                        moneda,
                        monto,
                        proveedor,
                        fechaInicio,
                        fechaFin);

        } else {

            page = pagosRepo.filtrarEnviadosPaginadoMultiBu(
                        BuUtils.splitBus(buParaConsulta),
                        codigoProveedor,
                        rfcBeneficiario,
                        tipoPago,
                        moneda,
                        monto,
                        proveedor,
                        fechaInicio,
                        fechaFin,
                        pageable);

            allList = pagosRepo.filtrarEnviadosListMultiBu(
                        BuUtils.splitBus(buParaConsulta),
                        codigoProveedor,
                        rfcBeneficiario,
                        tipoPago,
                        moneda,
                        monto,
                        proveedor,
                        fechaInicio,
                        fechaFin);

        }

        Page<PagoPendienteDTO> dtoPage = page.map(PagoMapper::toDTO);

        // Calcular total y sumatorias por moneda en Java de forma segura
        long totalPagos = allList.size();

        java.util.Map<String, Double> sumasMap = allList.stream()
                .filter(p -> p.getMoneda() != null && !p.getMoneda().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        PagosArchivo::getMoneda,
                        java.util.stream.Collectors.summingDouble(p -> {
                            try {
                                return p.getMonto() != null ? Double.parseDouble(p.getMonto()) : 0.0;
                            } catch (Exception e) {
                                return 0.0;
                            }
                        })
                ));

        List<SumaPorMonedaDTO> sumasList = sumasMap.entrySet().stream()
                .map(entry -> new com.rassini.pagos.dto.SumaPorMonedaDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return com.rassini.pagos.dto.PagosEnviadosResponseDTO.builder()
                .page(dtoPage)
                .totalPagos(totalPagos)
                .sumas(sumasList)
                .build();
    }
    

    @Override
    public Page<PagoPendienteDTO> filtrarErroresPaginado(
            String bu,
            String codigoProveedor,
            String rfcBeneficiario,
            String tipoPago,
            String moneda,
            String monto,
            String proveedor,
            String fechaInicio,
            String fechaFin,
            Pageable pageable) {

        String buParaConsulta = bu;
        Page<PagosArchivo> page;

        if (BuUtils.isAll(buParaConsulta)) {

            page = pagosRepo.filtrarErroresPaginadoAll(
                    codigoProveedor,
                    rfcBeneficiario,
                    tipoPago,
                    moneda,
                    monto,
                    proveedor,
                    fechaInicio,
                    fechaFin,
                    pageable);

        } else {

            page = pagosRepo.filtrarErroresPaginadoMultiBu(
                    BuUtils.splitBus(buParaConsulta),
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

        return page.map(PagoMapper::toDTO);
    }

    @Override
    public ValidacionEnvioDTO validarPagosPendientes(String bu) {

        List<PagosArchivo> pendientes;

        if (BuUtils.isAll(bu)) {

            pendientes = pagosRepo.findPendientesValidacionAll();

        } else {

            pendientes = pagosRepo.findPendientesValidacionMultiBu(
                    BuUtils.splitBus(bu));
        }

        if (pendientes == null || pendientes.isEmpty()) {

            return new ValidacionEnvioDTO(
                    true,
                    Collections.emptyList());
        }
       

        Map<String, List<PagosArchivo>> pagosPorArchivo =
                pendientes.stream()
                        .collect(Collectors.groupingBy(
                                p -> p.getNombreArchivo() == null
                                        ? "SinArchivo"
                                        : p.getNombreArchivo()));

        List<String> errores = new ArrayList<>();

        for (Map.Entry<String, List<PagosArchivo>> entry
                : pagosPorArchivo.entrySet()) {

            String nombreArchivo = entry.getKey();

            long sinClasificar =
                    entry.getValue()
                            .stream()
                            .filter(p -> p.getTipoPago() == null)
                            .count();

            if (sinClasificar > 0) {

                errores.add(
                        String.format(
                                "El archivo %s tiene %d pago(s) sin clasificar",
                                nombreArchivo,
                                sinClasificar));
            }

            // Validar que los tipos de pago seleccionados sean compatibles con el supplier resuelto
            for (PagosArchivo p : entry.getValue()) {
                if (p.getCodigoProveedor() != null && !p.getCodigoProveedor().isBlank()) {
                    Supplier s = null;
                    if (p.getCuentaBeneficiario() != null && !p.getCuentaBeneficiario().isBlank()) {
                        try {
                            String u8 = fileLoaderService.obtenerUltimos8DigitosCuenta(p.getCuentaBeneficiario());
                            s = fileLoaderService.obtenerSupplierPadrePorCuenta(p.getCodigoProveedor(), p.getEmpresa(), u8);
                        } catch (Exception ignored) {}
                    }
                    if (s == null) {
                        try {
                            s = fileLoaderService.obtenerSupplierPadre(p.getCodigoProveedor(), p.getEmpresa());
                        } catch (Exception ignored) {}
                    }
                    if (s != null) {
                        boolean aba = s.getRoutingCodeAba() != null && !s.getRoutingCodeAba().isBlank();
                        boolean swift = s.getRoutingCodeSwift() != null && !s.getRoutingCodeSwift().isBlank();
                        String pais = s.getCountryCode();

                        ResolucionTipoPago res = EmpresaUtils.resolverTipoPago(
                                p.getMoneda(),
                                pais,
                                aba,
                                swift,
                                p.getTipoPagoSeleccionado()
                        );

                        EmpresaUtils.logResolucion(
                                log,
                                p.getId(),
                                p.getNombreArchivo(),
                                p.getCodigoProveedor(),
                                p.getEmpresa(),
                                p.getMoneda(),
                                pais,
                                aba,
                                swift,
                                p.getTipoPagoSeleccionado(),
                                res
                        );

                        String tipoOficial = EmpresaUtils.calcularTipoPagoAutomatico(p.getMoneda(), pais);
                        String tipoEfectivo = p.getTipoPagoSeleccionado();
                        if (tipoEfectivo == null || tipoEfectivo.isBlank()) {
                            tipoEfectivo = tipoOficial;
                        }

                        String tipoNorm = EmpresaUtils.normalizarTipoPago(tipoEfectivo);
                        if (tipoNorm == null) {
                            errores.add("El pago id " + p.getId() + " del proveedor " + p.getCodigoProveedor()
                                    + " tiene un tipo de pago no permitido: " + tipoEfectivo);
                            continue;
                        }

                        if ("MXN".equalsIgnoreCase(p.getMoneda()) && !EmpresaUtils.TIPO_PAGO_SPEI.equals(tipoNorm)) {
                            errores.add("El pago id " + p.getId() + " del proveedor " + p.getCodigoProveedor()
                                    + " tiene tipo " + tipoNorm + ", pero por regla de negocio la única opción válida es SPEI.");
                            continue;
                        }

                        // Validación técnica de datos bancarios:
                        // ABA y SWIFT solamente se utilizan para validar si el método seleccionado puede ejecutarse.
                        if (EmpresaUtils.TIPO_PAGO_ACH.equals(tipoNorm)) {
                            if (!aba) {
                                String errorMsg = "Proveedor " + p.getCodigoProveedor()
                                        + " no cuenta con Routing Code ABA requerido para ACH.";
                                if (!errores.contains(errorMsg)) {
                                    errores.add(errorMsg);
                                }
                            }
                        } else {
                            // WIRE, SPID, SPEI requieren SWIFT
                            if (!swift) {
                                String errorMsg = "Proveedor " + p.getCodigoProveedor()
                                        + " no cuenta con Routing Code SWIFT requerido para " + tipoNorm + ".";
                                if (!errores.contains(errorMsg)) {
                                    errores.add(errorMsg);
                                }
                            }
                        }
                    }
                }
            }
        }

        return new ValidacionEnvioDTO(
                errores.isEmpty(),
                errores);
    }
    
    @Override
    public int enviarPagosPendientes(String bu) {

            List<PagosArchivo> pendientes;

            if (BuUtils.isAll(bu)) {

                    pendientes = pagosRepo.findPendientesPorEnviarAll();

            } else {

                    pendientes = pagosRepo.findPendientesPorEnviarMultiBu(
                                    BuUtils.splitBus(bu));
            }

            if (pendientes == null || pendientes.isEmpty()) {
                    return 0;
            }

            ValidacionEnvioDTO validacion = validarPagosPendientes(bu);

            if (!validacion.isPermitido()) {

                    throw new BusinessException(
                                    String.join("\n", validacion.getErrores()));
            }

            Map<String, Map<String, List<PagosArchivo>>> grouped = pendientes.stream()
                            .collect(Collectors.groupingBy(
                                            p -> p.getNombreArchivo() == null
                                                            ? "SinArchivo"
                                                            : p.getNombreArchivo(),
                                            Collectors.groupingBy(
                                                            p -> (p.getTipoPago() != null
                                                                            && p.getTipoPago().getDealType() != null)
                                                                                            ? p.getTipoPago()
                                                                                                            .getDealType()
                                                                                            : "SinTipo")));

            int filesGenerated = 0;

            for (Map.Entry<String, Map<String, List<PagosArchivo>>> entryArchivo : grouped.entrySet()) {

                    String nombreArchivo = entryArchivo.getKey();

                    for (Map.Entry<String, List<PagosArchivo>> entryTipo : entryArchivo.getValue().entrySet()) {

                            String tipoPago = entryTipo.getKey();
                            List<PagosArchivo> pagos = entryTipo.getValue();

                            String cleanTipoPago = tipoPago.replaceAll("\\s+", "");

                            String baseNombreArchivo = nombreArchivo;

                            if (baseNombreArchivo != null
                                            && baseNombreArchivo.toLowerCase().endsWith(".txt")) {

                                    baseNombreArchivo = baseNombreArchivo.substring(
                                                    0,
                                                    baseNombreArchivo.length() - 4);
                            }

                            String nombreArchivoOutput = cleanTipoPago + baseNombreArchivo;

                            if (nombreArchivoOutput.length() > 31) {
                                    nombreArchivoOutput = nombreArchivoOutput.substring(0, 31);
                            }

                            String outputFileName = String.format("%s.txt", nombreArchivoOutput);

                            Map<String, List<PagosArchivo>> pagosPorEmpresa = pagos.stream()
                                            .collect(Collectors.groupingBy(
                                                            p -> EmpresaUtils.obtenerEmpresaPadre(
                                                                            p.getEmpresa())));

                            for (Map.Entry<String, List<PagosArchivo>> entryEmpresa : pagosPorEmpresa.entrySet()) {

                                    String empresaPadre = entryEmpresa.getKey();

                                    List<PagosArchivo> pagosEmpresa = entryEmpresa.getValue();

                                    List<String> lineas = new ArrayList<>();

                                    for (PagosArchivo pago : pagosEmpresa) {

                                            Supplier supplier = null;

                                            if (pago.getCodigoProveedor() != null) {

                                                    String ultimos8 = fileLoaderService.obtenerUltimos8DigitosCuenta(
                                                                    pago.getCuentaBeneficiario());
                                                        
                                                    log.info("Obteniendo supplier para proveedor: {}, empresa: {}, ultimos8: {}",
                                                                    pago.getCodigoProveedor(),
                                                                    pago.getEmpresa(),
                                                                    ultimos8);
                                                    supplier = fileLoaderService.obtenerSupplierPadrePorCuenta(
                                                                    pago.getCodigoProveedor(),
                                                                    pago.getEmpresa(),
                                                                    ultimos8);
                                            }

                                            lineas.add(
                                                            generarLineaLayout(
                                                                            pago,
                                                                            supplier,
                                                                            outputFileName));

                                            pago.setNombreArchivoEnvio(outputFileName);
                                            pago.setEstatus("ENVIADO");
                                    }

                                    try {

                                            Path outputDir = Paths.get(
                                                            outputPathBase,
                                                            empresaPadre);

                                            if (!Files.exists(outputDir)) {
                                                    Files.createDirectories(outputDir);
                                            }

                                            Path outputPath = outputDir.resolve(outputFileName);

                                            Files.write(outputPath, lineas);

                                            log.info(
                                                            "Archivo generado en ruta: {}",
                                                            outputPath.toAbsolutePath());

                                            filesGenerated++;

                                    } catch (IOException e) {

                                            throw new BusinessException(
                                                            "Error al generar archivo txt: "
                                                                            + outputFileName);
                                    }
                            }
                    }
            }

            pagosRepo.saveAll(pendientes);

            return filesGenerated;
    }

    private String generarLineaLayout(PagosArchivo pago,
                    Supplier supplier,
                    String outputFileName) {

            String[] campos = new String[28];

            campos[0] = nvl(pago.getEmpresa());//
            campos[1] = nvl(pago.getCuentaOrdenante());
            campos[2] = nvl(pago.getMonedaOrdenante());
            campos[3] = nvl(pago.getReferencia());
            if (ConstantsSuppliers.PN.equals(EmpresaUtils.obtenerEmpresaPadre(pago.getEmpresa()))) {
                    campos[4] = nvl(pago.getReferencia());
            } else {
                    campos[4] = nvl(pago.getInformacionAdicional());
            }

            if(pago.getReferenciaManual()!=null && !pago.getReferenciaManual().isBlank()){
                campos[4]=nvl(pago.getReferenciaManual());
            }

            campos[5] = nvl(pago.getFechaEnvio());
            campos[6] = nvl(pago.getFechaValor());
            campos[7] = nvl(pago.getMonto());
            campos[8] = nvl(pago.getMoneda());
            campos[9] = nvl(pago.getTipoCambio());
            campos[10] = nvl(pago.getCodigoProveedor());
            campos[11] = nvl(pago.getNombreBeneficiario());
            campos[12] = nvl(pago.getRfcBeneficiario());

            if (supplier == null ||
                            supplier.getStreetName() == null || supplier.getStreetName().trim().isEmpty() ||
                            supplier.getStreetNumber() == null || supplier.getStreetNumber().trim().isEmpty() ||
                            supplier.getZipCode() == null || supplier.getZipCode().trim().isEmpty() ||
                            supplier.getCityCode() == null || supplier.getCityCode().trim().isEmpty() ||
                            supplier.getStateCode() == null || supplier.getStateCode().trim().isEmpty() ||
                            supplier.getCountryCode() == null || supplier.getCountryCode().trim().isEmpty()) {

                    throw new BusinessException(
                                    "El proveedor " + pago.getCodigoProveedor() +
                                                    " no cuenta con la información completa para generar el layout");
            }

            campos[13] = nvl(supplier.getStreetName());
            campos[14] = nvl(supplier.getStreetNumber());
            campos[15] = nvl(supplier.getZipCode());
            campos[16] = nvl(supplier.getCityCode());
            campos[17] = nvl(supplier.getStateCode());
            campos[18] = nvl(supplier.getCountryCode());

            campos[19] = nvl(pago.getCuentaBeneficiario());
            campos[20] = nvl(pago.getMonedaBeneficiario());

            // al parecer ya no se usara
            boolean isBeneficiaryBankValid = true;

            campos[21] = isBeneficiaryBankValid
                            ? nvl(supplier.getBeneficiaryBankName())
                            : "";


            String tipoEfectivo = pago.getTipoPagoSeleccionado();
            if (tipoEfectivo == null || tipoEfectivo.isBlank()) {
                tipoEfectivo = EmpresaUtils.calcularTipoPagoAutomatico(pago.getMoneda(), supplier != null ? supplier.getCountryCode() : null);
            }

            // Normalizar case de forma insensible para los valores permitidos
            String tipoNormalizado = null;
            for (String permitido : EmpresaUtils.TIPOS_PAGO_SELECCIONADOS_PERMITIDOS) {
                if (permitido.equalsIgnoreCase(tipoEfectivo.trim())) {
                    tipoNormalizado = permitido;
                    break;
                }
            }
            if (tipoNormalizado == null) {
                throw new BusinessException("Tipo de pago seleccionado inválido: " + tipoEfectivo
                        + ". Valores permitidos: ACH, WIRE, SPID, SPEI");
            }

            if (EmpresaUtils.TIPO_PAGO_ACH.equals(tipoNormalizado)) {
                String aba = supplier.getRoutingCodeAba();
                if (aba == null || aba.isBlank()) {
                    throw new BusinessException("Proveedor " + pago.getCodigoProveedor()
                            + " no cuenta con Routing Code ABA requerido para ACH.");
                }
                campos[22] = nvl(aba);
            } else {
                // WIRE, SPID, SPEI utilizan Routing Code SWIFT
                String swift = supplier.getRoutingCodeSwift();
                if (swift == null || swift.isBlank()) {
                    throw new BusinessException("Proveedor " + pago.getCodigoProveedor()
                            + " no cuenta con Routing Code SWIFT requerido para " + tipoNormalizado + ".");
                }
                campos[22] = nvl(swift);
            }

            // Todos los métodos conservan las fechas originales
            campos[5] = nvl(pago.getFechaEnvio());
            campos[6] = nvl(pago.getFechaValor());

            campos[23] = nvl(supplier.getBankCountry());
            campos[24] = nvl(supplier.getIntermediaryAccount());


            String intRuteo = supplier.getIntermediaryRoutingCodeSwift();

            if (intRuteo == null || intRuteo.isBlank()) {
                    intRuteo = supplier.getIntermediaryRoutingCodeAba();
            }

            campos[25] = nvl(intRuteo);

            campos[26] = nvl(supplier.getIntermediaryAccountCountry());

            campos[27] = nvl(outputFileName);

            return String.join("|", campos);
    }

    private String nvl(String val) {
        return val == null ? "" : val;
    }

    @Override
    @Transactional
    public void rechazarPago(Long id) {

        PagosArchivo pago = pagosRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Pago no encontrado: " + id));

        pago.setEstatus(ConstantsSuppliers.RECHAZADO);

        pagosRepo.save(pago);

    }

    @Override
    @Transactional
    public void rechazarPagos(List<Long> ids) {

        List<PagosArchivo> pagos = pagosRepo.findAllById(ids);

        pagos.forEach(p -> p.setEstatus(ConstantsSuppliers.RECHAZADO));

        pagosRepo.saveAll(pagos);

    }

    @Override
    @Transactional
    public void actualizarReferenciaManual(Long id, String referenciaManual) {

            PagosArchivo pago = pagosRepo.findById(id)
                            .orElseThrow(() -> new BusinessException(
                                            "No se encontró el pago con id: " + id));

            pago.setReferenciaManual(referenciaManual);

            pagosRepo.save(pago);
    }


    @Override
    @Transactional
    public void actualizarReferenciasManuales(
                    List<ReferenciaManualItemDTO> items) {

            List<Long> ids = items.stream()
                            .map(ReferenciaManualItemDTO::getId)
                            .toList();

            List<PagosArchivo> pagos = pagosRepo.findAllById(ids);

            Map<Long, String> referenciasPorId = items.stream()
                            .collect(Collectors.toMap(
                                            ReferenciaManualItemDTO::getId,
                                            ReferenciaManualItemDTO::getReferenciaManual));

            pagos.forEach(pago -> pago.setReferenciaManual(
                            referenciasPorId.get(pago.getId())));

            pagosRepo.saveAll(pagos);
    }

    @Override
    @Transactional
    public void actualizarTipoPagoSeleccionado(Long id, String tipoPagoSeleccionado) {
            if (id == null) {
                throw new BusinessException("El id del pago es obligatorio");
            }
            String tipoNormalizado = validarYNormalizarTipoPagoSeleccionado(tipoPagoSeleccionado);

            PagosArchivo pago = pagosRepo.findById(id)
                            .orElseThrow(() -> new BusinessException(
                                            "No se encontró el pago con id: " + id));

            if (!ConstantsSuppliers.PENDIENTE.equalsIgnoreCase(pago.getEstatus())) {
                throw new BusinessException("Solo se pueden modificar pagos en estatus PENDIENTE. El pago con id "
                        + id + " se encuentra en estatus: " + pago.getEstatus());
            }

            pago.setTipoPagoSeleccionado(tipoNormalizado);
            pagosRepo.save(pago);
    }

    @Override
    @Transactional
    public void actualizarTiposPagoSeleccionados(
                    List<com.rassini.pagos.dto.TipoPagoSeleccionadoItemDTO> items) {
            if (items == null || items.isEmpty()) {
                throw new BusinessException("La lista de pagos a actualizar no puede ser nula ni vacía");
            }

            // 1. Validar que ningún item sea null, ningún id sea null, valor permitido y detectar duplicados
            java.util.Set<Long> idsUnicos = new java.util.HashSet<>();
            for (com.rassini.pagos.dto.TipoPagoSeleccionadoItemDTO item : items) {
                if (item == null) {
                    throw new BusinessException("El elemento de pago a actualizar no puede ser nulo");
                }
                if (item.getId() == null) {
                    throw new BusinessException("El id del pago es obligatorio en la actualización masiva");
                }
                if (!idsUnicos.add(item.getId())) {
                    throw new BusinessException("Se detectó un ID duplicado en el request de actualización: " + item.getId());
                }
                validarYNormalizarTipoPagoSeleccionado(item.getTipoPagoSeleccionado());
            }

            List<Long> ids = items.stream()
                            .map(com.rassini.pagos.dto.TipoPagoSeleccionadoItemDTO::getId)
                            .toList();

            // 2. Consultar entidades y comparar IDs solicitados vs encontrados
            List<PagosArchivo> pagos = pagosRepo.findAllById(ids);
            if (pagos.size() != ids.size()) {
                java.util.Set<Long> encontradosIds = pagos.stream().map(PagosArchivo::getId).collect(Collectors.toSet());
                List<Long> faltantes = ids.stream().filter(id -> !encontradosIds.contains(id)).toList();
                throw new BusinessException("No se encontraron los siguientes pagos para actualizar: " + faltantes);
            }

            // 3. Validar que todos los pagos encontrados estén en estatus PENDIENTE
            for (PagosArchivo pago : pagos) {
                if (!ConstantsSuppliers.PENDIENTE.equalsIgnoreCase(pago.getEstatus())) {
                    throw new BusinessException("Solo se pueden modificar pagos en estatus PENDIENTE. El pago con id "
                            + pago.getId() + " se encuentra en estatus: " + pago.getEstatus());
                }
            }

            // 4. Modificar entidades y persistir atómicamente dentro de la transacción
            Map<Long, String> tiposPorId = items.stream()
                            .collect(Collectors.toMap(
                                             com.rassini.pagos.dto.TipoPagoSeleccionadoItemDTO::getId,
                                             item -> validarYNormalizarTipoPagoSeleccionado(item.getTipoPagoSeleccionado())));

            pagos.forEach(pago -> {
                String nuevoTipo = tiposPorId.get(pago.getId());
                if (nuevoTipo != null) {
                    pago.setTipoPagoSeleccionado(nuevoTipo);
                }
            });

            pagosRepo.saveAll(pagos);
    }

    private String validarYNormalizarTipoPagoSeleccionado(String tipoPagoSeleccionado) {
            if (tipoPagoSeleccionado == null || tipoPagoSeleccionado.isBlank()) {
                throw new BusinessException("El tipo de pago seleccionado es obligatorio");
            }
            String trim = tipoPagoSeleccionado.trim();
            for (String permitido : EmpresaUtils.TIPOS_PAGO_SELECCIONADOS_PERMITIDOS) {
                if (permitido.equalsIgnoreCase(trim)) {
                    return permitido;
                }
            }
            throw new BusinessException("Tipo de pago seleccionado inválido: " + tipoPagoSeleccionado
                    + ". Valores permitidos: ACH, WIRE, SPID, SPEI");
    }

    @Override
    public List<AnaliticaPendientesArchivoDTO> obtenerAnaliticaPendientes(
                    String bu) {

            List<PagosArchivo> pendientes;

            if (BuUtils.isAll(bu)) {

                    pendientes = pagosRepo.findPendientesValidacionAll();

            } else {

                    pendientes = pagosRepo.findPendientesValidacionMultiBu(
                    BuUtils.splitBus(bu));
            }

            if (pendientes == null || pendientes.isEmpty()) {
                    return Collections.emptyList();
            }

            Map<String, List<PagosArchivo>> pagosPorArchivo = pendientes.stream()
                            .collect(Collectors.groupingBy(
                                            AnaliticaPendientesUtils::obtenerNombreArchivo));

            return pagosPorArchivo.entrySet()
                            .stream()
                            .map(entryArchivo -> {

                                    AnaliticaPendientesArchivoDTO archivoDTO = new AnaliticaPendientesArchivoDTO();

                                    archivoDTO.setNombreArchivo(
                                                    entryArchivo.getKey());

                                    archivoDTO.setCantidadPagos(
                                                    (long) entryArchivo.getValue().size());

                                    archivoDTO.setMontoTotal(
                                                    AnaliticaPendientesUtils.calcularMontoTotal(
                                                                    entryArchivo.getValue()));

                                    Map<String, List<PagosArchivo>> pagosPorEmpresa = entryArchivo.getValue()
                                                    .stream()
                                                    .collect(Collectors.groupingBy(
                                                                    AnaliticaPendientesUtils::obtenerEmpresa));

                                    List<AnaliticaPendientesEmpresaDTO> empresas = pagosPorEmpresa.entrySet()
                                                    .stream()
                                                    .map(entryEmpresa -> {

                                                            AnaliticaPendientesEmpresaDTO empresaDTO = new AnaliticaPendientesEmpresaDTO();

                                                            empresaDTO.setEmpresa(
                                                                            entryEmpresa.getKey());

                                                            empresaDTO.setCantidadPagos(
                                                                            (long) entryEmpresa.getValue().size());

                                                            empresaDTO.setMontoTotal(
                                                                            AnaliticaPendientesUtils.calcularMontoTotal(
                                                                                            entryEmpresa.getValue()));

                                                            Map<String, List<PagosArchivo>> pagosPorTipo = entryEmpresa
                                                                            .getValue()
                                                                            .stream()
                                                                            .collect(Collectors.groupingBy(
                                                                                            AnaliticaPendientesUtils::obtenerTipoPago));

                                                            List<AnaliticaPendientesTipoPagoDTO> tiposPago = pagosPorTipo
                                                                            .entrySet()
                                                                            .stream()
                                                                            .map(entryTipo -> {

                                                                                    AnaliticaPendientesTipoPagoDTO tipoDTO = new AnaliticaPendientesTipoPagoDTO();

                                                                                    tipoDTO.setTipoPago(
                                                                                                    entryTipo.getKey());

                                                                                    tipoDTO.setCantidadPagos(
                                                                                                    (long) entryTipo.getValue()
                                                                                                                    .size());

                                                                                    tipoDTO.setMontoTotal(
                                                                                                    AnaliticaPendientesUtils
                                                                                                                    .calcularMontoTotal(
                                                                                                                                    entryTipo.getValue()));

                                                                                    Map<String, List<PagosArchivo>> pagosPorMoneda = entryTipo
                                                                                                    .getValue()
                                                                                                    .stream()
                                                                                                    .collect(Collectors
                                                                                                                    .groupingBy(
                                                                                                                                    AnaliticaPendientesUtils::obtenerMoneda));

                                                                                    List<AnaliticaPendientesMonedaDTO> monedas = pagosPorMoneda
                                                                                                    .entrySet()
                                                                                                    .stream()
                                                                                                    .map(entryMoneda -> {

                                                                                                            AnaliticaPendientesMonedaDTO monedaDTO = new AnaliticaPendientesMonedaDTO();

                                                                                                            monedaDTO.setMoneda(
                                                                                                                            entryMoneda.getKey());

                                                                                                            monedaDTO.setCantidadPagos(
                                                                                                                            (long) entryMoneda
                                                                                                                                            .getValue()
                                                                                                                                            .size());

                                                                                                            monedaDTO.setMontoTotal(
                                                                                                                            AnaliticaPendientesUtils
                                                                                                                                            .calcularMontoTotal(
                                                                                                                                                            entryMoneda.getValue()));

                                                                                                            return monedaDTO;
                                                                                                    })
                                                                                                    .toList();

                                                                                    tipoDTO.setMonedas(monedas);

                                                                                    return tipoDTO;
                                                                            })
                                                                            .toList();

                                                            empresaDTO.setTiposPago(tiposPago);

                                                            return empresaDTO;
                                                    })
                                                    .toList();

                                    archivoDTO.setEmpresas(empresas);

                                    return archivoDTO;
                            })
                            .toList();
    }

}
